# Onboarding a real CIP-56 asset

**No code change is required to add an asset.** A registrar is configuration. This document is
the schema of what must be obtained per asset, and the one asset for which it is all known.

Everything here is **per network**. A devnet registrar party is not the mainnet one, and a party
id is a key commitment rather than a name, so a devnet value used against mainnet does not
"mostly work" — it matches nothing.

---

## 1. The four facts

For each asset the fund is to hold for real:

| # | Fact | Why it is needed | Who has it |
|---|---|---|---|
| 1 | **Registrar / admin party id, in full** — including the `::1220…` fingerprint | This is the real-or-fake discriminator. It goes in `expectedIssuer` on the basket component, and it keys the registry URL. The fingerprint hashes that namespace's public key, so it cannot be impersonated — but a truncated or label-only form matches nothing | the issuer |
| 2 | **Instrument id string** — e.g. `CBTC` | Matched against the holding's `instrumentId.id` | the issuer |
| 3 | **Registry API base URL** — base, not a path | `TransferInstruction_Accept` fails with `Missing context entry for: utility.digitalasset.com/transfer-rule` until a `ChoiceContext` and its disclosed contracts are fetched from the registrar's registry. The registrar's `TransferRule` is not visible to the receiver | the issuer, or `/config.js` (§4) |
| 4 | **Confirmation the holdings implement `HoldingV1`** | The desk queries by INTERFACE, never by template — other issuers' assets are on their own templates | the issuer |

Without 1 and 3 the asset cannot be received. Without 1 the fund cannot tell it apart from a
lookalike.

## 2. Known-good: cBTC on HackCanton devnet

The one asset where all four are established, by having actually claimed 4.16 of it:

```
registrar    cbtc-network::12202a83c6f4082217c175e29bc53da5f2703ba2675778ab99217a5a881a949203ff
instrument   CBTC
registry     https://api.utilities.digitalasset-dev.com        (Digital Asset hosted)
HoldingV1    confirmed — verified by interface query across six parties
faucet       https://cbtc-faucet.bitsafe.finance               (min 0.01, max 1 per request)
```

Configured as:

```
REGISTRY_REMOTE_URLS=cbtc-network::12202a83c6f4082217c175e29bc53da5f2703ba2675778ab99217a5a881a949203ff=https://api.utilities.digitalasset-dev.com
```

and pinned on the basket leg:

```json
{ "instrumentId": "CBTC", "unitsPerShare": "0.002",
  "expectedIssuer": "cbtc-network::12202a83c6f4082217c175e29bc53da5f2703ba2675778ab99217a5a881a949203ff" }
```

## 3. Not yet established

| Asset | Issuer | What is missing |
|---|---|---|
| **cETH** | onRails | All four. onRails' integration page states cETH is a CIP-0056 asset, so the same flow should apply unchanged — the accept script already reads each offer's registrar from `instrumentId.admin`. **Try §4 first: one Digital-Asset-hosted host serves many registries, because the registrar party is in the path.** So `api.utilities.digitalasset-dev.com` may already serve cETH |
| **USDC on Canton** | Circle | All four. The `USDC` used in the demo baskets is self-issued and is cash-of-account only, not Circle's asset |
| **Canton Coin (Amulet)** | the network | ⚠️ **Different shape — do not assume the Utility Registry pattern.** Amulet is the network's own token with its own registry served by the Splice/scan APIs, and its admin is the DSO party rather than a Utility registrar. Its four facts come from the validator's scan endpoints, not from `api.utilities.*` |

## 3a. Corroboration — the issuer's own public API collection

BitSafe publish API collections at `github.com/DLC-link/api-collections-public` (Yaak format).
Checked 13 August 2026. Two things worth knowing:

**It confirms the endpoint in §1, path for path**, which was worked out here without their docs:

```
POST /api/token-standard/v0/registrars/{registrar}/registry/transfer-instruction/v1/{contractId}/choice-contexts/accept
```

It also documents an app-side surface this project has only partly seen —
`POST /app/get-account-contract-rules`, `POST /app/get-token-standard-contracts` and, for the mint
flow, `POST /app/get-bitcoin-address`. Note the paths differ from the
`/cbtc/v1/token-standard-contracts` observed on `api.devnet.bitsafe.finance`, so at least one of the
two has moved. Ask which is current rather than guessing.

**It does not give the four facts.** The concrete hosts, registrar party ids and instrument ids live
in the collection's environment section, which the public repository does not expose — their README
says those fields are "encrypted, as they are sensitive and specific to your setup". So §1 stands:
the four facts come from the issuer.

⚠️ **The collection carries no licence, so it is all rights reserved.** Read it; do not copy it into
this repository. Endpoint paths are facts and are recorded above in our own words. The collection
file itself is theirs.

## 3b. Not a route to a participant

