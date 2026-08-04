// accrual.ts — THE LEDGER'S ARITHMETIC, IN THE BROWSER, EXACTLY.
// ============================================================================
// WHY THIS FILE IS FIXED-POINT AND NOT `number`.
//
// The demo is a NAV that ticks up on screen, and the entire claim behind it is that
// the moving figure is what the LEDGER says the fund is worth — derived on-ledger
// from four attested inputs, not a plausible animation. A screen that disagrees with
// the ledger is worse than no screen at all, so this module reproduces
// `Governance.daml`'s `navAt` to the digit rather than approximately.
//
// A JavaScript `number` is a float64. Daml `Decimal` is `Numeric 10` — fixed-point,
// exactly ten decimal places, with multiplication and division rounding HALF-EVEN at
// that scale. Those are different type systems, and float64 cannot represent 0.036 or
// 100.0004166667 exactly. So every value here is a `bigint` holding the value scaled
// by 1e10 — the ledger's own representation — and every operation rounds where
// Daml-LF rounds. Nothing is ever routed through a float.
//
// TWO THINGS MUST MATCH, AND BOTH ARE DELIBERATE TRANSCRIPTIONS:
//
//   1. THE ORDER OF OPERATIONS. Governance.daml computes
//        base * (rate * elapsedMicros) / yearMicros
//      — multiply first, divide LAST. The obvious alternative (work out a rate per
//      second, then multiply) divides first: at 4%/yr ACT/360 that per-second rate is
//      1.28600823e-9, which at 10dp becomes 0.0000000013 and bakes a 1.09% RELATIVE
//      error into every tick forever. At 3.6% it underflows to zero entirely and the
//      fund earns nothing. The Daml is explicit about this; so is this file.
//
//   2. THE ROUNDING MODE. Daml-LF's MUL_NUMERIC / DIV_NUMERIC round half-even
//      (banker's rounding) at scale 10. `mulD`/`divD` below implement half-even on
//      integers rather than reaching for `Math.round`, which is half-up and would
//      disagree on ties.
//
// PATH INDEPENDENCE IS WHY THE TICKER CANNOT DRIFT. `navAt` is a pure function of the
// attested base and the elapsed time since the attested instant — never of a
// previously displayed value. The ticker recomputes from the base on every frame, so
// after ten minutes of animation the number on screen is bit-identical to a single
// evaluation at that instant. There is no accumulator to accumulate error in.
//
// VERIFIED AGAINST THE LEDGER'S OWN VECTORS. `ACCRUAL_VECTORS` below are lifted from
// `daml/Test.daml` (green, 53 scripts) and re-asserted by `selfCheck()`, which the
// panel runs on mount and reports on screen. The Java mirror asserts the same list in
// `AccrualTest`. Ledger → backend → browser is one chain of pinned numbers.
// ============================================================================

/** Daml `Decimal` is `Numeric 10`. */
export const SCALE = 10;

/** 1e10 as a bigint — the scaling factor every value here is expressed in. */
export const ONE: bigint = 10n ** BigInt(SCALE);

/** `Governance.microsPerDay`. Daml `Time` is microsecond-resolution. */
export const MICROS_PER_DAY = 86_400_000_000n;

/** `Governance.anchorAccrualToleranceBps` — the auction anchor's staleness budget. */
export const ANCHOR_TOLERANCE_BPS = 1n;

/** `Governance.anchorRoundingSlack` — ten units in the last place. */
export const ANCHOR_ROUNDING_SLACK = 10n;

/** The conventions this venue accrues on. 30/360 and ACT/ACT are refused, not defaulted. */
export const ACCRUING_DAY_COUNTS = ['ACT/360', 'ACT/365F'] as const;
export type DayCount = (typeof ACCRUING_DAY_COUNTS)[number] | 'NONE';

// ---------------------------------------------------------------------------
// Numeric 10 primitives
// ---------------------------------------------------------------------------

/**
 * Parse a decimal STRING into the ledger's fixed-point representation, exactly.
 *
 * The backend sends every Decimal as a plain string for precisely this reason: a
 * string survives the trip, a JSON number becomes a float and stops being the
 * ledger's value. Digits beyond the 10th are rounded half-even, which is what the
 * ledger would have done to them anyway.
 */
