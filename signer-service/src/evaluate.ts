/**
 * The seat rules - docs/SIGNER_PROTOCOL.md §2 as code.
 *
 * Every rule takes the evidence fields its sources produced and the proposal, and returns
 * pass/fail with the numbers that decided it. A rule never widens a tolerance and never
 * passes on missing evidence: a field that did not resolve is a HALT (escalate, retry),
 * not a refusal - a refusal is a statement about the numbers, and there are none.
 */
import type { Seat } from './config';

export interface EvalContext {
  seat: Seat;
  instrument: string;
  price: number;
  cid: string;
  now: Date;
  tolerances: Record<string, number>;
}

export interface RuleResult {
  pass: boolean;
  /** Every number the decision was made from - for the log. */
  values: Record<string, unknown>;
  /** What is sent to CrossDesk with a confirm - the numeric evidence for this condition. */
  evidence: Record<string, unknown>;
  /** A one-line, number-bearing reason. Used as the refusal reason when pass is false. */
  reason: string;
}

export type Rule = (fields: Record<string, unknown>, ctx: EvalContext) => RuleResult;

export interface RuleSpec {
  /** Evidence fields the rule needs. Alternatives are listed as one array. */
  needs: string[][];
  rule: Rule;
}

export class MissingEvidence extends Error {
  constructor(public readonly condition: string, public readonly field: string, detail: string) {
    super(`${condition}: ${detail}`);
  }
}

function asNumber(condition: string, field: string, v: unknown): number {
  if (v === undefined || v === null || v === '') throw new MissingEvidence(condition, field, `no value for '${field}'`);
  const n = typeof v === 'number' ? v : Number(String(v).trim());
  if (!Number.isFinite(n)) throw new MissingEvidence(condition, field, `'${field}' is not a number: ${JSON.stringify(v)}`);
  return n;
}

function asDate(condition: string, field: string, v: unknown): Date {
  if (v === undefined || v === null || v === '') throw new MissingEvidence(condition, field, `no value for '${field}'`);
  const d = typeof v === 'number' ? new Date(v < 1e12 ? v * 1000 : v) : new Date(String(v));
  if (Number.isNaN(d.getTime())) throw new MissingEvidence(condition, field, `'${field}' is not a timestamp: ${JSON.stringify(v)}`);
  return d;
}

function bps(a: number, b: number): number {
  return Math.abs(a - b) / b * 10_000;
}

const round = (n: number, dp = 4): number => Number(n.toFixed(dp));

/**
 * The ledger stores evidence as Daml Numeric 10 and refuses a number it cannot represent
 * exactly - and IEEE doubles produce 77385.70654600002 from 77292.955 * 1.0012. Every number
 * that leaves this service is therefore rounded to 10 decimals first.
 */
export function ledgerNumber(n: number): number {
  return Number(n.toFixed(10));
}

export function tidy<T extends Record<string, unknown>>(o: T): T {
  const out: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(o)) out[k] = typeof v === 'number' && Number.isFinite(v) ? ledgerNumber(v) : v;
  return out as T;
}

/** A list of prints: numbers, or objects with price / px / p, or {low, high}. */
function rangeOf(condition: string, prints: unknown): { low: number; high: number; count: number } {
  if (prints !== null && typeof prints === 'object' && !Array.isArray(prints)) {
    const o = prints as Record<string, unknown>;
    if ('low' in o && 'high' in o) {
      return { low: ledgerNumber(asNumber(condition, 'low', o.low)), high: ledgerNumber(asNumber(condition, 'high', o.high)), count: Number(o.count ?? 2) };
    }
  }
  if (!Array.isArray(prints)) throw new MissingEvidence(condition, 'prints', `'prints' must be a list of prints or {low, high}`);
  if (prints.length === 0) throw new MissingEvidence(condition, 'prints', 'no prints in the window');
  let low = Infinity;
  let high = -Infinity;
  for (const p of prints) {
    const v = p !== null && typeof p === 'object'
      ? (p as Record<string, unknown>).price ?? (p as Record<string, unknown>).px ?? (p as Record<string, unknown>).p
      : p;
    const n = asNumber(condition, 'prints[]', v);
    if (n < low) low = n;
    if (n > high) high = n;
  }
  return { low: ledgerNumber(low), high: ledgerNumber(high), count: prints.length };
}

