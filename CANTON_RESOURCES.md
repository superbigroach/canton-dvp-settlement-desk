# Canton / Daml Official Resources

Curated catalog of the official Digital Asset / Canton ecosystem repos — **reference and build ON these; do NOT fork them into this project.** Star the useful ones on GitHub; clone the key few separately to study/bootstrap.

## How to use these (the rule)
- ⭐ **Star** the useful ones (organizes your list + signals ecosystem engagement).
- 📥 **Clone the key few separately** (a `canton-reference/` folder *outside* your project) to study/bootstrap — not to copy in.
- 🔧 **Use the libraries as dependencies** (that's what they're for), don't reinvent.
- ❌ **Don't fork them into this repo** — it bloats it and muddies what's yours.

## The ones that matter (ranked)
| Repo | What it is | How to use it |
|---|---|---|
| **cn-quickstart** ⭐⭐⭐ | Official "start quick" bootstrap for a Canton app + Devnet deploy | **Fastest path to a deployed app** — bootstrap your app on top of it. |
| ~~**daml-finance**~~ | Official Daml **Finance** library: instruments, holdings, settlement, DvP | 🔴 **CANNOT BE USED — do not spend a day discovering this.** It targets `--target=1.17`, and a Canton 3.x participant rejects LF 1.x outright: `Disallowed language version … expected 2.1, 2.2, 2.3 but got 1.14`. This project builds on SDK 3.4.11 / LF 2.2 because the node requires it. The hand-rolled `Holding`/`Instrument` layer is a consequence of that constraint, not a preference. See `docs/DAML_FINANCE_INTEGRATION.md` for the mapping we would have used. |
| **daml-finance-app** ⭐⭐ | Reference app on daml-finance | Study settlement/holdings/DvP patterns. |
| **xreserve-deposits** ⭐ | USDC deposits to Canton via Ethereum (xReserve) | Reference for the cETH/deposit/bridging angle. |
| **splice** | Canton Network sync / Canton Coin / wallet infra | Reference for the network + wallet layer. |
| **daml** | The Daml SDK/language itself | Install it (`daml`), don't fork. |
| **cn-quickstart** docs / **docs.daml.com** | Documentation | The source of truth for SDK/Devnet specifics. |
| dazl-client (Py) · go-daml (Go) · ex-java-bindings (Java) · dabl-react (React) | Ledger API clients per language | Pick one for the **UI/backend** that talks to the ledger (dabl-react or dazl for a quick UI). |
| ex-secure-canton-infra | Secure node deployment reference | Only if self-hosting a validator. |
| wallet-gateway | Wallet gateway Docker build | Infra reference. |

## Clone commands (study / bootstrap — outside this repo)
```bash
mkdir ~/Desktop/canton-reference && cd ~/Desktop/canton-reference
git clone https://github.com/digital-asset/cn-quickstart.git        # bootstrap
git clone https://github.com/digital-asset/daml-finance.git         # the finance library
git clone https://github.com/digital-asset/daml-finance-app.git     # reference app
git clone https://github.com/digital-asset/xreserve-deposits.git    # USDC->Canton deposits
```

## The strategic path (given a tight timeline)
1. **Bootstrap from `cn-quickstart`** — get a deployable Canton app skeleton + Devnet wiring out of the box.
2. **Build the settlement desk using `daml-finance`** holdings/settlement/DvP primitives — faster and more professional than hand-rolling, and it reads as institutional-grade to judges *and* to a JPMorgan audience.
3. The hand-rolled DAML in `daml/` here is the **learning foundation** (shows the primitives are understood); a deployed build can bootstrap from the official quickstart + library.

> **Verify the Daml SDK / Splice version** Devnet expects (from `cn-quickstart` / Devnet onboarding) and set it in `daml.yaml`. Real **cETH is issued by onRails** — you can't self-mint it on Devnet; request it via their form and align `Instrument.daml`'s `issuer`/`id` to onRails' registry values.

---

## Issuer and asset infrastructure — BitSafe / `github.com/DLC-link`

