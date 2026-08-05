# Researching obligated selective disclosure — a programme

**The one thing CrossDesk asserts that nobody has tested.**

`docs/IMBALANCE_PUBLICATION_EVIDENCE.md` establishes the record: every component of this
design has precedent, but the *combination* — a closing-auction imbalance shown only to
parties who have signed an enforceable obligation to absorb it, with the seat open to all
comers — has never been run at scale and never been measured. Section 5.2 states it
plainly: *"No study shows selective disclosure beats broadcast for the imbalance side.
None was found — in either direction."*

This folder is how you would find out.

---

## The hypothesis, stated so it can fail

> Replacing broadcast imbalance publication with **obligated, contestable, non-exclusive
> disclosure** attracts *more* offsetting liquidity to the close, and therefore produces
> closing prices that are **less dislocated** — closer to the pre-auction reference and to
> the following day's open — than either broadcasting to all subscribers or publishing
> nothing.

Three sub-claims, each independently falsifiable:

**H1 — Participation.** More size shows up to offset the imbalance than under broadcast,
because the information is paired with a duty rather than a subscription fee.

**H2 — Dislocation.** The printed close lands nearer the reference price, and reverts less
overnight. (Reversion is the standard proxy for a price that moved further than
information justified.)

**H3 — No strategic withholding.** Providers do *not* hold volume back to the last moment.
This one matters because TSX documented the opposite under *more frequent broadcast*
messaging, and had to add a randomised auction end as a countermeasure.

**The honest prior:** H1 and H2 could easily go the other way. Broadcast's 1.7bps toll on
Nasdaq is low *because many capitalised firms see the same number simultaneously and
compete the premium away* (§5.2). A design with fewer informed participants might produce a
**worse** print, not a better one. That is the load-bearing risk and the simulation must be
built to detect it, not to avoid it.

---

## ⚠️ The problem you correctly identified

**You cannot simulate this without assuming how participants behave, and the assumption
drives the answer.** An agent-based model where liquidity providers are modelled as
generous produces a design that works; the same model with predatory agents produces one
that fails. Neither result is evidence about the real world — both are restatements of the
input.

This is the central methodological problem of market-structure simulation and it has no
clean solution. What it has is a discipline:

### 1. Never report a point estimate. Report the range across behavioural models.

Run the *same* venue design against a deliberately hostile spread of agent populations —
see `01_SIMULATION_DESIGN.md` §3. The output is not "the close is 4bps better." It is
"the close is between 6bps better and 3bps worse, and here is exactly which behavioural
assumption flips the sign."

### 2. The robust conclusions are the only real ones.

If a result holds across *every* plausible agent population, that is a genuine finding
about the mechanism rather than about your priors. If it flips, **you have learned
something more useful than a number: you have learned what the answer depends on** — and
that is precisely what to go and measure in the field.

### 3. Calibrate against the empirical anchors that do exist.

A simulation that cannot reproduce known facts is not evidence about unknown ones. Three
hard targets, all from the evidence doc:

| Anchor | Value | Source |
|---|---|---|
| Adverse repricing after broadcast | **5.5 bps within 300ms** | Nasdaq's own filing |
| Net toll on the MOC sender under broadcast | **~1.7 bps** | ibid. |
| Broadcast induces withholding | qualitative, documented | TSX primary document |

**Reproduce the broadcast baseline first.** If your model cannot generate ≈5.5bps of
adverse repricing under a broadcast regime, it is not yet a model of this market and its
verdict on selective disclosure is worthless.

---

## The better route, if it is available: real data

Simulation is what you do when you have no field data. **You may not need to be in that
position**, because the evidence doc already identifies unexploited natural experiments —
including one it calls *"the cleanest unexploited identification available"* (§5.2 checklist
row 5).

See `02_NATURAL_EXPERIMENTS.md`. If any of those datasets is reachable, a difference-in-
differences on a real regime change beats any simulation, and it beats it decisively.

**Order of preference:**

1. Natural experiment on a real disclosure regime change (`02_`)
2. Simulation calibrated to the broadcast baseline, reported as a range (`01_`)
3. Anything else

---

## Files

| File | What it is |
|---|---|
| `01_SIMULATION_DESIGN.md` | the agent-based design, the behavioural spread, what to measure |
| `02_NATURAL_EXPERIMENTS.md` | the real-data route, and which regime changes are still unstudied |

## Scope note

None of this is required to ship the venue, and none of it exists yet. This folder is a
research programme, not a claim of results. **Do not cite anything here as a finding.**