`github.com/DLC-link/canton-onboarding-testnet` looks like it should solve the node problem. It does
not, on two counts: it is **archived (October 2025)** and pinned to **Canton 3.3.0**, and it is a
guide to joining **their attestor network** — becoming a cBTC attestor — rather than to obtaining a
participant of your own. Also unlicensed. Read for context; do not depend on it.

## 4. Finding a registry base URL without asking

A hosted registry SPA keeps its backend URL in a **runtime config file**. This is how the devnet
URL above was found, and it needs no ledger and no credentials:

```
GET https://registry.dev.app.digitalasset.com/config.js   ->  utility_backend_url
GET https://registry.app.digitalasset.com/config.js       ->  (mainnet equivalent)
```

Verify a discovered host before trusting it: its `dsoPartyId` must match the local validator's
`/api/validator/v0/scan-proxy/dso-party-id` byte for byte.

**Try `/config.js` first when hunting any hosted service's API host.**

## 5. Finding a registrar party id without asking

Requires a live participant that can see at least one holding or pending transfer offer of the
asset — so it does not work while no node is reachable:

1. Query `/v2/state/active-contracts` **by interface**, filtering on
   `#splice-api-token-holding-v1:Splice.Api.Token.HoldingV1:Holding` (or the transfer-instruction
   interface for pending offers). Use the `#package-name` form for filters; an explicit package id
   matches nothing.
2. Read `instrumentId.admin` off the interface view. That is the registrar, in full.

## 6. The message to send an issuer

> We hold your asset on the HackCanton devnet participant and want to reference it from a fund.
> Four things, and they are all facts you already have:
>
> 1. The full registrar party id for <asset>, including the `::1220…` fingerprint — for devnet and
>    for mainnet, if they differ.
> 2. The instrument id string.
> 3. Your registry API base URL (base, not a path) — the one serving
>    `/registry/transfer-instruction/v1/{cid}/choice-contexts/accept`.
> 4. Confirmation your holdings implement `HoldingV1`.
>
> With those we can receive it and pin it, and nothing about the integration is bespoke to us.

## 7. What this does and does not prove

Pinning the registrar proves **the issuer authorised this holding** — a ledger fact, enforced by
the confirmation protocol, because a transaction requiring that party's authority cannot commit
without the participant hosting it confirming.

It does **not** prove the asset is backed. Whether a wrapped asset's reserves exist is an
off-ledger custody question answered by the issuer's attestor network and any proof of reserve
they publish. Those are two different claims and only the first is provable here.

---

## 8. Using someone else's code — the three ways, and only one involves copying

Checked 13 August 2026 against `github.com/DLC-link` (14 public repos).

| How | When it applies | What lands in this repository |
|---|---|---|
| **Vendor** — copy it in | Only when we **compile against** it and need a reproducible build | The artifact, plus its licence text and a `NOTICE` entry |
| **Depend** — fetched at build time | A package manager resolves it | A version in a manifest. Nothing else |
| **Run alongside** | It is a separate service we deploy and talk to over an API | Configuration and a paragraph of documentation |

**We already do the first one correctly, once:** the six `splice-api-token-*` DARs in `deps/`, because
the Daml compiles against those interfaces. `NOTICE` credits them and `licenses/Apache-2.0.txt`
carries the terms, as Apache-2.0 §4 requires. That is the *only* case here where copying is right.

### Per repository

| Repository | Licence | What to do |
|---|---|---|
| `decentralization-manager` | Apache-2.0 | **Run alongside.** Rust service + React UI for managing Canton *Decentralized Parties* — threshold key custody at the identity layer. Do not vendor. If its Daml governance templates are ever needed they arrive as a DAR data-dependency, exactly like Splice |
| `cbtc-por-tools` | Apache-2.0 | Read or run. Proof-of-reserve tooling — relevant to the reserve-attestation direction in `FIXING_METHODOLOGY.md` §7. Vendor only if we import it |
| `cantcost` | MIT | Run it if we want Canton cost telemetry. Not a dependency |
| `cbtc-lib`, `canton-lib` | MIT | **Skip.** Rust libraries; this project has no Rust. Reference reading only |
| `zed-daml-lsp` | Apache-2.0 | Local editor tooling. Nothing to do with this repository |
| `api-collections-public` | **none** | Read, never copy — see §3a |
| `canton-onboarding-testnet` | **none** | Archived, wrong problem — see §3b |

🔴 **Two of those have no licence, which means all rights reserved — not "free to take".** It is the
same rule this repository applies to itself: absence of a licence withholds permission rather than
granting it. Copying an unlicensed file into a repository we then licence commercially is the kind of
thing a buyer's counsel finds. Facts extracted from reading them (an endpoint path, a prerequisite)
are ours to write down; their files are not ours to ship.