export function parseDec(input: string | number): bigint {
  const s = typeof input === 'number' ? String(input) : input.trim();
  if (s === '') return 0n;
  const m = /^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$/.exec(s);
  if (!m) throw new Error(`not a decimal: ${input}`);
  const sign = m[1] === '-' ? -1n : 1n;
  const intPart = m[2] || '0';
  const fracPart = m[3] || '';
  const exp = m[4] ? Number(m[4]) : 0;

  // Shift the decimal point by the exponent, then by SCALE, then round half-even.
  const digits = intPart + fracPart;
  // Position of the point measured from the RIGHT of `digits`, after the exponent.
  const pointFromRight = fracPart.length - exp;
  let scaled: bigint;
  if (pointFromRight <= SCALE) {
    scaled = BigInt(digits || '0') * 10n ** BigInt(SCALE - pointFromRight);
  } else {
    // More precision than the ledger carries: round half-even at 10dp.
    scaled = roundDivHalfEven(BigInt(digits || '0'), 10n ** BigInt(pointFromRight - SCALE));
  }
  return sign * scaled;
}

/** Round `n / d` (d > 0) to the nearest integer, ties to even — Daml-LF's mode. */
function roundDivHalfEven(n: bigint, d: bigint): bigint {
  const neg = n < 0n;
  const a = neg ? -n : n;
  const q = a / d;
  const r = a - q * d;
  const twice = r * 2n;
  let out = q;
  if (twice > d) out = q + 1n;
  else if (twice === d && q % 2n === 1n) out = q + 1n;   // tie -> even
  return neg ? -out : out;
}

/** `MUL_NUMERIC`: multiply two scaled values and round back to 10dp half-even. */
export function mulD(a: bigint, b: bigint): bigint {
  return roundDivHalfEven(a * b, ONE);
}

/** `DIV_NUMERIC`: divide, landing directly on 10dp half-even (never a two-step round). */
export function divD(a: bigint, b: bigint): bigint {
  if (b === 0n) throw new Error('division by zero');
  const neg = b < 0n;
  return roundDivHalfEven(neg ? -(a * ONE) : a * ONE, neg ? -b : b);
}

/** Multiply a scaled Decimal by a WHOLE number (e.g. a microsecond count). Exact. */
export function mulInt(a: bigint, k: bigint): bigint {
  return a * k;
}

/** Render a scaled value as a plain decimal string with `dp` places (truncating, not rounding). */
export function formatD(v: bigint, dp = SCALE): string {
  const neg = v < 0n;
  const a = neg ? -v : v;
  const whole = a / ONE;
  const frac = (a % ONE).toString().padStart(SCALE, '0');
  const shown = dp <= 0 ? '' : `.${frac.slice(0, dp)}`;
  return `${neg ? '-' : ''}${whole.toString()}${shown}`;
}

/** Group the integer part with thin separators for a headline figure. */
export function formatGrouped(v: bigint, dp = SCALE): string {
  const plain = formatD(v, dp);
  const [w, f] = plain.split('.');
  const sign = w.startsWith('-') ? '-' : '';
  const digits = sign ? w.slice(1) : w;
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return f === undefined ? `${sign}${grouped}` : `${sign}${grouped}.${f}`;
}

/** Only for display of ratios/percentages — never for money. */
export function toNumber(v: bigint): number {
  return Number(v) / Number(ONE);
}

// ---------------------------------------------------------------------------
// The accrual, transcribed from Governance.daml
// ---------------------------------------------------------------------------

/** `Governance.dayCountYearMicros` — the convention's YEAR length in microseconds. */
export function dayCountYearMicros(dayCount: string): bigint | null {
  switch (dayCount) {
    case 'ACT/360':
      return 360n * MICROS_PER_DAY; // USD money market: SOFR, repo, T-bills
    case 'ACT/365F':
      return 365n * MICROS_PER_DAY; // GBP/AUD/NZD/HKD/SGD money market
    case 'NONE':
      return 360n * MICROS_PER_DAY; // non-accruing snapshot (the rate is 0)
    default:
      return null;                  // 30/360, ACT/ACT: refused, never defaulted
  }
}

export function supportedDayCount(dayCount: string): boolean {
  return dayCountYearMicros(dayCount) !== null;
}

/**
 * `Governance.elapsedMicrosFrom` — CLAMPED AT ZERO.
 *
 * Asking what a fixing says at an instant before it applies from is a legal question
 * (a backfill, a replay, a report window that opens early). The honest answer is
 * "nothing had accrued by then", not a negative accrual that would mark the fund
 * below a value the committee actually attested.
 */
