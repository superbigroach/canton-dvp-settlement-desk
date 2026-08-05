# Simulation design

How to test obligated selective disclosure when no field data exists — and how to stop the
result being a restatement of your own assumptions.

---

## 1. The three regimes to compare

Hold the auction mechanics identical across all three. **Only the disclosure rule changes.**
That isolation is the whole experiment; if anything else differs, the comparison is void.

| Regime | Who learns the residual imbalance, and when |
|---|---|
| **B — Broadcast** | every subscriber, simultaneously, through the call phase. The Nasdaq/Xetra/Euronext status quo. |
| **N — None** | nobody. The Cboe Market Close / Euronext AVD case. |
| **O — Obligated** | only parties holding a live mandate. CrossDesk. |

Run **O** at several seat counts — 1, 3, 10, unlimited. This is the most important sweep in
the whole programme, because §5.2 names single-recipient concentration as the design's
central weakness: broadcast's toll is small *because competition among many informed firms
competes the premium away*. If O's advantage disappears below some seat count, that number
is a design parameter you need to know, and it belongs in the venue's rules.

---

## 2. What to measure

Per simulated close, per regime:

**Price quality**
- `|print − pre-auction reference|` in bps — raw dislocation
- **Overnight reversion**: `(next open − print)` in bps. *The primary outcome.* A print
  that reverts moved further than information justified.
- Executed volume at the cross, and the fill rate on unpriced (MOC) interest

**Who paid**
- Realised cost to the imbalance side, in bps — the sender's toll
- Realised P&L to offsetting providers — is the seat profitable, and how profitable?

**Behaviour**
- Arrival-time distribution of offsetting volume. **Late-skewed = strategic withholding**,
  the failure TSX documented under broadcast.
- Fraction of the residual actually absorbed
- Under **O**: seat take-up rate, and the rate of revoked/barred mandates

**The headline comparison is reversion, not dislocation.** A close can be far from the
reference for perfectly good reasons — genuine news. What indicts a mechanism is a print
that snaps back the next morning.

---

## 3. The behavioural spread — the part that decides everything

⚠️ **Do not pick one agent population and report its result.** Run all of these, and report
the range. The spread *is* the finding.

| Population | Offsetting agents behave as… |
|---|---|
| **P1 Competitive** | risk-neutral, compete margin toward zero. The optimistic bound. |
| **P2 Rent-seeking** | extract the maximum the obligation permits — supply exactly the committed size, at the worst permitted price in the band, never more |
| **P3 Predatory** | trade *ahead* of the disclosed imbalance rather than against it. The adverse case, and the one broadcast critics have in mind. |
| **P4 Withholding** | delay commitment to the last instant to maximise information before acting — the TSX-documented behaviour |
| **P5 Capacity-constrained** | willing but balance-sheet-limited; absorb the commitment and no more |
| **P6 Mixed** | an empirically plausible blend. **Report this last**, so it cannot anchor the reader. |

**The decision rule, fixed in advance:**

- A conclusion that holds across **P1–P5** is a claim about the mechanism.
- A conclusion that holds only under **P1** is a claim about your optimism.
- If **P3** flips the sign, say so in the abstract. That is the result most worth knowing.

### The imbalance side must also be modelled honestly

MOC senders are not passive. Model them as **constrained**: an index fund's mandate is
literally to own the closing price, so it *cannot* withdraw when the print goes against it.
That constraint is the whole reason predictability costs it money — Chen/Noronha/Singal put
that cost at $1.0–2.1bn a year. A simulation whose MOC agents can walk away has removed the
thing being studied.

---

## 4. Calibration gate — run this before believing anything

**The model must reproduce the broadcast baseline before its verdict on anything else
counts.** Under regime **B**, with population **P6**:

- adverse repricing ≈ **5.5 bps within 300ms** of publication
- net toll to the MOC sender ≈ **1.7 bps**
- offsetting arrivals **late-skewed** (the TSX withholding pattern)

Miss these and the model is not yet a model of this market. Tune the agent population to
hit them — **then freeze it** and run O and N without further tuning. Tuning after seeing
the O result is how simulation studies produce whatever their authors wanted.

---

## 5. What this can and cannot settle

**Can:**
- whether the mechanism is *coherent* — does an obligated seat clear the residual at all
- the seat-count threshold below which competition is too thin
- which behavioural assumption the answer is most sensitive to
- whether the fixed-band design (see below) leaves a hole in fast markets

**Cannot:**
- what real institutions would actually do. No simulation settles that.
- whether the seat would be taken up at all in a real market — that is a commercial
  question about balance sheet and mandate, not a modelling one

---

## 6. Two design parameters worth sweeping while you are in there

**Band: fixed vs moving.** CrossDesk pins `maxBandBps` to the `anchorPrice` at posting.
NYSE's DMM obligation instead tracks a moving NBBO. The fixed band gives a bounded, priceable
worst case — which is what makes the seat signable — but the obligation **lapses exactly when
the market has moved most**, which is when the liquidity was most needed. Sweep both and
measure absorbed fraction in high-volatility closes specifically.

**Random auction end.** Xetra ends its call phase at a random instant, in its own words "in
order to avoid price manipulation," and TSX added the same after observing withholding. If
P4 shows withholding under any regime, test whether a random end suppresses it — that is a
cheap fix with strong precedent and it is not currently in the design.

---

## 7. Suggested stack

Nothing here is exotic. An event-driven matching engine plus an agent loop is a few hundred
lines. The hard part is §3, not the code.

- Reuse the real uncrossing logic rather than reimplementing it — `discoverPrice` in
  `daml/MarketOnClose.daml` is a pure function over (anchor, book). Porting it keeps the
  simulated venue honest to the shipped one.
- Fixed seeds, and **report the seed**. Market-structure results are notoriously
  seed-sensitive.
- Sweep: regime × population × seat count × band type × volatility. That is the table.
