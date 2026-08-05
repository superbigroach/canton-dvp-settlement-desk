# Natural experiments — the route that beats simulation

**Prefer this.** A difference-in-differences on a real disclosure regime change is worth
more than any agent-based model, because it does not require you to assume how participants
behave — it observes it.

`docs/IMBALANCE_PUBLICATION_EVIDENCE.md` §5.2, checklist row 5, records the striking fact
that makes this route available:

> **Peer-reviewed evaluation of any closing-imbalance disclosure regime change** (NYSE 2019,
> Nasdaq EOII 2019 & 2021, TSX 2021, Euronext AVD 2025, Xetra 2026): **Not found** after
> multiple search strategies.

Five regime changes at major venues. **Nobody has published on any of them.** That is not a
gap in your reading — it is a gap in the literature, and it is a research opportunity
sitting in plain sight.

---

## The candidates, ranked

### 1. Nasdaq opening-cross EOII — live 26 April 2021
**The evidence doc calls this "the cleanest unexploited identification available."**

Why it is the best target:
- A **dated, discrete** change to what is disclosed before a cross
- The **closing** cross is untouched, so it is a natural within-venue control — same
  securities, same participants, same day, different mechanism
- US equities, so the data is obtainable

**Design:** difference-in-differences, opening cross (treated) vs closing cross (control),
across the 26 April 2021 boundary. Outcomes: dislocation, reversion, arrival timing of
offsetting volume.

### 2. Euronext AVD — live 8 December 2025
The closest living relative to CrossDesk's design: the residual imbalance is worked
**without being published**. Euronext's own 2026 research frames that residual as *"untapped
liquidity"* to be interacted with rather than broadcast.

- ✅ Directly on-point — it is the "shield the residual" half of the design
- ⚠️ Recent, so post-period data is thin, and European data is harder to obtain
- ⚠️ Confounded: Xetra moved toward *more* disclosure in the same T7 release cycle, so
  "the direction of travel" is genuinely two-directional. Do not claim a trend.

### 3. TSX 2021 imbalance-messaging change
Valuable because TSX **documented the mechanism in a primary source** — more frequent
messaging created an incentive to hold volume back, and they added a randomised freeze to
counter it. A study here tests a stated causal claim rather than fishing for one.

### 4. Xetra 2026 order-book opening
Europe's largest auction venue moving toward *more* disclosure. The mirror image of AVD, and
the two together are the cleanest available evidence on direction.

### 5. Hong Kong — abolished, then rebuilt
The strongest cautionary case in the whole record: a closing auction removed after
dislocation, then redesigned and reintroduced. Already partly documented in the evidence doc
(§1.4.1) against HKEX primary sources. Less a DiD than a case study, but the most vivid.

---

## What none of them test

⚠️ **Be precise about this.** Every experiment above is a change in *how much* is published
to *everyone*. **None of them tests disclosure conditional on an enforceable obligation.**

The nearest real-world analogues to that are:
- **NYSE Rule 104** — the DMM's privileged view paired with an enforceable duty. But it has
  been in place for decades with no clean on/off boundary to exploit.
- **Börse Frankfurt's Continuous Auction** with Market Maker / Specialist models — imbalance
  side to the designated party, withheld from the market. Genuinely the right shape, but
  event-triggered auctions in illiquid instruments, not a scheduled market-wide close.

So the natural-experiment route can establish **what broadcast does**. Establishing what
*obligated* disclosure does still needs either a simulation (`01_`) or a venue willing to
run it — which is the actual long-term argument for building CrossDesk on a network where
such a venue can exist.

---

## The honest framing for any output

Whatever this programme produces, the claim it can support is bounded:

> Broadcast has a measured cost to the imbalance side and a documented tendency to induce
> withholding. Two live venues have chosen non-disclosure of the residual. Whether pairing
> disclosure with an obligation improves on either is **untested**, and this work is the
> first attempt to test it.

That is a real contribution. **"We proved our design is better" is not**, and would not
survive the first person who checks.

---

## If someone asks in a Q&A

> There are five closing-auction disclosure regime changes at major venues since 2019 and
> no peer-reviewed evaluation of any of them. The Nasdaq opening-cross change in April 2021
> is the cleanest identification available — the closing cross is an untouched within-venue
> control. That is the study I would run, and it would test broadcast, not us. What we do
> has no field precedent at scale, and I would rather say that than pretend otherwise.
