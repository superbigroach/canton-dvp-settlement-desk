// In-memory stand-in for the §5 contract, switched on by VITE_API_MOCK=1.
//
// It is a DEVELOPMENT aid so the app can be built before the backend lands. Numbers are
// invented and say so in the UI (the shell shows a "mock data" tag). State lives for the
// page's lifetime: confirming a proposal really moves it, creating shares really writes
// a receipt — so every screen's optimistic path can be exercised end to end.
import { ApiError, type SignerProtocolResponse } from '../api';
import { findSandboxUser, SANDBOX_USERS } from '../auth/sandboxUsers';
import { actAsUser, sandboxUser } from '../auth/token';
import type { DeskClient } from './client';
import type {
  BenchmarkLast,
  ApFund, Benchmark, Committee, FixingEvent, FundDashboard, Me, Proposal, ProposalEvent, Receipt,
  ScheduleRow, SeriesRow, SignerSettings, UserRow,
} from './types';

const wait = (ms = 220) => new Promise<void>((r) => setTimeout(r, ms));
const iso = (d: Date) => d.toISOString();
const minutesFromNow = (m: number) => iso(new Date(Date.now() + m * 60_000));
const daysAgo = (d: number) => new Date(Date.now() - d * 86_400_000);
const hex = (n: number) => Math.floor(Math.random() * n).toString(16);

const PROTOCOL: SignerProtocolResponse = {
  version: '1.0',
  roles: [
    { key: 'issuer', title: 'Issuer', uniquelyKnows: 'the wrapper factor and the reserve it represents',
      conditions: [
        { name: 'reserve-attested', passesWhen: 'the reserve report at the strike covers every unit outstanding' },
        { name: 'factor-current', passesWhen: 'the factor in the proposal is the one last published' },
      ], requiresObservedRange: false },
    { key: 'lender', title: 'Lender', uniquelyKnows: 'what collateral was actually accepted at',
      conditions: [
        { name: 'within-haircut-band', passesWhen: 'the proposed price sits inside the band collateral was margined at today' },
        { name: 'no-open-dispute', passesWhen: 'no margin dispute is open on this instrument' },
      ], requiresObservedRange: false },
    { key: 'venue', title: 'Venue', uniquelyKnows: 'the range its own book traded',
      conditions: [
        { name: 'book-acceptance', passesWhen: 'the proposal sits inside the traded range you attach' },
        { name: 'no-halt', passesWhen: 'the instrument was not halted in the window' },
      ], requiresObservedRange: true },
  ],
};

const roleFor = (seat?: string) => PROTOCOL.roles.find((r) => r.key === seat);

function me(): Me {
  const u = findSandboxUser(sandboxUser());
  if (!u) throw new ApiError('not signed in', 401);
  const { note: _note, ...rest } = u;
  // Admin "View as": honoured only for a real admin, and echoed back as actingAs.
  const act = actAsUser();
  if (act && rest.role === 'admin') {
    const target = findSandboxUser(act) ?? users.find((x) => x.email.toLowerCase() === act.toLowerCase());
    if (!target) throw new ApiError(`X-Act-As: no mapped user ${act}`, 404);
    const { note: _n2, ...t } = target as typeof u;
    return { ...t, actingAs: act };
  }
  return rest;
}

// ---- series -------------------------------------------------------------------
function makeSeries(start: number, days: number, factor?: number): SeriesRow[] {
  const rows: SeriesRow[] = [];
  let p = start;
  for (let i = days; i >= 0; i--) {
    const d = daysAgo(i);
    if (d.getDay() === 0 || d.getDay() === 6) continue;
    p = p * (1 + Math.sin(i * 1.7 + start) * 0.012);
    const tier = i === 9 ? 3 : 1;
    rows.push({
      date: d.toISOString().slice(0, 10),
      asOf: iso(new Date(d.setHours(16, 0, 0, 0))),
      price: Math.round(p * 100) / 100,
      referencePrice: factor ? Math.round((p / factor) * 100) / 100 : undefined,
      wrapperFactor: factor,
      tier, k: tier === 1 ? 2 : 0, n: 3,
      signers: tier === 1 ? ['Issuer', 'Bank'] : [],
      fixingCid: `00${(i * 7919).toString(16).padStart(6, '0')}…a1`,
      restated: i === 14,
    });
  }
  return rows.reverse();
}
const SERIES: Record<string, SeriesRow[]> = {
  CBTC: makeSeries(61_250, 40, 0.9985),
  cETH: makeSeries(2_410, 40, 0.999),
  LX1: makeSeries(102.4, 40),
};

