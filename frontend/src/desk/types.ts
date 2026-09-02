// The product API contract — docs/PRODUCT-PLAN.md §5, as TypeScript.
//
// Where §5 gives a shape it is copied verbatim. Where §5 names a route but not its
// body ("proposals for my instruments with my conditions and what I already did"), the
// shape below is the frontend's ASSUMPTION and is listed in the build report so the
// backend can confirm or correct it. Every field a page merely displays is optional, so
// a backend that sends less still renders.

export type Role = 'admin' | 'signer' | 'ap' | 'fund_admin' | 'auditor' | 'viewer';
export type Seat = 'issuer' | 'lender' | 'venue';

export interface Me {
  uid: string;
  email: string;
  role: Role;
  party: string;
  seat?: Seat;
  instruments?: string[];
  org: string;
  displayName: string;
  /**
   * Echoed by the backend when it honoured `X-Act-As` (admin "View as"). The live backend
   * sends `{ by: <admin email> }` — the identity fields above are then the acted-as user's.
   * Older shapes sent the acted-as email as a string; both are accepted.
   */
  actingAs?: string | { by?: string } | null;
}

// ---- public -----------------------------------------------------------------

export interface BenchmarkLast {
  price: number | null;
  asOf: string;
  tier: number;
  /** The backend's word for the tier ("seed", "committee", "missed"), when it sends one. */
  tierLabel?: string;
  k: number;
  n: number;
  signers: string[];
  ageSeconds: number;
}

export interface Benchmark {
  id: string;
  name: string;
  kind: string;              // "wrapped" | "nav" | "snapshot"
  publishTime: string;       // "16:00"
  timezone: string;          // "Europe/London"
  description: string;
  last: BenchmarkLast | null;
  referencing: { id: string; name: string }[];
}

export interface SeriesRow {
  date: string;
  asOf: string;
  /** Absent on a tier-5 miss: the gap is published as a gap (§4). */
  price?: number | null;
  referencePrice?: number;
  wrapperFactor?: number;
  tier: number;
  tierLabel?: string;
  note?: string;
  k: number;
  n: number;
  signers: string[];
  fixingCid: string;
  restated: boolean;
}

export interface Methodology {
  version: string;
  url: string;
  signerProtocolVersion: string;
}

/** Existing `/api/fixing-schedule` (backend Dtos.FixingScheduleResponse). */
export interface ScheduleStatus {
  instrumentId: string;
  session: string;
  strikeAt: string;
  zone: string;
  graceMinutes: number;
  state: string;            // "ON_TIME" | "DUE" | "OVERDUE" | "STRUCK" …
  expectedAt: string;
  minutesLate: number;
  note: string;
}
export interface FixingSchedule {
  identifiers: ScheduleStatus[];
  overdueCount: number;
  asOf: string;
}

// ---- signer -----------------------------------------------------------------

export type ProposalStatus = 'open' | 'finalized' | 'refused' | 'restruck' | 'missed';

/** ASSUMED shape of GET /api/proposals (§5 names the route, not the body). */
export interface Proposal {
  cid: string;
  instrument: string;
  session?: string;
  kind: 'wrapped' | 'nav' | 'snapshot';
  price: number;
  referencePrice?: number;   // wrapped: the benchmark print
  wrapperFactor?: number;    // wrapped: the last factor
  navComponents?: { instrumentId: string; unitsPerShare: number; mark: number }[];
  proposedBy?: string;
  proposedAt: string;
  deadline: string;          // ISO instant — the restrike window end
  k: number;
  n: number;
  confirmed: string[];       // seats/parties that have already attested
  status: ProposalStatus;
  /** What THIS caller's seat must verify (names from /api/signer-protocol). */
  conditions: string[];
  requiresObservedRange: boolean;
  /** The backend's own account of how the price was built (inputs published, §8). */
  rationale?: string;
  /** Every refusal so far, from any seat — a signer should see what the others saw. */
  refusals?: { actor?: string; seat?: string; condition?: string; reason?: string; ts?: string }[] | null;
  attestations?: { member?: string; seat?: string; checks?: string[]; observedLow?: number; observedHigh?: number; ts?: string }[] | null;
  /** What this caller already did on this proposal, if anything. */
  mine?: {
    action: 'confirmed' | 'refused';
    at: string;
    checks?: string[];
    evidence?: { low: number; high: number };
    reason?: string;
    cid?: string;            // the on-ledger contract id after the action
  } | null;
}

