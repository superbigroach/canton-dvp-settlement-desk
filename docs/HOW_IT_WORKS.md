# CrossDesk — how it actually works

**Written 2026-08-03, for the HackCanton Season 2 final (Wed 2026-08-05, 14:00 UTC).**

Read this once end to end. It explains the machine as it stands *today*, after the finalist-
feedback rebuild — not as the original submission worked.

---

## 1. What it is, in one paragraph

A **closing auction on Canton**. Traders lodge **sealed** orders that nobody else can see — not
other traders, not the auditor. At the close, the venue uncrosses the whole book in **one atomic
transaction**: it discovers a single clearing price from the orders themselves, allocates fills by
price priority, moves every leg, and issues a receipt per fill. Either the entire close prints, or
none of it does.

The reason it has to be Canton: on a public chain the mempool *is* the book. Every resting order is
visible before it executes, which is the exact opposite of a sealed auction. On Canton a contract
is visible only to its signatories and observers, so a sealed order is a **native primitive**.

---

## 2. The lifecycle — four steps, all operator-driven

There is **no timer and no scheduler.** Nothing fires by itself. A Daml contract cannot trigger
itself — nothing on a ledger executes unless a party submits a transaction. You drive it.

| # | Step | Who | What happens on the ledger |
|---|---|---|---|
| 1 | **Create the auction** | venue | A `ClosingAuction` with an **anchor** price, `isOpen = True`, and counters at zero |
| 2 | **Lodge sealed orders** | each trader | A `SealedOrder` signed by **operator + that trader only**. Their backing is reserved and disclosed to the venue |
| 3 | **Seal the window** | venue | `CloseBidding` → `isOpen = False`. No more orders |
| 4 | **Run the close** | venue | `RunClose` discovers the price, allocates, settles everything, writes receipts — **one transaction** |

Steps 3 and 4 are one API call: `POST /api/moc/{auctionCid}/close`.

**The production answer, if a judge asks "what happens at 4pm?":** an off-ledger scheduler (a Daml
Trigger or a cron) would fire steps 3–4 on the clock. *The scheduler decides the moment; every rule
about who may do what, and at what price, stays on the ledger.*

---

## 3. Price discovery — the part that changed

### What it used to do (the criticism)

`RunClose` printed at `referencePrice` — **a number the operator typed in**. Every order carried a
`limitPrice`, but it was only used to decide who was *eligible*, never *where the trade printed*.
The book existed and was ignored. For a project whose thesis is *"plenty of settlement, no price
formation"*, the price was still being handed in from outside.

### What it does now

The **Xetra ladder** (T7 §11.1.1) with Euronext's nearest-the-reference final tie-break:

1. **Candidate prices** = every distinct `limitPrice` in the book, plus the anchor.
2. At each candidate `P`:
   `buyVol(P)` = all buys willing to pay **≥ P** · `sellVol(P)` = all sells willing to accept **≤ P**
   `executable(P) = min(buyVol, sellVol)`
3. **Pick the price that trades the most.**
4. Tie → **smallest imbalance**.
5. Still tied → **market pressure**: buy-heavy takes the **highest**, sell-heavy the **lowest**.
6. Still tied → **nearest the anchor**, then the lower price.

The anchor is demoted from *the answer* to a **last-resort tie-breaker**.

### Worked example

Anchor published at **255**:

| Party | Side | Qty | Limit |
|---|---|---|---|
| Alice | Buy | 10 | 258 |
| Dave | Buy | 5 | 252 |
| Bob | Sell | 8 | 250 |
| Carol | Sell | 9 | 256 |

| P | buyVol | sellVol | **executable** | imbalance |
|---|---|---|---|---|
| 250 | 15 | 8 | 8 | 7 |
| 252 | 15 | 8 | 8 | 7 |
| **255** *(anchor)* | 10 | 8 | **8** | 2 |
| **256** | 10 | 17 | **10** | 7 |
| 258 | 10 | 17 | **10** | 7 |

Max executable is **10**, tied at 256 and 258, imbalance tied at 7. Sell-heavy → take the lowest →
**prints at 256.** The venue said 255, where only 8 would have traded.

### "But shouldn't more buyers push the price up?"

**Yes, and it does.** Add a big buyer — Eve, 12 @ 262 — to that same book and the maximum shifts to
17 units, tied at 256 and 258, now **buy-heavy** → takes the **highest** → **prints at 258.**

The price walked up purely because buying arrived. Nobody adjusted anything.

**The distinction that matters:** the *residual* imbalance left over at the crossing price does not
push the price further — that gets rationed. The price already moved when the algorithm found where
the curves cross furthest.

---