Checked 13 August 2026, 14 public repos. BitSafe is legally **DLC-Link, Inc. dba BitSafe** — the org
name is why searching for "bitsafe" on GitHub finds nothing.

They issue **cBTC**, the asset this project holds 4.16 of. Licences are Apache-2.0 and MIT except
where noted, so these are usable — but see the consumption rule below, and note that **two are
unlicensed, which withholds permission rather than granting it.**

| Repo | Licence | What it is | Verdict |
|---|---|---|---|
| **`decentralization-manager`** | Apache-2.0 | **Rust service + React UI for managing Canton *Decentralized Parties*** — coordinated multi-party onboarding, threshold key custody, governance-contract deployment requiring multi-party signatures, membership and threshold changes by vote. The framework that powers cBTC, opened under their $1M+ Canton Foundation grant. Uses Daml templates for governance logic but is itself Rust infrastructure | 🟢 **Run alongside.** This is the answer to "our venue operator is one party holding one key" — make the operator a Decentralized Party. **Do not vendor** |
| **`cbtc-por-tools`** | Apache-2.0 | Proof-of-reserve tooling for cBTC (TypeScript) | 🟢 Read or run. Directly relevant to `docs/FIXING_METHODOLOGY.md` §7 — the reserve-attestation direction, i.e. the difference between "provably issued" and "provably backed" |
| `cantcost` | MIT | Canton cost exporter (Go) | 🟡 Run it for cost telemetry. Not a dependency |
| `cbtc-lib` | MIT | Mint / burn / transfer helpers for cBTC (Rust) | ⚪ **Skip** — no Rust in this project. Reference reading |
| `canton-lib` | MIT | General Canton helpers (Rust) | ⚪ Skip, same reason |
| `zed-daml-lsp` | Apache-2.0 | Daml language server for the Zed editor | ⚪ Local tooling only |
| `dlc-solidity` | MIT | Their EVM contracts | ⚪ Not relevant — this is the Canton side |
| **`api-collections-public`** | **none** | Yaak API collections for cBTC: setup, mint flow, transfer flow | ⚠️ **Read, never copy.** Confirms our choice-context endpoint path for path, and shows an `/app/*` surface whose paths differ from what `api.devnet.bitsafe.finance` actually served. Concrete hosts and party ids are withheld as sensitive. See `docs/ASSET_ONBOARDING.md` §3a |
| **`canton-onboarding-testnet`** | **none** | Guide to onboarding Canton into **their attestor network** | ⚠️ **Not a route to a participant**, despite the name. Archived Oct 2025, pinned to Canton 3.3.0. See `docs/ASSET_ONBOARDING.md` §3b |

**cETH is NOT theirs** — it is issued by **onRails**, a different registrar. A two-asset crypto
basket therefore needs two issuer integrations, not one.

## Operational resources that are not repositories

| Resource | Where | Note |
|---|---|---|
| **Token Standard interface DARs** | `github.com/canton-network/splice`, Apache-2.0 | The only third-party code **vendored** here (`deps/`), because the Daml compiles against it. Credited in `NOTICE`, terms in `licenses/` |
| **DA-hosted registry API (devnet)** | `https://api.utilities.digitalasset-dev.com` | No auth. One host serves many registrars — the registrar party is in the path. Found via `/config.js`; see `docs/ASSET_ONBOARDING.md` §4 |
| **cBTC faucet (devnet)** | `https://cbtc-faucet.bitsafe.finance` | min 0.01, max 1 per request. Offers carry a 7-day `executeBefore` — accept promptly |
| **Canton Foundation grants** | `canton.foundation/grants-program/` | Milestone-based, paid in CC, quarterly, no fixed cap. Submitted as a PR to `canton-dev-fund`. Funds developer tools and **reference implementations**. A non-member needs a member or Tech & Ops champion to sponsor the proposal |
| **Splice token standard docs** | `docs.sync.global` → *Token Standard APIs* | The spec does document that a choice context must be fetched from the registrar. What it does **not** give is which host serves it |

## The consumption rule, restated

