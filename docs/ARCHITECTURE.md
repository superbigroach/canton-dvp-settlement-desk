# Architecture — CrossDesk (Canton DvP / sealed-auction settlement desk)

The stack top-to-bottom, local vs. production, and how a trade flows end to end.

## The 60-second version
A **React/TypeScript** front end talks over REST to a **Java 17 / Spring Boot** service. That
service uses the **Daml Java Bindings** to speak the **Ledger API** (gRPC) to a **Canton
participant node**, which runs the **Daml** smart contracts and holds each party's data. The
participant connects to a **Canton synchronizer** that orders and delivers *encrypted* messages
between participants and coordinates atomic, multi-party settlement **without seeing the contract
data**. Locally it runs against a Canton **sandbox** with no auth; in production the same jar
points at a real participant with **mTLS + JWT**, keys in an **HSM/KMS**, deployed on
**Kubernetes/GKE via Helm**.

## The layers
```
React + TypeScript + Vite        the trading-desk UI (holds no keys)
        │  REST / JSON
Java 17 · Spring Boot 3          business logic; REST → Ledger API commands (Daml Java Bindings + codegen)
        │  Ledger API — gRPC · mTLS · JWT (actAs/readAs)   ← THE SEAM (auth boundary)
Canton PARTICIPANT NODE          runs the DAR, hosts parties, stores only its slice, signs txns
        │  encrypted messages
Canton SYNCHRONIZER              Sequencer (orders ciphertext) + Mediator (confirms) + Topology (PKI)
                                 coordinates atomic multi-party settlement, never sees contract data
Daml contracts run on the participant: Instrument · Holding · Settlement · MarketOnClose · Agent
```

## Layer by layer
- **Frontend — React/TS/Vite.** Pure client; talks only to the backend over REST. TypeScript for money-path type safety. Language-agnostic — the ledger doesn't care.
- **Backend — Java 17 / Spring Boot 3.** Turns REST calls into **Ledger API commands** (create contract / exercise choice) via the **Daml Java Bindings** + **codegen** (Daml templates → typed Java classes); reads state from the **Active Contract Set (ACS)**. *This is the exact JPM layer.*
- **Ledger API — gRPC (the seam).** Every call carries a **JWT** scoping `actAs`/`readAs` parties, over **mTLS**. Auth = bearer token, not wallet signature.
- **Participant node.** Runs the compiled **DAR**, hosts the **parties**, stores **only contracts its parties are stakeholders on**, signs and submits transactions, exposes the Ledger API. An institution must run one — that is the privacy model.
- **Synchronizer (domain).** Sequencer orders encrypted messages (can't read them); Mediator confirms authorization without content; Topology maps party→key→participant. Coordinates atomic multi-party settlement; sees only ciphertext.
- **Daml — the 3-layer data model** (Daml-Finance shape):
  - **Instrument** = reference data (*what* an asset is: `DEMO:AAPL`, `USDC`, `cETH`, `CBTC`).
  - **Holding** = balances (*who holds how much*; issuer-signed, owner-observer).
  - **Settlement / MarketOnClose** = movement (atomic DvP + the sealed call auction), plus `Agent` (mandates) and the designated-LP imbalance disclosure.
  - Authority is declared by **party** (`signatory`/`observer`/`controller`); Daml never sees keys — topology maps party→key, the participant signs.

## Local build (now — free)
```
React (:5173) → Spring Boot (:8080) → Ledger API → Canton SANDBOX (:6900)
    all local (WSL) · no auth · self-issued tokens · parties are just names
```
The **sandbox** is one in-memory process = a participant + a mini-synchronizer, so you build fast. No JWT/mTLS, no HSM, no Docker/K8s, no wallet. Built with **Daml SDK 2.9.4** (`daml build` / `daml test` / `daml sandbox`).

## Production / ideal build (same code)
```
React → Spring Boot (Docker → Kubernetes/GKE via Helm)
      → Ledger API (mTLS + JWT)
      → REAL Canton participant node (K8s; party keys in HSM/KMS, never leave)
      → Devnet/MainNet SYNCHRONIZER (shared with DTCC, Goldman, JPMorgan…)
```
- **Auth on** via config only: `LEDGER_TLS=true` + `LEDGER_JWT=<bearer>` — "same jar, two ledgers."
- **Real assets:** cETH (onRails), CBTC (BitSafe), USDCx (Circle) sent to your party.
- **Infra:** app tier Dockerized on **GKE via Helm**; the participant likewise containerized/K8s (often a managed **NaaS** node). Gas = Canton Coin (free-tapped on Devnet).

## A trade, end to end
1. Click **Send to Auction** → POST `/api/moc/order` to Spring Boot.
2. Backend builds an **exercise `SubmitOrder`** command (typed via codegen), submits over the **Ledger API** as that party (JWT `actAs`).
3. Participant validates against the Daml, **signs** (HSM/KMS key in prod), sends the encrypted tx to the **synchronizer**.
4. **Sequencer** orders it, **Mediator** confirms authorization — the sealed `SealedOrder` exists, visible only to that trader + the venue.
5. Venue exercises **`RunClose`** → one atomic tx moves every matched leg (DvP) + writes the receipt. All-or-nothing, instant finality.
6. Backend reads the new ACS, returns fills; React shows the official price + receipt.

## The senior talking points (the *why*)
- **"Daml is the contract layer, Canton is the network, the Ledger API is the seam, a Java/Spring service drives it."**
- **Privacy is architectural** — each participant holds only its slice; the synchronizer sees ciphertext. That's why a *sealed* order book is possible here and impossible on a transparent chain.
- **Load-bearing design:** holdings are issuer-signed / owner-observer → a two-leg swap settles in ONE atomic transaction via delegated authority (no counterparty co-sign). Making the holder a signatory would break single-transaction atomicity.
- **Auth = JWT + mTLS, not wallet signatures** — OAuth/Firebase-style; the participant signs with HSM/KMS keys the topology maps to the party.
- **Instant hard finality** (BFT synchronizer) → credit after one block, no reorgs, no T+2 — why it fits institutional settlement.