## 4. Allocation — price priority first, then pro-rata

This changed after research into the actual rulebooks, and it is the subtler half.

**Orders priced *through* the print fill in FULL. Only the marginal level — the one the volume runs
out on — is rationed. Levels behind it get zero.**

- Xetra T7: *"the maximum of **one** order … can be partially executed"*
- Nasdaq Rule 4754(b)(3): class → price → display → time
- NYSE Rule 7.35B(h)(1): better-priced orders are *"guaranteed to participate"*

So in the example above, printing at 256: **Bob (250, through the print) fills all 8. Carol (256, at
the print) is rationed.** Previously both were pro-rated together — which meant a better limit
bought eligibility but *no precedence*, and the rational strategy became oversizing your order.

### Why at-the-print rationing is pro-rata by SIZE, not time

Real venues use time priority. This one can't, and the reason is worth saying out loud:

> `SealedOrder` carries **no on-ledger arrival timestamp**. The only "time" available at the close is
> the order of the array the operator hands to `RunClose` — so rationing on it would let the venue
> pick the marginal winner by permuting a list. **Pro-rata by size is invariant to that permutation.**

*"Real venues use time priority. I have no trustworthy clock in a sealed book, and the only ordering
available is one the operator controls. So I use the rule the operator can't game."*

### Rounding

Fills sum to the crossed volume **exactly**, by construction. The bounded rounding residual (≤1e-10
per order) is carried by the **largest order at the marginal level**, ties to the first in book
order. Whatever is delivered is what the `SettlementReceipt` says — they cannot disagree.

---

## 5. What happens to everything that doesn't fill