export const RULES: Record<Seat, Record<string, RuleSpec>> = {
  issuer: {
    'attestor-quorum': {
      needs: [['quorumSigners', 'quorumThreshold']],
      rule: (f) => {
        const c = 'attestor-quorum';
        const quorumSigners = asNumber(c, 'quorumSigners', f.quorumSigners);
        const quorumThreshold = asNumber(c, 'quorumThreshold', f.quorumThreshold);
        const pass = quorumSigners >= quorumThreshold && quorumThreshold > 0;
        return {
          pass,
          values: { quorumSigners, quorumThreshold },
          evidence: { quorumSigners, quorumThreshold },
          reason: `attestor quorum ${quorumSigners} of ${quorumThreshold} required${pass ? '' : ' - below threshold'}`,
        };
      },
    },
    'reserves-current': {
      needs: [['reservesAsOf']],
      rule: (f, ctx) => {
        const c = 'reserves-current';
        const asOf = asDate(c, 'reservesAsOf', f.reservesAsOf);
        const ageHours = (ctx.now.getTime() - asOf.getTime()) / 3_600_000;
        const maxAge = ctx.tolerances.reservesMaxAgeHours;
        const pass = ageHours >= -0.25 && ageHours <= maxAge;
        return {
          pass,
          values: { reservesAsOf: asOf.toISOString(), ageHours: round(ageHours, 2), maxAgeHours: maxAge },
          evidence: { reservesAsOf: asOf.toISOString() },
          reason: `proof of reserve as of ${asOf.toISOString()} is ${round(ageHours, 1)}h old (max ${maxAge}h)`,
        };
      },
    },
    'reserves-cover-supply': {
      needs: [['reserves', 'supply']],
      rule: (f) => {
        const c = 'reserves-cover-supply';
        const reserves = asNumber(c, 'reserves', f.reserves);
        const supply = asNumber(c, 'supply', f.supply);
        const pass = reserves >= supply && supply >= 0;
        const coverage = supply > 0 ? round(reserves / supply, 6) : null;
        return {
          pass,
          values: { reserves, supply, coverage },
          evidence: { reserves, supply },
          reason: `attested reserves ${reserves} vs circulating supply ${supply}${coverage !== null ? ` (coverage ${coverage}x)` : ''}`,
        };
      },
    },
    'redemption-queue-clear': {
      needs: [['queueDepth']],
      rule: (f, ctx) => {
        const c = 'redemption-queue-clear';
        const queueDepth = asNumber(c, 'queueDepth', f.queueDepth);
        const maxQueueDepth = f.maxQueueDepth === undefined ? ctx.tolerances.maxQueueDepth : asNumber(c, 'maxQueueDepth', f.maxQueueDepth);
        const pass = queueDepth <= maxQueueDepth;
        return {
          pass,
          values: { queueDepth, maxQueueDepth },
          evidence: { queueDepth, maxQueueDepth },
          reason: `${queueDepth} redemption(s) unfilled beyond window (max ${maxQueueDepth})`,
        };
      },
    },
  },

  lender: {
    'independent-mark-within-tolerance': {
      needs: [['independentMark']],
      rule: (f, ctx) => {
        const c = 'independent-mark-within-tolerance';
        const independentMark = asNumber(c, 'independentMark', f.independentMark);
        if (independentMark <= 0) throw new MissingEvidence(c, 'independentMark', 'independent mark must be positive');
        const deviationBps = round(bps(ctx.price, independentMark), 2);
        const tol = ctx.tolerances.markToleranceBps;
        const pass = deviationBps <= tol;
        return {
          pass,
          values: { proposed: ctx.price, independentMark, deviationBps, toleranceBps: tol },
          evidence: { independentMark, deviationBps },
          reason: `proposed ${ctx.price} vs our mark ${independentMark}: ${deviationBps}bp (tolerance ${tol}bp)`,
        };
      },
    },
    'liquidations-consistent': {
      needs: [['liquidationsToday', 'worstDeviationBps']],
      rule: (f, ctx) => {
        const c = 'liquidations-consistent';
        const liquidationsToday = asNumber(c, 'liquidationsToday', f.liquidationsToday);
        const worstDeviationBps = liquidationsToday === 0 && (f.worstDeviationBps === undefined || f.worstDeviationBps === null)
          ? 0
          : asNumber(c, 'worstDeviationBps', f.worstDeviationBps);
        const tol = ctx.tolerances.liquidationToleranceBps;
        const pass = liquidationsToday === 0 || Math.abs(worstDeviationBps) <= tol;
        return {
          pass,
          values: { liquidationsToday, worstDeviationBps, toleranceBps: tol },
          evidence: { liquidationsToday, worstDeviationBps },
          reason: liquidationsToday === 0
            ? 'no liquidations in the session'
            : `${liquidationsToday} liquidation(s) in session, worst cleared ${worstDeviationBps}bp from the mark (tolerance ${tol}bp)`,
        };
      },
    },
    'book-acceptance': {
      needs: [['acceptedAt']],
      rule: (f, ctx) => {
        const c = 'book-acceptance';
        if (f.acceptedAt === false || f.acceptedAt === null || f.acceptedAt === undefined) {
          return {
            pass: false,
            values: { acceptedAt: null },
            evidence: {},
            reason: `book did not accept ${ctx.price} for ${ctx.instrument}`,
          };
        }
        const acceptedAt = asDate(c, 'acceptedAt', f.acceptedAt);
        const ageMin = (ctx.now.getTime() - acceptedAt.getTime()) / 60_000;
        const maxAge = ctx.tolerances.bookAcceptanceMaxAgeMinutes;
        const pass = ageMin >= -5 && ageMin <= maxAge;
        return {
          pass,
          values: { acceptedAt: acceptedAt.toISOString(), ageMinutes: round(ageMin, 1), maxAgeMinutes: maxAge, price: ctx.price },
          evidence: { acceptedAt: acceptedAt.toISOString() },
          reason: pass
            ? `book accepted ${ctx.price} at ${acceptedAt.toISOString()}`
            : `book acceptance at ${acceptedAt.toISOString()} is ${round(ageMin, 0)} min old (max ${maxAge})`,
        };
      },
    },
  },

  venue: {
    'traded-range': {
      needs: [['prints'], ['low', 'high']],
      rule: (f, ctx) => {
        const c = 'traded-range';
        const r = f.prints !== undefined ? rangeOf(c, f.prints) : rangeOf(c, { low: f.low, high: f.high, count: f.count });
        if (r.low > r.high) throw new MissingEvidence(c, 'low', `inverted range ${r.low} > ${r.high}`);
        const pass = r.low <= ctx.price && ctx.price <= r.high;
        return {
          pass,
          values: { low: r.low, high: r.high, prints: r.count, proposed: ctx.price },
          evidence: { low: r.low, high: r.high },
          reason: `proposed ${ctx.price} ${pass ? 'inside' : 'outside'} traded range ${r.low}-${r.high} (${r.count} print(s))`,
        };
      },
    },
    'spread-within-tolerance': {
      needs: [['spreadBps'], ['bid', 'ask']],
      rule: (f, ctx) => {
        const c = 'spread-within-tolerance';
        let spreadBps: number;
        let extra: Record<string, unknown> = {};
        if (f.spreadBps !== undefined) {
          spreadBps = asNumber(c, 'spreadBps', f.spreadBps);
        } else {
          const bid = asNumber(c, 'bid', f.bid);
          const ask = asNumber(c, 'ask', f.ask);
          if (ask < bid) throw new MissingEvidence(c, 'ask', `crossed book: bid ${bid} > ask ${ask}`);
          spreadBps = round((ask - bid) / ((ask + bid) / 2) * 10_000, 2);
          extra = { bid, ask };
        }
        const tol = ctx.tolerances.maxSpreadBps;
        const pass = spreadBps <= tol;
        return {
          pass,
          values: { ...extra, spreadBps, toleranceBps: tol },
          evidence: { ...extra, spreadBps },
          reason: `spread at strike ${spreadBps}bp (tolerance ${tol}bp)`,
        };
      },
    },
    'sufficient-volume': {
      needs: [['volume']],
      rule: (f, ctx) => {
        const c = 'sufficient-volume';
        const volume = asNumber(c, 'volume', f.volume);
        const min = ctx.tolerances.minVolume;
        const pass = volume >= min && volume > 0;
        return {
          pass,
          values: { volume, minVolume: min },
          evidence: { volume },
          reason: `traded volume ${volume} in window (minimum ${min})`,
        };
      },
    },
  },
};

export function ruleFor(seat: Seat, condition: string): RuleSpec | undefined {
  return RULES[seat]?.[condition];
}

/** Every field name a seat's rules can accept - used to validate the config up front. */
export function fieldsFor(seat: Seat, condition: string): string[] {
  const spec = RULES[seat]?.[condition];
  if (!spec) return [];
  return Array.from(new Set(spec.needs.flat()));
}

/** Which alternative of `needs` the configured fields satisfy, or null. */
export function satisfiedAlternative(spec: RuleSpec, configured: string[]): string[] | null {
  for (const alt of spec.needs) {
    if (alt.every((f) => configured.includes(f))) return alt;
  }
  return null;
}