/** A series row as a benchmark's `last`: the mock never publishes a gap, but the type allows one. */
const lastOf = (r: SeriesRow, ageSeconds: number): BenchmarkLast => ({ ...r, price: r.price ?? null, ageSeconds });

const BENCHMARKS: Benchmark[] = [
  { id: 'CBTC', name: 'CBTC Close', kind: 'wrapped', publishTime: '16:00', timezone: 'Europe/London',
    description: 'Closing value of cBTC on Canton: the BTC benchmark print × the last attested wrapper factor.',
    last: lastOf(SERIES.CBTC[0], 4_800), referencing: [{ id: 'LX1', name: 'LX1 NAV' }] },
  { id: 'cETH', name: 'cETH Close', kind: 'wrapped', publishTime: '16:00', timezone: 'Europe/London',
    description: 'Closing value of cETH on Canton: the ETH benchmark print × the last attested wrapper factor.',
    last: lastOf(SERIES.cETH[0], 4_800), referencing: [{ id: 'LX1', name: 'LX1 NAV' }] },
  { id: 'LX1', name: 'LX1 NAV', kind: 'nav', publishTime: '16:05', timezone: 'Europe/London',
    description: 'Net asset value per share of the LX1 basket: Σ units per share × attested component close.',
    last: lastOf(SERIES.LX1[0], 4_500), referencing: [] },
];

// ---- proposals ----------------------------------------------------------------
let proposals: Proposal[] = [
  { cid: '00a3f1…9c', instrument: 'CBTC', session: 'Close', kind: 'wrapped', price: 61_402.15, referencePrice: 61_494.4,
    wrapperFactor: 0.9985, proposedBy: 'Operator', proposedAt: minutesFromNow(-6), deadline: minutesFromNow(24),
    k: 2, n: 3, confirmed: ['issuer'], status: 'open', conditions: [], requiresObservedRange: false, mine: null },
  { cid: '00b7c2…4e', instrument: 'cETH', session: 'Close', kind: 'wrapped', price: 2_398.6, referencePrice: 2_401.0,
    wrapperFactor: 0.999, proposedBy: 'Operator', proposedAt: minutesFromNow(-3), deadline: minutesFromNow(27),
    k: 2, n: 3, confirmed: [], status: 'open', conditions: [], requiresObservedRange: false, mine: null },
  { cid: '00c9d4…77', instrument: 'LX1', session: 'Close', kind: 'nav', price: 102.87,
    navComponents: [
      { instrumentId: 'CBTC', unitsPerShare: 0.001, mark: 61_402.15 },
      { instrumentId: 'cETH', unitsPerShare: 0.0173, mark: 2_398.6 },
    ],
    proposedBy: 'Operator', proposedAt: minutesFromNow(-1), deadline: minutesFromNow(29),
    k: 2, n: 3, confirmed: [], status: 'open', conditions: [], requiresObservedRange: false, mine: null },
  { cid: '009e11…b0', instrument: 'CBTC', session: 'Close', kind: 'wrapped', price: 61_250.0, referencePrice: 61_342.0,
    wrapperFactor: 0.9985, proposedBy: 'Operator', proposedAt: iso(daysAgo(1)), deadline: iso(daysAgo(1)),
    k: 2, n: 3, confirmed: ['issuer', 'lender'], status: 'finalized', conditions: [], requiresObservedRange: false,
    mine: { action: 'confirmed', at: iso(daysAgo(1)), checks: ['reserve-attested'], cid: '009e12…c1' } },
];
const propEvents: Record<string, ProposalEvent[]> = {};
const ev = (cid: string, e: Omit<ProposalEvent, 'ts'> & { ts?: string }) => {
  (propEvents[cid] ||= []).push({ ts: e.ts || iso(new Date()), ...e });
};
for (const p of proposals) {
  ev(p.cid, { kind: 'proposed', actor: 'Operator', price: p.price, ts: p.proposedAt, cid: p.cid });
  ev(p.cid, { kind: 'webhook.sent', detail: '3 seats notified', ts: p.proposedAt });
  for (const s of p.confirmed) ev(p.cid, { kind: 'confirmed', seat: s, actor: s, ts: minutesFromNow(-2) });
  if (p.status === 'finalized') ev(p.cid, { kind: 'finalized', price: p.price, cid: '00fix…01', ts: p.deadline });
}

