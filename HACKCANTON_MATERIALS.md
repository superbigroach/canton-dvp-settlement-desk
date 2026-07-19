# HackCanton — Materials Reference (saved from the League materials page)

Everything needed to build on Canton, in one place. Local reference — links
current as of July 2026. (Sibling doc: `CANTON_RESOURCES.md` = official repo
catalog and how to use each.)

## 🚀 Start Here
| Resource | Description | Link |
|---|---|---|
| Canton 101 | Network architecture, APIs, SDKs, core concepts | canton-101.vercel.app |
| CN Quickstart | Fastest way to scaffold a Canton Network app | github.com/digital-asset/cn-quickstart |
| Canton Developer Resources | Official builder entry point | canton.network/developer-resources |
| Official Docs | Full Canton Network documentation | docs.canton.network |
| Build Docs (DA) | Quickstart, JSON Ledger API, smart contracts, how-tos | docs.digitalasset.com/build/3.5 |
| DAML Studio | Browser IDE for DAML — no local setup | damlstudio.tenzro.network |

## 📚 Core Documentation
| Resource | Description | Link |
|---|---|---|
| Sync.Global Docs | Network overview, validators, Global Synchronizer | docs.sync.global |
| Canton Protocol | Privacy-first protocol deep dive | canton.network/protocol |
| Intro to Canton | How Canton works under the hood | docs.daml.com/canton/about |
| Canton Network APIs | Overview of all APIs | docs.digitalasset.com/build/3.4 |
| Canton GitHub | Official open-source repo | github.com/digital-asset/canton |
| DAML Getting Started | Templates, contracts, choices | docs.daml.com/canton/tutorials/getting_started |
| Install Canton + DAML SDK | Local dev installation | docs.daml.com/canton/usermanual/installation |
| Canton Dev Guide | Opinionated dev guide: patterns, tokenomics, security, Featured App | github.com/JohnLilic/canton-dev-guide |
| DPM Framework | Build/test framework for DAML apps | github.com/digital-asset/dpm |

## 🛠 SDKs & Tooling
| Resource | Description | Link |
|---|---|---|
| Go DAML SDK (NODERS) | Go client for DAML ledger | github.com/noders-team/go-daml |
| Go Wallet DAML SDK | Go wallet flows / app integration | github.com/noders-team/go-wallet-daml |
| splice-wallet-kernel | dApp Development Kit for Canton | github.com/hyperledger-labs/splice-wallet-kernel |
| dazl-client (Python) | Python ledger client — AI/data pipelines | github.com/digital-asset/dazl-client |
| JSON Ledger API | HTTP API — any language | docs.digitalasset.com/build/3.5 |

## 🤖 AI Tools
| Resource | Description | Link |
|---|---|---|
| Build on Canton MCP | Local MCP plugin for Claude — Canton dev knowledge base | github.com/Jatinp26/Build-on-Canton-MCP |
| DAML Studio | AI-powered browser IDE | damlstudio.tenzro.network |

## 🔍 Explorers & Data
| Resource | Link |
|---|---|
| Lighthouse Explorer | lighthouse.cantonloop.com |
| Modo Agentic API | docs.modo.link/agentic-api/intro |
| CCView Explorer | ccview.io |
| CCView Indexing API | docs.ccview.io |

## 💳 Wallets & Identity
| Resource | Link |
|---|---|
| Canton Ecosystem Wallets | cantonecosystem.com |
| Five North ID SDK | docs.fivenorth.io/id-sdk |

## 🏗 Featured App (network rewards)
Requirements: a running node connected to the Global Synchronizer; active
on-chain activity; activity markers per the 1.15 marker-to-gas ratio rule; a
request to the GSF Tokenomics Committee. See: Canton Network Ecosystem (live
Featured Apps), Sync.Global Tokenomics, Canton Dev Guide → Featured App section.

## 💡 DAML Contract Patterns (Canton Dev Guide)
AccessControl · Escrow · Multisig · Vesting · Timelock · Voting

## 🖧 HackCanton DevNet node (hackcanton-01, NODERS)
| Resource | Link |
|---|---|
| Wallet | https://wallet.validator.hackcanton-01.devnet.naas.noders.services |
| CNS | https://cns.validator.hackcanton-01.devnet.naas.noders.services |
| gRPC Ledger API (port 5001) | ledger-api-grpc.participant.hackcanton-01.devnet.naas.noders.services:443 |
| JSON Ledger API (port 7575) | https://ledger-api-json.participant.hackcanton-01.devnet.naas.noders.services:443 |
| Validator/Scan API (port 5003) | https://validator-api-http.validator.hackcanton-01.devnet.naas.noders.services:443 |
| Logs (Grafana/Loki) | https://grafana.participant.hackcanton-01.devnet.naas.noders.services |
| Audience | https://hackcanton-01.devnet.naas.noders.services |
| OIDC token endpoint | https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token |

Obtain a bearer token (client `web-app-ui-hackcanton-01-devnet`):
```bash
curl -sS 'https://keycloak.naas.noders.services/realms/noders-appsfactory/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=web-app-ui-hackcanton-01-devnet' \
  --data-urlencode 'username=<YOUR_EMAIL>' \
  --data-urlencode 'password=<YOUR_PASSWORD>' \
  --data-urlencode 'scope=openid daml_ledger_api offline_access'
```

**Grafana log search for a denied tid** (Loki datasource proxy; Bearer = the same
Keycloak access token):
```
GET https://grafana.participant.../api/datasources/proxy/uid/loki/loki/api/v1/query_range
  ?query={namespace=~".+"} |= "<tid>"
```
How the actAs mystery was solved with it: the participant log showed
`Claims are only valid for userId '<sub>'` → in Ledger API v2 the submission's
`applicationId` must EQUAL the token's userId (`LEDGER_APPLICATION_ID=<sub uuid>`),
and actAs must reference FULL party ids (`label::1220…`), not labels.

## 🤖 AI + Canton integration ideas (from materials)
On-chain AI agents · tamper-proof AI-output data layer · verified inference ·
multi-party/federated ML via sub-transaction privacy · AI-gated contract choices.

## 📋 Judging criteria
| Category | What judges look for |
|---|---|
| MVP Materials | Prototype, demo, mockups, public repo |
| GTM Materials | Strategy, channels, positioning, acquisition hypotheses |
| ICP / Audience | Target user + specific pain point |
| Value / Problem | What's solved, why it matters, why now |
| Metrics / Validation | Interviews, tests, validation notes, success criteria |
| Pitch Materials | Problem → solution → why us |
