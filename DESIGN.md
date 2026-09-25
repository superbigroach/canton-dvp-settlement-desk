# ETP Foundry — design system

The desk is a professional instrument, not a consumer app. The people who use it are a
custodian at a crypto firm signing a daily attestation in thirty seconds, an authorised
participant deciding whether to create a basket, and a risk analyst deciding whether to
trust us at all. None of them wants to be delighted. They want to be certain, fast.

So "addictive to use" here means what it means on a trading desk: dense, legible,
instant, and never surprising. Nobody enjoys software that is fun once. They keep coming
back to software that never wastes their time and never lies to them.

---

## 1. The one idea

**Gold means a human signed it. Nothing else is gold.**

This product's entire claim is that a number carries K-of-N signatures from named
counterparties. That claim has to be *visible*, not written in a paragraph. So exactly one
colour in the system is reserved for it, and everything else is deliberately quiet so that
gold reads as the loudest thing on any screen.

| | |
|---|---|
| `--gold` `#E6B450` | an attested value (tier 1), and nothing else — ever |
| everything else | quiet greys, one blue for actions |

The discipline is the value. The moment gold appears on a heading, a logo or a decorative
border, the signal is dead and the product's core claim becomes invisible. If a screen has
no attested value on it, it has no gold on it.

Corollary, already enforced in code: a seeded or indicative value is never gold, never
rendered in the official style, and carries a label saying what it is.

## 2. Who we are visually

We are three TradFi businesses in one: a **benchmark administrator** (ICE, CF Benchmarks),
an **index provider** (MSCI, Solactive) and a **fund administrator** (U.S. Bank, State
Street). Those firms' interfaces are authoritative and unbearable — 1990s density with no
craft. Modern fintech (Stripe, Mercury, Linear) has the craft and none of the authority;
it looks like a startup, which is the wrong signal when you are asking someone to settle
against your number.

**We take the density and the seriousness from the first, and the typography, spacing and
motion discipline from the second.** Concretely:

- Information density like a terminal. Tables, not cards-in-a-grid. Numbers aligned.
- Craft like Linear. Real type scale, real spacing scale, 150ms transitions, focus rings.
- No marketing gestures anywhere inside the desk: no gradients on buttons, no glassmorphism,
  no illustrations, no rounded-everything, no emoji, no hero copy.

## 3. Tokens

Already in `frontend/src/styles.css :root` and correct. Do not redefine; extend.

```
--bg        #0A0B0F     page
--surface   #12141A     card
--surface-2 #171A21     raised / hover / active nav
--border    #1E212B     hairline
--border-2  #262A35     emphasis hairline
--text      #E8EAED     primary
--muted     #8A909C     secondary
--faint     #5B616D     tertiary / disabled
--accent    #4C82F7     actions, focus, active
--gold      #E6B450     ATTESTED ONLY
--buy       #3E9B72     confirm / pass / in place
--sell      #C65B5B     refuse / fail / overdue
```

**Type.** `Inter` for UI, `JetBrains Mono` for every number, identifier, party id, contract
id and code. A number in a proportional font is a bug — money and hashes get
`font-variant-numeric: tabular-nums` so columns align and a changing digit does not reflow
the row.

**Scale.** 20 / 16 / 14 / 13 / 12.5 / 11px. Page title 20, section 16, body 14, table 13,
label 11 uppercase with `0.08em` tracking. Nothing else.

**Space.** 4 / 8 / 12 / 16 / 24 / 32. Card padding 16. Gap between cards 14.

**Radius.** 14 cards, 9 controls, 999 pills. **Shadow.** One, the token. No second shadow.

## 4. Layout

Every portal page is the same three things in the same order, and it is the order that
makes the product learnable:

```
1  WHAT YOU MUST DO NOW      the job. First screen, no scrolling.
2  WHAT YOU NEED TO DECIDE   the state that informs it.
3  WHAT IT MEANS             reference. Collapsed by default.
```

This is the rule the signer page broke: ~190 lines of reference sat above the signature.
Reference material is never between a user and their task. It goes below, collapsed, and
it remembers being opened.

Shell: 200px sticky left nav, `minmax(0, 1fr)` main, 1440px max, `clamp(12px, 2.5vw, 28px)`
gutter. Sub-nav under the active section. One `<h1>` per page in `.page-head` with the
action button on the right.

## 5. Components

**Card.** `--surface`, 1px `--border`, radius 14, padding 16. Header row: `<h2>` at 16px,
a one-line `.hint` under it, actions right. Cards do not nest.

**Table** is the default for any set of things. Header `11px` uppercase `--muted`. Numbers
right-aligned, mono, tabular. Row hover `--surface-2`. First column identifies, last column
acts.

**Status tag.** The one thing that must be readable at a glance, so it is a system, not
ad-hoc colours:

| meaning | colour | used for |
|---|---|---|
| attested | gold | tier 1, K-of-N met |
| good | `--buy` | in place, confirmed, settled |
| warning | gold-dim, dashed | seed, indicative, not attested, pilot disclosure |
| bad | `--sell` | overdue, refused, expired, missed |
| neutral | `--muted` | not applicable, not scheduled |

Dashed border is reserved for *provisional* things — a value nobody signed, a sandbox
identity. It reads as "not final" without shouting.

**Empty state** always says what would make it non-empty. Never just "no data".
"Nothing waiting for your signature. Proposals appear here at the strike time and by
webhook." — that is the standard.

**Numbers.** Two decimals for cash, full precision for units, thousands separators, and the
currency as a separate muted span so the digits stay scannable.

## 6. Motion

150ms `ease-out` on colour, background and border. 200ms on height for disclosure. Nothing
else animates. No page transitions, no skeleton shimmer, no spinners longer than 400ms —
a table that is reloading keeps its rows and dims them rather than collapsing, because a
list that disappears while you read it is the single most irritating thing an interface can
do.

## 7. Per-role information architecture

The roles see genuinely different products, and that is correct. What each one needs:

**Signer** (the seat at a counterparty). Open proposals first. For each: the value proposed,
the conditions their seat verifies, the evidence fields, confirm and refuse. Everything else
— what the seat asserts, the failure policy, the automation config, the trust ladder —
below, collapsed.

**Authorised participant.** Funds they can deal, the NAV, whether it is attested, and
whether dealing is open. Then the state of the assets underneath: last value, who signed it,
how old, next strike. They must never see the operator desk.

**Fund administrator.** The fund as the administrator: NAV series, shares outstanding,
fees, basket, licensees, create/redeem log. Read-only.

**Auditor.** Events and series, exportable. Nothing else.

**Admin.** Everything, plus `View as`. This is the only role that gets the operator desk,
and the operator desk is allowed to be dense because it is one person's cockpit.

## 8. Rules

1. Gold is attestation. Never decoration.
2. Never show a number without saying what kind of number it is.
3. The job goes above the manual.
4. A table beats a grid of cards.
5. Mono for anything a machine produced.
6. Empty states name the thing that would fill them.
7. No gradient, no glass, no illustration, no emoji inside the desk.
8. A reloading list keeps its rows.