export function elapsedMicrosFrom(fromMicros: bigint, atMicros: bigint): bigint {
  const d = atMicros - fromMicros;
  return d <= 0n ? 0n : d;
}

/**
 * `Governance.accruedAmount` — `base * (rate * elapsed) / yearMicros`.
 *
 * The parenthesisation is load-bearing. `rate * elapsedMicros` is exact (a 10dp value
 * times a whole number of microseconds still has at most 10dp); multiplying by the
 * base is the only intermediate rounding; the division by ~3.11e13 shrinks that
 * inherited error by thirteen orders of magnitude before rounding once, at the end.
 */
export function accruedAmount(
  basePrice: bigint,
  ratePerAnnum: bigint,
  dayCount: string,
  elapsedMicros: bigint,
): bigint {
  const yearMicros = dayCountYearMicros(dayCount);
  // An unrecognised convention accrues NOTHING rather than guessing a year length.
  if (yearMicros === null || elapsedMicros <= 0n) return 0n;
  const rateTimesMicros = mulInt(ratePerAnnum, elapsedMicros); // exact
  const numerator = mulD(basePrice, rateTimesMicros);
  return divD(numerator, yearMicros * ONE);
}

/** The four attested inputs — everything a value at any instant is derived from. */
export interface AccrualRecipe {
  basePrice: bigint;                 // what one share was worth AT accrualFrom
  ratePerAnnum: bigint;              // 0 = a pure snapshot; may be NEGATIVE
  dayCount: string;                  // ACT/360 | ACT/365F | NONE
  accrualFromEpochMicros: bigint;    // the instant the mark applies from
}

/**
 * `Governance.navAt` — what ONE SHARE is worth at an instant.
 *
 * Floored at zero for the same real reason the ledger floors it: EUR/CHF/JPY money
 * markets printed negative rates from 2015 to 2022, so a downward accrual exists, and
 * a NAV driven through zero must report zero rather than a negative "price".
 */
export function navAt(r: AccrualRecipe, atEpochMicros: bigint): bigint {
  const accrued = accruedAmount(
    r.basePrice,
    r.ratePerAnnum,
    r.dayCount,
    elapsedMicrosFrom(r.accrualFromEpochMicros, atEpochMicros),
  );
  const nav = r.basePrice + accrued;   // addition is EXACT in Numeric 10
  return nav < 0n ? 0n : nav;
}

/** The 1bp staleness budget, in price units, for a given accrued NAV. */
export function staleBudget(accruedNav: bigint): bigint {
  return divD(mulD(accruedNav, ANCHOR_TOLERANCE_BPS * ONE), 10000n * ONE);
}

/**
 * `Governance.anchorConsistentWithNav` — would `RunClose` accept this anchor?
 *
 * ASYMMETRIC, deliberately. BELOW the accrual is staleness, forgivable up to one basis
 * point (at 3.6%/yr ACT/360 a fund accrues exactly 1bp per day, so the band means "the
 * anchor may be up to a day old"). ABOVE it is not staleness at all — it is a venue
 * pricing value the fund has not earned — so that side is a hard edge.
 */
export function anchorConsistentWithNav(accruedNav: bigint, anchor: bigint): boolean {
  const budget = staleBudget(accruedNav);
  return anchor <= accruedNav + ANCHOR_ROUNDING_SLACK && accruedNav - anchor <= budget;
}

// ---------------------------------------------------------------------------
// Clock
// ---------------------------------------------------------------------------

/**
 * Epoch microseconds from the browser clock, corrected by the desk's own clock.
 *
 * The value on screen is a function of TIME, so whose time matters. Every accrual
 * response carries `asOfEpochMicros` — the instant the desk derived its number at —
 * and the offset between that and the browser's clock at the moment of the reply is
 * carried forward, so the ticker advances on the desk's clock (which is the ledger's,
 * to within one HTTP round trip) rather than on a laptop that is four seconds fast.
 */
export function nowEpochMicros(offsetMicros: bigint = 0n): bigint {
  return BigInt(Date.now()) * 1000n + offsetMicros;
}

