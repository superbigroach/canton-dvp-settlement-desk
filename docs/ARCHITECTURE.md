# Architecture — ETP Foundry (formerly CrossDesk) · Canton benchmark fixings, fund creation/redemption, DvP

The stack top-to-bottom, local vs. production, how a trade flows end to end, and — since the
22 September 2026 audit — where each security control actually lives.

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

## Security posture — as verified 22 September 2026

Three layers, each enforcing something the others cannot. Read with
`BUSINESS/5-RUN-THE-COMMITTEE/12-FULL-SYSTEM-AUDIT-2026-09-22.md`, which is the ranked finding list.

```
Firebase Hosting (etpfoundry.com)   /api/** → Cloud Run · everything else → static site
        │  security headers on ** (X-Frame-Options DENY · nosniff · Referrer-Policy · Permissions-Policy)
Spring Boot  AuthFilter            identity: Firebase ID token (email-verified only) · API key (ck_, SHA-256 at rest)
        │                          · X-Sandbox-User ONLY when AUTH_MODE=sandbox (hosted host runs AUTH_MODE=firebase)
        │                          path is NORMALISED before classification: `;params` stripped per segment, decoded,
        │                          `..` / `//` / backslash / encoded-slash refused → unauthenticated
        │  SettlementController     every non-venue seat must carry evidence (no bare ticks); SignerEvidence
        │                          applies each condition's rule server-side; reserve model per instrument
        │  SeriesService.recognised  a NavFixing is PUBLISHED only if a real OperatorCommittee visible to the
        │                          auditor has the same admin, declares the same threshold, that threshold is
        │                          met, and every attestor is a member — else dropped + logged as possible forgery
Ledger API (JWT actAs/readAs)      the operator backend holds actAs for every hosted (L1) seat — see trust ladder
Daml  Governance.daml (3.0.0)      FixingProposal / NavFixing `signatory admin :: approvers|attestors`;
                                   committee ensure K ≥ 2, K ≤ N, admin ∉ members; plain Confirm retired;
                                   venue range or no-prints mandatory; asOfDate + FixingSeries slot = one
                                   fixing per series per day; any approver or the admin finalises
```

**What the ledger enforces since package 3.0.0 (22 Sep 2026).** A fixing can only be born from
a proposal the administrator signed, inside a committee the administrator signed, whose `ensure`
bounded K and N and excluded the administrator from the roster. The 2.x forgery (a proposal
naming the real administrator with `threshold = 1` and the forger as sole approver) is
unconstructible — `Test:testFixingCannotBeForged` attempts it and fails at the ledger. A
signatory change is not a compatible upgrade, so this is a new package; nothing needed
migrating because no fixing existed on any shared ledger.

**Trust ladder (who can forge what).** L1 hosted party — the operator's backend signs for the
seat, so K-of-N collapses to the operator's key; suitable for evaluation only, never for a fixing
a third party settles against. L2 external signing — the seat's own service POSTs evidence under
its own API key; the operator still submits the ledger command. L3 own participant — the seat
hosts its party and signs on its own node; the operator cannot forge it. Disclosed in the rulebook
§6.7 and `SIGNER_PROTOCOL.md` §5.

**Open, in the order they matter** (all in the audit file): `backend-devnet/` (the shared
HackCanton node's desk) still targets 2.1.0 — not a benchmark surface, but it should not be
started against a 3.0.0 ledger; the ops desk's plain-confirm button now gets a 410 (its
checked path works); creation/redemption fee payable in counterfeit cash (`Basket.daml` `chargeFee` does not check the cash issuer — appended
`Optional cashIssuer`, upgrade-safe); signer-service still on the 3-seat protocol v1 (halts, never
confirms wrongly); admin act-as can confirm as a seat; tolerance caps; BigDecimal scale bound;
webhook-URL SSRF; `/api/diag` detail public; hosting catch-all serves the landing page as 200 for
unknown paths; per-instrument freshness not bound.