| Situation | Outcome |
|---|---|
| **Away from the cross** (Dave's 252 bid when the print is 256) | Never trades. **Cancelled on close**, reserved cash returned in the same transaction |
| **At the print, rationed** | Fills partially. Unfilled remainder returns to the trader |
| **Fill rounds to dust (0.0)** | Filtered out, order cancelled, balance untouched, **no receipt for a trade that didn't happen** |
| **Book doesn't overlap at all** | Executable volume is zero everywhere → **close aborts, nothing settles** |

Nothing rests to a next session. Same as a real MOC order: it fills or it dies.

---

## 6. The price collar

A thin book could otherwise print anywhere. Nasdaq's construction:

```
band = max($0.50, 10% of the anchor)
```

Both parts, because every venue with a percentage band also has an absolute floor (Nasdaq $0.50,
Euronext €0.02, NYSE $0.15/$1.00). Checked **after** the committee-fix validation, so the band is
provably centred on the attested anchor.

**A breach CLAMPS — it does not abort.** If the volume-maximising price falls outside the band, the
print is set **at the nearer boundary** and the cross is re-scored there: the crossed quantity is
smaller, the excess on the heavy side simply doesn't fill and returns to its traders, and **the
auction still prints.**

That matters most once unpriced MOC exists — an index fund sending MOC *must* own the closing
price, so a venue that cancels under stress is useless to exactly the participants who need it. It
also means one large order can no longer deny everyone else a close.

Clamping is provably the right price, not merely a safe one: `exec(P)` is the minimum of a
non-increasing and a non-decreasing function, so it is quasi-concave — the maximum over a band that
excludes the true peak sits at the nearer endpoint. **Clamping *is* constrained volume maximisation.**

🔴 The one case that still aborts, correctly: if the boundary itself trades zero, quasi-concavity
means nothing inside the band trades either — so there is no auction price at all. That is the
ordinary no-cross case, not the collar failing. Clamping bounds a price; it cannot manufacture a
counterparty.

---

## 7. Complete-order commitments — the trust fix

`RunClose` takes the order lists **from the operator**. Nothing used to force those lists to be the
whole book, so the venue could omit orders and move the print.

Now `ClosingAuction` carries `submittedCount` and `cancelledCount`, and `RunClose` asserts:

```
length buyOrders + length sellOrders == submittedCount − cancelledCount
```

plus a **distinctness check**, so the operator can't pad the list with a duplicate to satisfy the
count.

**Cost, stated openly:** `SubmitOrder` had to become consuming to maintain the counter, which
serialises submissions on one contract. That's real contention, accepted deliberately so the close
is provably over the complete book. Withdrawals route through the auction so the count stays
honest; a venue calling `VenueCancel` directly is **fail-safe** — the count then over-states the
book and its own close won't run. It can never manufacture a print over a truncated book.

---

## 8. Settlement — why the venue is the counterparty

Every leg moves through the operator as a **momentary central counterparty**: sellers pledge the
asset, buyers pledge cash, the venue pools both and redistributes — all inside one transaction.

That isn't a preference, it's forced by Daml's authority model: **a trader's authority only exists
inside a choice on a contract they signed.** Their own order is the only place their leg can move.

> *"The venue is the counterparty to every fill for exactly one transaction — long enough to net,
> not long enough to fail."*

---

## 9. What's CIP-56 and what isn't

**Genuinely Token Standard compliant:** `daml/TokenStandardDvp.daml` — six interfaces (`Holding`,
`TransferFactory`, `AllocationFactory`, `TransferInstruction`, `Allocation`, `AllocationRequest`)
and an atomic two-leg DvP over `AllocationRequest`. Allocations lock a real holding and name it in
`holdingCids`, rather than the shortcut Splice's own reference token takes.

**Still the legacy self-issued layer:** `Holding`, `Instrument`, `Settlement`, `MarketOnClose`,
`Basket`, `Agent`, `Governance` — i.e. **the auction centrepiece**. The two sets of cETH do not
interoperate.

**Say this first, before anyone asks.** Full detail in `docs/TOKEN_STANDARD_DVP.md`.

> *"Implementing CIP-56 doesn't make our cETH* the *cETH — it makes the venue registry-agnostic."*

**On Daml Finance:** it could not be used. Latest release is `sdk/2.10.0`, which emits **LF 1.x**;
this node is Canton 3.x and rejects LF 1.x. The 3.x asset layer *is* the Token Standard.

---

## 10. Known gaps — name them before a judge does

| Gap | Status |
|---|---|
| ~~No unpriced MOC order type~~ | ✅ **BUILT.** `limitPrice` is `Optional`; `None` = unpriced MOC. Eligible at every candidate price and allocated **ahead of** every limit order (Nasdaq 4754(b)(3)(A) class → price → time). An unpriced buy reserves `quantity × (anchor + collar band)` — the collar is what bounds an otherwise unbounded obligation, and is why the order type became possible |
| **No continuous session** | A real closing cross inherits the whole resting day book — that is the limit ladder MOC flow walks into. There is no continuous session here, so the ladder must come from LOC orders submitted directly into the auction |
| **No time priority** | Deliberate — see §4 |
| **No auction phases** | No call phase, no freeze/no-cancel window. Close is manually triggered |
| **No tick size, no lot size** | Not modelled |
| **Auction path isn't CIP-56** | See §9 |
| **Never run end-to-end** | Everything is verified by **33 passing Daml scripts and compiling backends**, not by a cross printing on a live ledger |

Deliberately **not** built: volatility interruptions and extensions. Xetra's documented end state is
*"terminated manually per FWB rules"* — a human — which would destroy the atomic-finality thesis.
Random end is unnecessary here too: **Daml authorisation makes late withdrawal impossible rather
than merely ill-timed**, which is strictly better than the market-structure workaround.

---

## 11. The demo, step by step

1. Create the auction with an anchor **deliberately away from where the book will cross**.
2. Submit orders as three or four different parties, **setting limits spread around the anchor** —
   use the Limit field; leaving it blank pins to the anchor and nothing interesting happens.
3. Show a trader's view: they see **only their own order**. That's the dark pool.
4. *(Optional)* Show the DLP's imbalance panel — aggregate only, no identities.
5. Run the close as the venue. **The print lands somewhere the venue never nominated.**
6. Show the receipts and balances: through-the-print orders full, at-the-print rationed, away-from-
   the-cross returned intact.

**The two lines:**

> *"The venue published 260. Nobody would have traded there. The book crossed at 256 and fifteen
> units printed — and the venue couldn't have moved that number if it wanted to."*

> *"And it can't cherry-pick. Omitting one cheap offer would have moved the print and halved the
> volume. The ledger refuses the close."*

---

## 12. Current state

```
daml build : clean
daml test  : 33 scripts, 33 ok, 0 failed
backend/         ./gradlew clean build  SUCCESSFUL
backend-devnet/  ./gradlew clean build  SUCCESSFUL
frontend/        tsc --noEmit           clean
```

Committed on branch **`feat/price-discovery-and-cip56`**. **Not pushed. Not deployed.**

🔴 **The DAR upload to devnet is admin-only on the node operator's side (Kiryl/NODERS).** Nothing
above is live until that happens — the hosted demo still runs the old package `72ec9833…`.

## Related documents

- `docs/REAL_AUCTION_MECHANICS.md` — the 567-line research spec: per-venue rules, citations, gap table
- `docs/TOKEN_STANDARD_DVP.md` — what is and isn't CIP-56
- `DEVNET_INTEGRATION.md` — the LF 1.14 → 2.2 / Ledger API v1 → v2 port