const events: FixingEvent[] = [];
for (const p of proposals) {
  for (const e of propEvents[p.cid]) {
    events.push({ ts: e.ts, instrument: p.instrument, proposalCid: p.cid, kind: e.kind, actor: e.actor, cid: e.cid, price: e.price });
  }
}
events.push({ ts: iso(daysAgo(9)), instrument: 'cETH', kind: 'fallback.tier3', actor: 'scheduler',
  reason: 'K not reached by window end — benchmark × last factor', price: 2_380.1, tier: 3 });

// ---- funds --------------------------------------------------------------------
const fund: ApFund = {
  id: 'LX1', name: 'LX1 basket', cash: 'USDC',
  official: { nav: SERIES.LX1[0].price ?? 0, asOf: SERIES.LX1[0].asOf, tier: 1, k: 2, n: 3, fixingCid: SERIES.LX1[0].fixingCid },
  indicative: Math.round((SERIES.LX1[0].price ?? 0) * 1.0021 * 100) / 100,
  components: [
    { instrumentId: 'CBTC', unitsPerShare: 0.001, mark: 61_402.15 },
    { instrumentId: 'cETH', unitsPerShare: 0.0173, mark: 2_398.6 },
  ],
  sharesOutstanding: 12_400,
  fee: { createBps: 15, redeemBps: 15, minimum: 25, currency: 'USDC' },
  cutoff: { time: '15:30', timezone: 'Europe/London', nextAt: minutesFromNow(95) },
  minShares: 100, lot: 100,
};
let receipts: Receipt[] = [
  { id: 'r-1001', fundId: 'LX1', kind: 'create', shares: 500, nav: 101.9,
    units: [{ instrumentId: 'CBTC', amount: 0.5 }, { instrumentId: 'cETH', amount: 8.65 }],
    fee: 76.43, feeCurrency: 'USDC', ts: iso(daysAgo(2)), cid: '00cr…21', status: 'settled', party: 'Alice' },
];

const settingsByUser: Record<string, SignerSettings> = {};
const users: UserRow[] = [];
let apiKeyPrefix: string | null = null;
let schedule: ScheduleRow[] = [
  { instrument: 'CBTC', session: 'Close', strikeAt: '16:00', timezone: 'Europe/London', windowMinutes: 30, enabled: true, tiers: { t2: false, t3: true, t4: true } },
  { instrument: 'cETH', session: 'Close', strikeAt: '16:00', timezone: 'Europe/London', windowMinutes: 30, enabled: true, tiers: { t2: false, t3: true, t4: true } },
  { instrument: 'LX1', session: 'Close', strikeAt: '16:05', timezone: 'Europe/London', windowMinutes: 30, enabled: true, tiers: { t2: false, t3: false, t4: true } },
];

const withMyConditions = (p: Proposal, m: Me): Proposal => {
  const role = roleFor(m.seat);
  return { ...p, conditions: role ? role.conditions.map((c) => c.name) : [], requiresObservedRange: role?.requiresObservedRange ?? false };
};

