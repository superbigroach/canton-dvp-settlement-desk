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
| **cn-quickstart** ⭐⭐⭐ | Official "start quick" bootstrap for a Canton app + Devnet deploy | **Fastest path to a deployed submission** — bootstrap your app on top of it. |
| **daml-finance** ⭐⭐⭐ | Official Daml **Finance** library: instruments, holdings, settlement, DvP | **Use as a dependency** — build the settlement desk on its primitives instead of hand-rolling. Institutional-grade. |
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
3. The hand-rolled DAML in `daml/` here is your **learning foundation** (shows you understand the primitives); the *submission* can bootstrap from the official quickstart + library.

> **Verify the Daml SDK / Splice version** Devnet expects (from `cn-quickstart` / HackCanton onboarding) and set it in `daml.yaml`. Real **cETH is issued by onRails** — you can't self-mint it on Devnet; request it via their form and align `Token.daml`'s `issuer`/`instrument` to onRails' registry values.