This file has said it from the start and it still holds: **reference and build ON these; do not fork
them in.** `docs/ASSET_ONBOARDING.md` §8 has the long form — vendor only what you compile against,
depend on what a package manager can fetch, run everything else alongside. The single vendored
exception here is the Splice interface set, and it carries its licence and attribution with it.

## Every other issuer and oracle, checked — 13 August 2026

Searched the GitHub orgs of the institutions whose assets or feeds this project would touch. Most
publish **nothing Canton-specific**, which is itself worth knowing: it means the four facts in
`docs/ASSET_ONBOARDING.md` §1 have to be asked for, not found.

| Who | Canton/Daml repos | What that means for us |
|---|---|---|
| **Circle** (`circlefin`, 95 repos) | **none** | But the USDC→Canton path is public elsewhere — see xReserve below |
| **Chainlink** (`smartcontractkit`) | 🔴 **four** — see below | The only institution actively shipping Canton integrations |
| **RedStone** (`redstone-finance`, 88 repos) | **none** | They run as a Canton participant and advertise NAV feeds, but publish nothing Canton-specific |
| **Ondo** (`ondoprotocol`, 15 repos) | none | USDY is not reachable from here |
| **BlackRock** (`blackrock`, 13 repos) | none | BUIDL is not reachable |
| **Franklin Templeton** | org exists, **0 repos** | BENJI is not reachable |
| **Hashnote** | no org found | USYC is modelled, never held |
| **onRails** (cETH issuer) | **no org found** under the obvious names | cETH's four facts must come from them directly. ⚠️ **cETH is onRails', NOT BitSafe's** — confirmed again here |

### Circle USDC on Canton — the asset is called **USDCx**

`github.com/digital-asset/xreserve-deposits` · **0BSD** (public-domain-equivalent, no attribution
required) · TypeScript · last pushed 2026-02-20.

A sample implementation of the real mechanism: deposit USDC into **Circle's xReserve contract on
Ethereum**, an attestation of that deposit is observed on Canton, and a named party may then mint the
equivalent **USDCx**. Works against Sepolia or Ethereum mainnet.

🟢 **This is the closest thing to a public route to real Canton USDC**, and the licence lets us use
the code outright. What its README does **not** say — and what we would still have to ask for — is
who issues USDCx, whether it implements `HoldingV1`, and its registrar party id. So: the mechanism is
public, the four facts are not.

### Chainlink on Canton — read this, it is the competitive picture

| Repo | Licence | Why it matters |
|---|---|---|
| **`data-streams-canton`** | **MIT** | *"Chainlink Data Streams Canton Integration."* Their low-latency price feed, on Canton. **Read it** — it shows exactly how a price is delivered onto Canton today |
| `ccip-starter-kit-canton` | MIT | Starter kit for Chainlink on Canton |
| `chainlink-canton` | NOASSERTION | Check the licence before touching it |
| `go-daml` | Apache-2.0 | A Go Daml client |

**This is the "do not claim the transport layer" point, made concrete.** Chainlink *delivers*
prices; CrossDesk *produces* one, from a sealed auction and a K-of-N attestation. Those are different
layers, and `data-streams-canton` being MIT means the delivery shape can be read rather than guessed.
The constructive reading: a CrossDesk fixing could be **distributed** over rails like these, which
makes an oracle a channel rather than a rival.

### What a CUSTOMER would need from Digital Asset

Not this repository, and not our DAR alone:

| They need | From where | Note |
|---|---|---|
| A **participant node** | `canton-network/splice` (Apache-2.0, pushed today) ships the validator bundle; `digital-asset/cn-quickstart` (0BSD, actively maintained) is the app-side wiring | Canton **Enterprise** is licence-gated; the Splice validator is the practical route |
| Onboarding to the **Global Synchronizer** | the network | ⚠️ Not obtainable from code. Needs an onboarding secret or sponsor — this is the standing blocker |
| The **Daml SDK** | `digital-asset/daml` | Free for development. Match the node: build LF 2.x for a Canton 3.x participant |
| A **secure deployment reference** | `digital-asset/ex-secure-canton-infra` | ⚠️ `NOASSERTION` licence and last pushed June 2025 — verify before relying on it |