/** ISO-8601 -> epoch microseconds, keeping sub-millisecond digits the ledger may carry. */
export function isoToEpochMicros(iso: string): bigint {
  const ms = Date.parse(iso);
  if (Number.isNaN(ms)) throw new Error(`not an ISO-8601 instant: ${iso}`);
  const frac = /\.(\d+)/.exec(iso);
  if (!frac) return BigInt(ms) * 1000n;
  const micros = frac[1].padEnd(6, '0').slice(0, 6);
  // Date.parse already consumed the first three fractional digits as milliseconds.
  return BigInt(Math.floor(ms / 1000)) * 1_000_000n + BigInt(micros);
}

// ---------------------------------------------------------------------------
// The agreement check — the ledger's own vectors, re-asserted in the browser
// ---------------------------------------------------------------------------

/**
 * Vectors lifted verbatim from `daml/Test.daml` (`testAccrualArithmeticUnit`,
 * `testCommitteeAttestedClose`, `testAccrualBackwardsTimeIsSafe`), which pass on the
 * ledger. If this file's arithmetic ever stops being the ledger's arithmetic, these
 * stop matching — and the panel says so on screen rather than quietly showing a wrong
 * number to a judge.
 */
export const ACCRUAL_VECTORS: {
  what: string;
  base: string;
  rate: string;
  dayCount: string;
  elapsedMicros: bigint;
  expected: string;
}[] = [
  { what: '3.6% ACT/360, 1 day = exactly 1bp',
    base: '100.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: MICROS_PER_DAY, expected: '100.01' },
  { what: '3.6% ACT/360, 10 days',
    base: '100.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: 10n * MICROS_PER_DAY, expected: '100.10' },
  { what: '3.6% ACT/360, 12 hours = half a bp',
    base: '100.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: MICROS_PER_DAY / 2n, expected: '100.005' },
  { what: '3.6% ACT/360, 1 hour (rounded at 10dp)',
    base: '100.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: MICROS_PER_DAY / 24n, expected: '100.0004166667' },
  { what: '3.6% ACT/365F, 1 day — the 1.389% convention gap',
    base: '100.0', rate: '0.036', dayCount: 'ACT/365F',
    elapsedMicros: MICROS_PER_DAY, expected: '100.0098630137' },
  { what: 'snapshot (rate 0, NONE) accrues nothing in ten years',
    base: '100.0', rate: '0.0', dayCount: 'NONE',
    elapsedMicros: 3650n * MICROS_PER_DAY, expected: '100.0' },
  { what: 'the auction vector: 2,400 + 10 x 0.24',
    base: '2400.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: 10n * MICROS_PER_DAY, expected: '2402.40' },
  { what: 'before the strike clamps to the base, never marks down',
    base: '100.0', rate: '0.036', dayCount: 'ACT/360',
    elapsedMicros: -5n * MICROS_PER_DAY, expected: '100.0' },
  { what: 'an unsupported convention accrues nothing rather than guessing',
    base: '100.0', rate: '0.036', dayCount: 'ACT/ACT',
    elapsedMicros: MICROS_PER_DAY, expected: '100.0' },
];

export interface SelfCheckResult {
  ok: boolean;
  passed: number;
  total: number;
  failures: string[];
}

/** Run every ledger vector through this module. Cheap, synchronous, no network. */
export function selfCheck(): SelfCheckResult {
  const failures: string[] = [];
  for (const v of ACCRUAL_VECTORS) {
    const recipe: AccrualRecipe = {
      basePrice: parseDec(v.base),
      ratePerAnnum: parseDec(v.rate),
      dayCount: v.dayCount,
      accrualFromEpochMicros: 0n,
    };
    const got = navAt(recipe, v.elapsedMicros);
    const want = parseDec(v.expected);
    if (got !== want) {
      failures.push(`${v.what}: got ${formatD(got)} want ${formatD(want)}`);
    }
  }
  // The anchor band, from testCommitteeAttestedClose.
  const navOneDay = parseDec('2400.24');
  const navTwoDays = parseDec('2400.48');
  const anchor = parseDec('2400.0');
  if (!anchorConsistentWithNav(navOneDay, anchor)) {
    failures.push('anchor band: one day behind should be inside the 1bp budget');
  }
  if (anchorConsistentWithNav(navTwoDays, anchor)) {
    failures.push('anchor band: two days behind should be outside the 1bp budget');
  }
  if (anchorConsistentWithNav(anchor, parseDec('2400.01'))) {
    failures.push('anchor band: an anchor AHEAD of the accrual must be rejected');
  }
  const total = ACCRUAL_VECTORS.length + 3;
  return { ok: failures.length === 0, passed: total - failures.length, total, failures };
}