export interface ProposalEvent {
  id?: string;
  ts: string;
  kind: string;              // "proposed" | "confirmed" | "refused" | "restruck" | "finalized" | "missed" | "webhook.sent" …
  actor?: string;
  seat?: string;
  reason?: string;
  cid?: string;
  price?: number;
  detail?: string;
}

export interface ConfirmBody { checks: string[]; evidence?: { low: number; high: number } }
export interface RefuseBody { condition: string; reason: string }

export interface SignerSettings {
  webhookUrl: string;
  webhookSecret?: string | null;
  /** The backend never returns the secret; it says whether one is stored. */
  webhookSecretSet?: boolean;
  email: string;
  tolerances: Record<string, number>;
  /** ASSUMED: whether an API key exists, never the key itself. */
  apiKey?: { createdAt: string; prefix: string } | null;
}

// ---- AP ---------------------------------------------------------------------

export interface FundComponent { instrumentId: string; unitsPerShare: number; mark?: number | null }

/** ASSUMED shape of one row of GET /api/ap/funds. */
export interface ApFund {
  id: string;
  name: string;
  cash: string;
  official: { nav: number; asOf: string; tier: number; tierLabel?: string; k: number; n: number; fixingCid?: string | null } | null;
  indicative?: number | null;
  components: FundComponent[];
  sharesOutstanding?: number;
  fee: { createBps: number; redeemBps: number; minimum?: number; currency?: string };
  cutoff: { time: string; timezone: string; nextAt?: string };
  minShares?: number;
  lot?: number;
}

export interface Receipt {
  id: string;
  fundId: string;
  kind: 'create' | 'redeem';
  shares: number;
  nav: number;
  units: { instrumentId: string; amount: number }[];
  fee: number;
  feeCurrency?: string;
  /** Tier of the NAV this settled at, when the backend records it; gold only for tier 1. */
  navTier?: number;
  ts: string;
  cid?: string;
  status: 'settled' | 'pending' | 'failed';
  party?: string;
  note?: string;
}

// ---- fund admin -------------------------------------------------------------

export interface FundDashboard {
  id: string;
  name: string;
  cash: string;
  series: SeriesRow[];
  sharesOutstanding: number;
  components?: FundComponent[];
  log: Receipt[];
  fees: { accrued: number; currency: string; rows: { date: string; kind: string; amount: number; ref?: string }[] };
  licensees?: { name: string; kind: string }[];
}

// ---- admin ------------------------------------------------------------------

export interface ScheduleRow {
  instrument: string;
  session?: string;
  strikeAt: string;           // "16:00"
  timezone: string;
  windowMinutes: number;
  enabled: boolean;
  tiers: { t2: boolean; t3: boolean; t4: boolean };
}

export interface CommitteeSeat {
  seat: string;
  party: string;
  users: string[];
  lastAction?: { kind: string; at: string } | null;
}
export interface Committee { instrument: string; k: number; n: number; seats: CommitteeSeat[] }

export interface UserRow extends Me { enabled?: boolean }
export type UserInput = Omit<UserRow, 'uid'>;

export interface FixingEvent {
  id?: string;
  ts: string;
  instrument: string;
  proposalCid?: string;
  kind: string;
  actor?: string;
  reason?: string;
  cid?: string;
  price?: number;
  tier?: number;
}

export interface StrikeResult { proposalCid?: string; price?: number; note?: string }