function receiptFor(m: Me, kind: 'create' | 'redeem', shares: number): Receipt {
  if (!fund.official) throw new ApiError(`no official NAV today — ${kind === 'create' ? 'creations' : 'redemptions'} are closed`, 409);
  const bps = kind === 'create' ? fund.fee.createBps : fund.fee.redeemBps;
  const r: Receipt = {
    id: `r-${1000 + receipts.length + 1}`, fundId: fund.id, kind, shares, nav: fund.official.nav,
    units: fund.components.map((c) => ({ instrumentId: c.instrumentId, amount: c.unitsPerShare * shares })),
    fee: Math.max(fund.fee.minimum ?? 0, (shares * fund.official.nav * bps) / 10_000), feeCurrency: 'USDC',
    ts: iso(new Date()), cid: `00${kind === 'create' ? 'cr' : 'rd'}…${(receipts.length + 30).toString(16)}`,
    status: 'settled', party: m.party,
  };
  receipts = [r, ...receipts];
  fund.sharesOutstanding = (fund.sharesOutstanding ?? 0) + (kind === 'create' ? shares : -shares);
  return r;
}

export const mockClient: DeskClient = {
  benchmarks: async () => { await wait(); return BENCHMARKS; },
  benchmark: async (id) => {
    await wait();
    const b = BENCHMARKS.find((x) => x.id === id);
    if (!b) throw new ApiError(`no benchmark ${id}`, 404);
    return b;
  },
  series: async (id, p = {}) => {
    await wait();
    const s = SERIES[id];
    if (!s) throw new ApiError(`no series ${id}`, 404);
    return s.slice(0, p.limit ?? s.length);
  },
  seriesCsv: async () => { await wait(); throw new ApiError('CSV download is not available in mock mode', 501); },
  methodology: async () => ({ version: '1.0', url: '/methodology', signerProtocolVersion: PROTOCOL.version }),
  signerProtocol: async () => { await wait(); return PROTOCOL; },
  fixingSchedule: async () => {
    await wait();
    return {
      identifiers: schedule.map((s) => ({
        instrumentId: s.instrument, session: s.session || 'Close', strikeAt: s.strikeAt, zone: s.timezone,
        graceMinutes: s.windowMinutes, state: s.instrument === 'cETH' ? 'OVERDUE' : 'STRUCK', expectedAt: minutesFromNow(-40),
        minutesLate: s.instrument === 'cETH' ? 12 : 0, note: s.instrument === 'cETH' ? 'awaiting second signature' : 'struck on time',
      })),
      overdueCount: 1, asOf: iso(new Date()),
    };
  },
  me: async () => { await wait(120); return me(); },

  proposals: async (status = 'open') => {
    await wait();
    const m = me();
    if (m.role !== 'signer' && m.role !== 'admin') throw new ApiError('your role is not a signer', 403);
    const mine = proposals.filter((p) => !m.instruments || m.instruments.includes(p.instrument));
    return (status === 'open' ? mine.filter((p) => p.status === 'open') : mine).map((p) => withMyConditions(p, m));
  },
  proposalEvents: async (cid) => { await wait(); return [...(propEvents[cid] || [])].reverse(); },
  confirm: async (cid, body) => {
    await wait(400);
    const m = me();
    const p = proposals.find((x) => x.cid === cid);
    if (!p) throw new ApiError('no such proposal', 404);
    if (p.status !== 'open') throw new ApiError('this proposal is no longer open', 409);
    if (!body.checks.length) throw new ApiError('name at least one condition you verified', 400);
    if (m.seat === 'venue') {
      if (!body.evidence) throw new ApiError('the venue seat must attach its traded range', 400);
      if (p.price < body.evidence.low || p.price > body.evidence.high) {
        throw new ApiError(`refused on-ledger: ${p.price} is outside the range ${body.evidence.low}–${body.evidence.high} you attached`, 409);
      }
    }
    const next = `${cid.slice(0, 4)}${hex(0xffff)}…${m.seat?.[0] ?? 'x'}1`;
    p.confirmed = [...p.confirmed, m.seat || m.party];
    p.mine = { action: 'confirmed', at: iso(new Date()), checks: body.checks, evidence: body.evidence, cid: next };
    ev(cid, { kind: 'confirmed', actor: m.party, seat: m.seat, cid: next, detail: body.checks.join(', ') });
    events.push({ ts: iso(new Date()), instrument: p.instrument, proposalCid: cid, kind: 'confirmed', actor: m.party, cid: next });
    if (p.confirmed.length >= p.k) {
      p.status = 'finalized';
      ev(cid, { kind: 'finalized', price: p.price, cid: '00fix…' + next.slice(-2) });
      events.push({ ts: iso(new Date()), instrument: p.instrument, proposalCid: cid, kind: 'finalized', price: p.price, tier: 1 });
    }
    return { cid: next };
  },
  refuse: async (cid, body) => {
    await wait(400);
    const m = me();
    const p = proposals.find((x) => x.cid === cid);
    if (!p) throw new ApiError('no such proposal', 404);
    if (!body.reason.trim()) throw new ApiError('a refusal needs a reason', 400);
    p.mine = { action: 'refused', at: iso(new Date()), reason: body.reason };
    p.status = 'refused';
    ev(cid, { kind: 'refused', actor: m.party, seat: m.seat, reason: `${body.condition}: ${body.reason}` });
    events.push({ ts: iso(new Date()), instrument: p.instrument, proposalCid: cid, kind: 'refused', actor: m.party, reason: body.reason });
    // the operator restrikes inside the window
    const re: Proposal = {
      ...p, cid: `${cid.slice(0, 4)}r${hex(0xfff)}…`, price: Math.round(p.price * 0.9992 * 100) / 100,
      proposedAt: iso(new Date()), deadline: minutesFromNow(25), confirmed: [], status: 'open', mine: null,
    };
    proposals = [re, ...proposals];
    ev(re.cid, { kind: 'restruck', actor: 'Operator', price: re.price, detail: `after refusal on ${cid}` });
    events.push({ ts: iso(new Date()), instrument: p.instrument, proposalCid: re.cid, kind: 'restruck', actor: 'Operator', price: re.price });
    return { cid: re.cid };
  },
  signerSettings: async () => {
    await wait();
    const m = me();
    return (settingsByUser[m.email] ||= {
      webhookUrl: '', email: m.email, tolerances: { maxDeviationBps: 50, maxAgeSeconds: 900 },
      apiKey: apiKeyPrefix ? { createdAt: iso(new Date()), prefix: apiKeyPrefix } : null,
    });
  },
  saveSignerSettings: async (s) => {
    await wait();
    const m = me();
    settingsByUser[m.email] = { ...s, webhookSecret: undefined };
    return settingsByUser[m.email];
  },
  createApiKey: async () => {
    await wait();
    const key = 'ck_' + Array.from(crypto.getRandomValues(new Uint8Array(18)), (b) => b.toString(16).padStart(2, '0')).join('');
    apiKeyPrefix = key.slice(0, 8);
    const m = me();
    if (settingsByUser[m.email]) settingsByUser[m.email].apiKey = { createdAt: iso(new Date()), prefix: apiKeyPrefix };
    return { key };
  },
  revokeApiKey: async () => {
    await wait();
    apiKeyPrefix = null;
    const m = me();
    if (settingsByUser[m.email]) settingsByUser[m.email].apiKey = null;
    return {};
  },

  apFunds: async () => {
    await wait();
    const m = me();
    if (m.role !== 'ap' && m.role !== 'admin') throw new ApiError('your role is not an authorised participant', 403);
    return [fund];
  },
  apCreate: async (fundId, shares) => {
    await wait(500);
    if (fundId !== fund.id) throw new ApiError(`no fund ${fundId}`, 404);
    return receiptFor(me(), 'create', shares);
  },
  apRedeem: async (fundId, shares) => {
    await wait(500);
    if (fundId !== fund.id) throw new ApiError(`no fund ${fundId}`, 404);
    return receiptFor(me(), 'redeem', shares);
  },
  apReceipts: async () => {
    await wait();
    const m = me();
    return receipts.filter((r) => m.role === 'admin' || r.party === m.party);
  },

  fundDashboard: async (id) => {
    await wait();
    if (id !== fund.id) throw new ApiError(`no fund ${id}`, 404);
    const d: FundDashboard = {
      id, name: fund.name, cash: fund.cash, series: SERIES.LX1, sharesOutstanding: fund.sharesOutstanding ?? 0,
      components: fund.components, log: receipts,
      fees: {
        accrued: receipts.reduce((a, r) => a + r.fee, 0), currency: 'USDC',
        rows: receipts.map((r) => ({ date: r.ts.slice(0, 10), kind: `${r.kind} fee`, amount: r.fee, ref: r.id })),
      },
      licensees: [{ name: 'LX1 basket', kind: 'fund NAV reference' }],
    };
    return d;
  },

  schedule: async () => { await wait(); return schedule; },
  saveSchedule: async (rows) => { await wait(); schedule = rows; return schedule; },
  strikeNow: async (id) => {
    await wait(600);
    const b = BENCHMARKS.find((x) => x.id === id);
    if (!b) throw new ApiError(`no instrument ${id}`, 404);
    const p: Proposal = {
      cid: `00s${hex(0xffff)}…`, instrument: id, session: 'Close', kind: b.kind === 'nav' ? 'nav' : 'wrapped',
      price: b.last?.price ? Math.round(b.last.price * 1.0008 * 100) / 100 : 0, referencePrice: b.last?.price ?? undefined,
      wrapperFactor: b.kind === 'wrapped' ? 0.9985 : undefined,
      proposedBy: 'Operator', proposedAt: iso(new Date()), deadline: minutesFromNow(30), k: 2, n: 3, confirmed: [],
      status: 'open', conditions: [], requiresObservedRange: false, mine: null,
    };
    proposals = [p, ...proposals];
    ev(p.cid, { kind: 'proposed', actor: 'Operator', price: p.price, cid: p.cid });
    events.push({ ts: iso(new Date()), instrument: id, proposalCid: p.cid, kind: 'proposed', actor: 'Operator', price: p.price });
    return { proposalCid: p.cid, price: p.price };
  },
  committees: async () => {
    await wait();
    const c = (instrument: string): Committee => ({
      instrument, k: 2, n: 3, seats: [
        { seat: 'issuer', party: 'Issuer', users: ['issuer@sandbox.crossdesk'], lastAction: { kind: 'confirmed', at: minutesFromNow(-2) } },
        { seat: 'lender', party: 'Bank', users: ['lender@sandbox.crossdesk'], lastAction: { kind: 'confirmed', at: iso(daysAgo(1)) } },
        { seat: 'venue', party: 'Venue', users: ['venue@sandbox.crossdesk'], lastAction: null },
      ],
    });
    return ['CBTC', 'cETH', 'LX1'].map(c);
  },
  users: async () => {
    await wait();
    return [...SANDBOX_USERS.map(({ note: _note, ...r }) => ({ ...r, enabled: true })), ...users];
  },
  usersAsSelf: async () => {
    await wait();
    return [...SANDBOX_USERS.map(({ note: _note, ...r }) => ({ ...r, enabled: true })), ...users];
  },
  addUser: async (u) => { await wait(); const row: UserRow = { uid: `u_${Date.now()}`, ...u, enabled: true }; users.push(row); return row; },
  updateUser: async (uid, u) => {
    await wait();
    const i = users.findIndex((x) => x.uid === uid);
    const row: UserRow = { uid, ...u };
    if (i >= 0) users[i] = row; else users.push(row);
    return row;
  },
  events: async (p = {}) => {
    await wait();
    return [...events].filter((e) => !p.instrument || e.instrument === p.instrument).sort((a, b) => b.ts.localeCompare(a.ts));
  },
  eventsCsv: async () => { await wait(); throw new ApiError('CSV download is not available in mock mode', 501); },
};
