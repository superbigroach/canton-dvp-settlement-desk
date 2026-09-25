import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { parseConfig, type Seat } from '../config';
import { CrossDeskClient, type Proposal, type ProtocolRole } from '../client';
import { State } from '../state';
import { evaluateProposal, handleProposal, type HandlerDeps } from '../handler';
import { ProtocolCache, roleOf, staticLookup, type ProtocolLookup } from '../protocol';
import * as log from '../log';

log.setLevel('error');

function tmpState(): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'crossdesk-signer-'));
  return path.join(dir, 'state.json');
}

interface Call { method: string; path: string; body?: unknown }

/** A fake CrossDesk that records what it was sent. `answer` overrides a path's reply. */
function fakeClient(calls: Call[], confirmStatus = 200, answer?: (p: string) => Response | undefined) {
  const fetchImpl = (async (url: string | URL | Request, init?: RequestInit) => {
    const u = String(url);
    const p = u.replace(/^https?:\/\/[^/]+/, '');
    calls.push({ method: init?.method ?? 'GET', path: p, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    const custom = answer?.(p);
    if (custom) return custom;
    const status = p.endsWith('/confirm') ? confirmStatus : 200;
    return new Response(JSON.stringify({ ok: true, path: p }), { status, headers: { 'content-type': 'application/json' } });
  }) as typeof fetch;
  return new CrossDeskClient({ baseUrl: 'https://x.test', apiKey: 'ck_test', fetchImpl });
}

/** The v2 protocol as GET /api/signer-protocol?instrument= serves it, keyed by reserve model. */
const V2: Record<Seat, string[]> = {
  issuer: ['attestor-quorum', 'reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'],
  lender: ['independent-mark-within-tolerance', 'liquidations-consistent', 'book-acceptance'],
  venue: ['traded-range', 'spread-within-tolerance', 'sufficient-volume', 'no-prints-attested'],
  custodian: ['holdings-current', 'holdings-cover-supply', 'no-encumbrance'],
  'transfer-agent': ['shares-outstanding-reconciled', 'fees-accrued'],
};
const ISSUER_3 = ['reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'];

const v2lookup = (seat: Seat, model: 'attested' | 'onchain-verifiable' | 'custodial' = 'attested'): ProtocolLookup =>
  staticLookup(seat, roleOf(seat, seat === 'issuer' && model !== 'attested' ? ISSUER_3 : V2[seat]), 'SIGNER_PROTOCOL v2', model);

const cfg = (seat: Seat, conditions: Record<string, unknown>, extra: Record<string, unknown> = {}) => parseConfig({
  crossdesk: { baseUrl: 'https://x.test', apiKey: 'ck_test' },
  seat, instruments: ['CBTC', 'cETH'], conditions, ...extra,
}, {});

const venueConfig = (extra: Record<string, unknown> = {}) => cfg('venue', {
  'traded-range': { prints: { kind: 'static', value: [64950, 65020, 65100] } },
  'spread-within-tolerance': { bid: 64990, ask: 65010 },
  'sufficient-volume': { volume: 4 },
  'no-prints-attested': { bestBid: 64980, bestAsk: 65020 },
}, { tolerances: { minVolume: 1 }, ...extra });

const proposal = (over: Partial<Proposal> = {}): Proposal => ({
  cid: '00aa:1', rootCid: '00aa:0', instrument: 'CBTC', price: 65000,
  conditions: ['traded-range', 'spread-within-tolerance', 'sufficient-volume', 'no-prints-attested'],
  requiresObservedRange: true, my: { seat: 'venue', action: 'pending', canConfirm: true }, mine: null, ...over,
});

const confirmBody = (calls: Call[]) => calls.find((c) => c.path.endsWith('/confirm'))!.body as { checks: string[]; evidence: Record<string, any> };

// ---- one confirm per seat: the right checks and the right evidence block ----------------

test('venue with prints: one confirm with traded-range + spread + volume; {low, high} hoisted; no-prints never claimed', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'confirm');
  assert.equal(d?.venueMode, 'traded-range');
  const confirm = calls.find((c) => c.path.endsWith('/confirm'));
  assert.equal(confirm!.path, '/api/proposals/00aa%3A1/confirm');
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, ['traded-range', 'spread-within-tolerance', 'sufficient-volume']);
  assert.equal(body.evidence.low, 64950, 'venue range hoisted to the top level for the ledger');
  assert.equal(body.evidence.high, 65100);
  assert.deepEqual(body.evidence['traded-range'], { low: 64950, high: 65100 });
  assert.equal(body.evidence['sufficient-volume'].volume, 4);
  assert.ok(body.evidence['spread-within-tolerance'].spreadBps > 0);
  assert.equal('no-prints-attested' in body.evidence, false);
  assert.ok(deps.state.has('00aa:0'), 'recorded under the root cid');
});

test('issuer (attested, cBTC): four checks, evidence per condition with the backend field names', async () => {
  const calls: Call[] = [];
  const c = cfg('issuer', {
    'attestor-quorum': { quorumSigners: 8, quorumThreshold: 7 },
    'reserves-current': { reservesAsOf: new Date().toISOString() },
    'reserves-cover-supply': { reserves: 1250.75, supply: 1200 },
    'redemption-queue-clear': { queueDepth: 0 },
  });
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('issuer', 'attested') };
  const d = await handleProposal(deps, proposal({ conditions: V2.issuer, my: { seat: 'issuer', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  assert.equal(d?.protocol?.model, 'attested');
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, V2.issuer);
  assert.deepEqual(body.evidence['attestor-quorum'], { quorumSigners: 8, quorumThreshold: 7 });
  assert.ok(typeof body.evidence['reserves-current'].reservesAsOf === 'string');
  assert.deepEqual(body.evidence['reserves-cover-supply'], { reserves: 1250.75, supply: 1200 });
  assert.deepEqual(body.evidence['redemption-queue-clear'], { queueDepth: 0, maxQueueDepth: 0 });
  assert.equal('low' in body.evidence, false, 'no range for an issuer');
});

test('issuer (onchain-verifiable, cETH): the protocol lookup drops attestor-quorum; three checks, quorum source never resolved', async () => {
  const calls: Call[] = [];
  const touched: string[] = [];
  const c = cfg('issuer', {
    // The same config serves cBTC and cETH: the quorum source exists but must not be consulted for cETH.
    'attestor-quorum': { quorumSigners: { kind: 'command', command: 'attest-status', pointer: '/n' }, quorumThreshold: 7 },
    'reserves-current': { reservesAsOf: new Date().toISOString() },
    'reserves-cover-supply': { reserves: 40210.5, supply: 40000 },
    'redemption-queue-clear': { queueDepth: 0 },
  });
  const runner = async (cmd: string) => { touched.push(cmd); return '{"n":1}'; };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('issuer', 'onchain-verifiable'), runner };
  // The ROW still lists the strict profile (audit medium #5); the lookup wins.
  const d = await handleProposal(deps, proposal({ instrument: 'cETH', price: 2400, conditions: V2.issuer, my: { seat: 'issuer', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  assert.equal(d?.protocol?.model, 'onchain-verifiable');
  assert.deepEqual(confirmBody(calls).checks, ISSUER_3);
  assert.equal('attestor-quorum' in confirmBody(calls).evidence, false);
  assert.deepEqual(touched, [], 'the attestor source was not run for an on-chain asset');
});

test('lender: evidence carries independentMark, deviation, liquidations and acceptedAt; command source runs', async () => {
  const calls: Call[] = [];
  const c = parseConfig({
    crossdesk: { baseUrl: 'https://x.test', sandboxUser: 'lender@sandbox.crossdesk' },
    seat: 'lender', instruments: ['CBTC'],
    conditions: {
      'independent-mark-within-tolerance': { independentMark: { kind: 'http', url: 'https://risk.test/marks/{instrument}', pointer: '/mark' } },
      'liquidations-consistent': { liquidationsToday: 1, worstDeviationBps: 12 },
      'book-acceptance': { acceptedAt: { kind: 'command', command: 'accept {instrument} {price}', pointer: '/acceptedAt' } },
    },
  }, {});
  const seen: string[] = [];
  const fetcher = async (url: string) => { seen.push(url); return { ok: true, status: 200, text: async () => '{"mark":65050}' }; };
  const runner = async (cmd: string) => { seen.push(cmd); return JSON.stringify({ acceptedAt: new Date().toISOString() }); };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('lender'), fetcher, runner };
  const d = await handleProposal(deps, proposal({ conditions: V2.lender, my: { seat: 'lender', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  assert.deepEqual(seen, ['https://risk.test/marks/CBTC', 'accept CBTC 65000']);
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, V2.lender);
  assert.equal(body.evidence['independent-mark-within-tolerance'].independentMark, 65050);
  assert.equal(body.evidence['liquidations-consistent'].liquidationsToday, 1);
  assert.equal(body.evidence['liquidations-consistent'].worstDeviationBps, 12);
  assert.ok(typeof body.evidence['book-acceptance'].acceptedAt === 'string');
  assert.equal('low' in body.evidence, false, 'no range for a lender');
});

test('custodian: three checks with statementAsOf / holdings+supply / encumbered', async () => {
  const calls: Call[] = [];
  const c = cfg('custodian', {
    'holdings-current': { statementAsOf: { kind: 'http', url: 'https://custody.test/{instrument}', pointer: '/asOf' } },
    'holdings-cover-supply': {
      holdings: { kind: 'http', url: 'https://custody.test/{instrument}', pointer: '/balance' },
      supply: 1000000,
    },
    'no-encumbrance': { encumbered: { kind: 'http', url: 'https://custody.test/{instrument}', pointer: '/encumbered' } },
  });
  const urls: string[] = [];
  const fetcher = async (url: string) => { urls.push(url); return { ok: true, status: 200, text: async () => JSON.stringify({ asOf: new Date().toISOString(), balance: 1000250, encumbered: 0 }) }; };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('custodian'), fetcher };
  const d = await handleProposal(deps, proposal({ conditions: V2.custodian, my: { seat: 'custodian', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  assert.deepEqual(urls, ['https://custody.test/CBTC'], 'one statement read serves all three fields');
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, V2.custodian);
  assert.ok(typeof body.evidence['holdings-current'].statementAsOf === 'string');
  assert.deepEqual(body.evidence['holdings-cover-supply'], { holdings: 1000250, supply: 1000000 });
  assert.deepEqual(body.evidence['no-encumbrance'], { encumbered: 0 });
  assert.equal('low' in body.evidence, false);
});

test('transfer-agent: two checks with registerShares+ledgerShares / accruedFees', async () => {
  const calls: Call[] = [];
  const c = cfg('transfer-agent', {
    'shares-outstanding-reconciled': { registerShares: 2500000, ledgerShares: { kind: 'command', command: 'ledger-shares {instrument}', parse: 'text' } },
    'fees-accrued': { accruedFees: 1834.22 },
  });
  const runner = async () => '2500000\n';
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('transfer-agent'), runner };
  const d = await handleProposal(deps, proposal({ conditions: V2['transfer-agent'], my: { seat: 'transfer-agent', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, V2['transfer-agent']);
  assert.deepEqual(body.evidence['shares-outstanding-reconciled'], { registerShares: 2500000, ledgerShares: 2500000 });
  assert.deepEqual(body.evidence['fees-accrued'], { accruedFees: 1834.22 });
});

test('transfer-agent: a register that disagrees with the ledger refuses, naming both counts', async () => {
  const calls: Call[] = [];
  const c = cfg('transfer-agent', { 'shares-outstanding-reconciled': { registerShares: 2500000, ledgerShares: 2499000 }, 'fees-accrued': { accruedFees: 0 } });
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('transfer-agent') };
  const d = await handleProposal(deps, proposal({ my: { seat: 'transfer-agent', canConfirm: true } }));
  assert.equal(d?.decision, 'refuse');
  const refuse = calls.find((c) => c.path.endsWith('/refuse'))!.body as { condition: string; reason: string };
  assert.equal(refuse.condition, 'shares-outstanding-reconciled');
  assert.match(refuse.reason, /2500000 .* 2499000/);
});

// ---- venue: the no-prints path ------------------------------------------------------------

test('venue with an EMPTY tape: confirms no-prints-attested alone with bestBid/bestAsk, no range, no spread/volume', async () => {
  const calls: Call[] = [];
  const c = venueConfig();
  c.conditions['traded-range'] = { prints: { kind: 'static', value: [] } };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'confirm');
  assert.equal(d?.venueMode, 'no-prints-attested');
  const body = confirmBody(calls);
  assert.deepEqual(body.checks, ['no-prints-attested'], 'never traded-range and no-prints-attested together');
  assert.deepEqual(body.evidence, { 'no-prints-attested': { bestBid: 64980, bestAsk: 65020 } });
  assert.equal('low' in body.evidence, false, 'the backend refuses a range next to no-prints-attested');
  assert.equal('sufficient-volume' in body.evidence, false);
});

test('venue with an empty tape and a quote that does not bracket the price: refuses no-prints-attested', async () => {
  const calls: Call[] = [];
  const c = venueConfig();
  c.conditions['traded-range'] = { prints: { kind: 'static', value: [] } };
  c.conditions['no-prints-attested'] = { bestBid: { kind: 'static', value: 65100 }, bestAsk: { kind: 'static', value: 65200 } };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'refuse');
  assert.equal(d?.failed?.condition, 'no-prints-attested');
  assert.equal(calls.some((c) => c.path.endsWith('/confirm')), false);
});

test('venue with an empty tape and attestNoPrints: false -> halt, nothing sent (v1 behaviour)', async () => {
  const calls: Call[] = [];
  const c = venueConfig({ venue: { attestNoPrints: false } });
  c.conditions['traded-range'] = { prints: { kind: 'static', value: [] } };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'halt');
  assert.match(d!.halt!, /attestNoPrints is off/);
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0);
  assert.equal(deps.state.size(), 0);
});

test('venue with an empty tape on a v1 protocol (no no-prints-attested condition) -> halt', async () => {
  const c = venueConfig();
  c.conditions['traded-range'] = { prints: { kind: 'static', value: [] } };
  const v1 = staticLookup('venue', roleOf('venue', ['traded-range', 'spread-within-tolerance', 'sufficient-volume']), 'SIGNER_PROTOCOL v1');
  const d = await evaluateProposal({ config: c, client: fakeClient([]), state: new State(tmpState()), protocol: v1 }, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /no 'no-prints-attested' condition/);
});

test('venue: a null tape is a source problem, not an empty window -> halt', async () => {
  const c = venueConfig();
  c.conditions['traded-range'] = { prints: { kind: 'static', value: null } };
  const d = await evaluateProposal({ config: c, client: fakeClient([]), state: new State(tmpState()), protocol: v2lookup('venue') }, proposal());
  assert.equal(d.decision, 'halt');
  assert.equal(d.venueMode, 'traded-range');
  assert.match(d.halt!, /prints/);
});

// ---- halts: any unevaluable condition stops the proposal ----------------------------------

test('a source that cannot be evaluated -> halt: nothing sent, nothing recorded, so it retries', async () => {
  const calls: Call[] = [];
  const c = venueConfig();
  c.conditions['sufficient-volume'] = { volume: { kind: 'http', url: 'https://down.test/v', pointer: '/volume' } };
  const fetcher = async () => ({ ok: false, status: 503, text: async () => 'down' });
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue'), fetcher };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'halt');
  assert.match(d!.halt!, /source failed/);
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0);
  assert.equal(deps.state.size(), 0);
});

test('a halt beats a refusal: one failed condition and one unevaluable one -> halt, no refuse is sent', async () => {
  const calls: Call[] = [];
  const c = cfg('issuer', {
    'attestor-quorum': { quorumSigners: 5, quorumThreshold: 7 },                 // FAILS on real numbers
    'reserves-current': { reservesAsOf: new Date().toISOString() },
    'reserves-cover-supply': { reserves: { kind: 'http', url: 'https://por.test', pointer: '/r' }, supply: 9 }, // UNEVALUABLE
    'redemption-queue-clear': { queueDepth: 0 },
  });
  const fetcher = async () => ({ ok: false, status: 500, text: async () => 'boom' });
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('issuer'), fetcher };
  const d = await handleProposal(deps, proposal({ my: { seat: 'issuer', canConfirm: true } }));
  assert.equal(d?.decision, 'halt');
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0, 'neither confirm nor refuse');
  assert.equal((d!.conditions['attestor-quorum'] as { pass: boolean }).pass, false, 'the failure was still evaluated and logged');
});

test('lender: a null acceptance stamp halts (retry) rather than refusing', async () => {
  const calls: Call[] = [];
  const c = cfg('lender', {
    'independent-mark-within-tolerance': { independentMark: 65010 },
    'liquidations-consistent': { liquidationsToday: 0, worstDeviationBps: 0 },
    'book-acceptance': { acceptedAt: { kind: 'command', command: 'accept', pointer: '/acceptedAt' } },
  });
  const runner = async () => '{"acceptedAt":null}';
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('lender'), runner };
  const d = await handleProposal(deps, proposal({ my: { seat: 'lender', canConfirm: true } }));
  assert.equal(d?.decision, 'halt');
  assert.match(d!.halt!, /no acceptance stamp/);
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0);
});

test('a condition with no configured source is a halt, not a confirm', async () => {
  const c = venueConfig();
  delete c.conditions['sufficient-volume'];
  const d = await evaluateProposal({ config: c, client: fakeClient([]), state: new State(tmpState()), protocol: v2lookup('venue') }, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /no data source configured for 'sufficient-volume'/);
});

test('a protocol condition this build has no rule for is a halt', async () => {
  const c = cfg('custodian', { 'holdings-current': { statementAsOf: new Date().toISOString() }, 'holdings-cover-supply': { holdings: 1, supply: 1 }, 'no-encumbrance': { encumbered: 0 } });
  const v3 = staticLookup('custodian', roleOf('custodian', [...V2.custodian, 'segregation-attested']), 'SIGNER_PROTOCOL v3');
  const d = await evaluateProposal({ config: c, client: fakeClient([]), state: new State(tmpState()), protocol: v3 }, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /unknown condition 'segregation-attested'/);
});

test('protocol-declared evidence the config cannot produce is a halt', async () => {
  const c = venueConfig();
  // The backend's schema shape: { required, fields: [{ name, type, description }], verifiedBy }.
  const role: ProtocolRole = { key: 'venue', conditions: [
    { name: 'traded-range', evidence: { required: true, fields: [{ name: 'low', type: 'number' }, { name: 'high', type: 'number' }], verifiedBy: 'ledger' } },
    { name: 'spread-within-tolerance', evidence: { required: true, fields: [{ name: 'spreadBps', type: 'number' }, { name: 'depthAtTouch', type: 'number' }], verifiedBy: 'signer' } },
    { name: 'sufficient-volume', evidence: { required: false, fields: [], verifiedBy: 'signer' } },
    { name: 'no-prints-attested', evidence: { required: true, fields: [{ name: 'bestBid', type: 'number' }, { name: 'bestAsk', type: 'number' }], verifiedBy: 'server' } },
  ], requiresObservedRange: true };
  const d = await evaluateProposal({ config: c, client: fakeClient([]), state: new State(tmpState()), protocol: staticLookup('venue', role) }, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /depthAtTouch/);
});

test('a failed protocol lookup is a halt: nothing evaluated, nothing sent', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), protocol: async () => { throw new Error('GET /api/signer-protocol?instrument=CBTC -> HTTP 502'); } };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'halt');
  assert.match(d!.halt!, /protocol lookup for CBTC failed/);
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0);
});

// ---- the protocol lookup against the real client ---------------------------------------------

test('the default lookup calls GET /api/signer-protocol?instrument=<id> once per instrument (cached) and infers the model', async () => {
  const calls: Call[] = [];
  const protocol = (issuerConds: string[], wording: string) => JSON.stringify({ version: 'SIGNER_PROTOCOL v2', roles: [
    { key: 'issuer', conditions: issuerConds.map((name) => ({ name, passesWhen: name === 'reserves-current' ? wording : '' })) },
    { key: 'venue', conditions: V2.venue.map((name) => ({ name })), requiresObservedRange: true },
  ] });
  const client = fakeClient(calls, 200, (p) => {
    if (p === '/api/signer-protocol?instrument=CBTC') return new Response(protocol(V2.issuer, 'attestation is less than 24h old'), { status: 200 });
    if (p === '/api/signer-protocol?instrument=cETH') return new Response(protocol(ISSUER_3, 'Your on-chain verification of the locked reserve is less than 24h old'), { status: 200 });
    return undefined;
  });
  const cache = new ProtocolCache(client, 'issuer', 60_000);
  const a = await cache.lookup('CBTC');
  const b = await cache.lookup('cETH');
  await cache.lookup('CBTC');
  assert.equal(a.model, 'attested');
  assert.deepEqual(a.role.conditions.map((c) => c.name), V2.issuer);
  assert.equal(b.model, 'onchain-verifiable');
  assert.deepEqual(b.role.conditions.map((c) => c.name), ISSUER_3);
  assert.deepEqual(calls.map((c) => c.path), ['/api/signer-protocol?instrument=CBTC', '/api/signer-protocol?instrument=cETH'], 'second CBTC lookup served from cache');
  assert.deepEqual(cache.models(), { CBTC: 'attested', CETH: 'onchain-verifiable' });
});

// ---- the CrossDesk side: 2xx non-JSON, 5xx, idempotency ----------------------------------------

test('a 2xx non-JSON /api/proposals is a failure, not a list: openProposals throws, so the poll goes red', async () => {
  const calls: Call[] = [];
  const client = fakeClient(calls, 200, (p) => (p.startsWith('/api/proposals?') ? new Response('<html><body>Sign in</body></html>', { status: 200, headers: { 'content-type': 'text/html' } }) : undefined));
  await assert.rejects(client.openProposals(), (e: Error) => {
    assert.match(e.message, /HTTP 200 but the body is not JSON/);
    assert.match(e.message, /Sign in/);
    return true;
  });
  // An empty 2xx is the same failure.
  const empty = fakeClient([], 200, (p) => (p.startsWith('/api/proposals?') ? new Response('', { status: 200 }) : undefined));
  await assert.rejects(empty.openProposals(), /not JSON/);
  // JSON that is not a list is refused too - never iterated.
  const obj = fakeClient([], 200, (p) => (p.startsWith('/api/proposals?') ? new Response('{"message":"ok"}', { status: 200 }) : undefined));
  await assert.rejects(obj.openProposals(), /not a list of proposals/);
});

test('a 2xx non-JSON reply to a confirm is not ok and is not recorded, so the next poll re-checks', async () => {
  const calls: Call[] = [];
  const client = fakeClient(calls, 200, (p) => (p.endsWith('/confirm') ? new Response('OK', { status: 200 }) : undefined));
  const deps: HandlerDeps = { config: venueConfig(), client, state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'confirm');
  assert.equal(d?.http?.status, 200);
  assert.equal(deps.state.size(), 0, 'not recorded: a body we could not read is not a confirmation');
});

test('evidence numbers are rounded to Numeric 10 so the ledger accepts them', async () => {
  const calls: Call[] = [];
  const c = venueConfig();
  c.conditions['traded-range'] = { prints: { kind: 'static', value: [77292.955 * 0.999, 77292.955 * 1.0012] } };
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  await handleProposal(deps, proposal({ price: 77292.955 }));
  const body = confirmBody(calls);
  assert.equal(body.evidence.high, 77385.706546);
  assert.equal(String(body.evidence.low).split('.')[1].length <= 10, true);
});

test('idempotent: the same proposal is never acted on twice, across cids and restarts', async () => {
  const calls: Call[] = [];
  const file = tmpState();
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(file), protocol: v2lookup('venue') };
  await handleProposal(deps, proposal());
  await handleProposal(deps, proposal({ cid: '00aa:2' }));
  const reloaded: HandlerDeps = { ...deps, state: new State(file) };
  await handleProposal(reloaded, proposal({ cid: '00aa:3' }));
  assert.equal(calls.filter((c) => c.method === 'POST').length, 1);
});

test('a failed condition -> refuse naming it, with the numbers; nothing confirmed', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal({ price: 66000 }));
  assert.equal(d?.decision, 'refuse');
  assert.equal(calls.some((c) => c.path.endsWith('/confirm')), false);
  const refuse = calls.find((c) => c.path.endsWith('/refuse'));
  assert.deepEqual(refuse!.body, { condition: 'traded-range', reason: 'proposed 66000 outside traded range 64950-65100 (3 print(s))' });
});

test('already confirmed on CrossDesk -> recorded, not re-sent', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('venue') };
  const d = await handleProposal(deps, proposal({ mine: { action: 'confirmed' } }));
  assert.equal(d, null);
  assert.equal(calls.length, 0);
  assert.equal(deps.state.get('00aa:0')?.decision, 'already-confirmed');
});

test('issuer: quorum below threshold refuses with "5 of 7"', async () => {
  const calls: Call[] = [];
  const c = cfg('issuer', {
    'attestor-quorum': { quorumSigners: 5, quorumThreshold: 7 },
    'reserves-current': { reservesAsOf: new Date().toISOString() },
    'reserves-cover-supply': { reserves: 10, supply: 9 },
    'redemption-queue-clear': { queueDepth: 0 },
  });
  const deps: HandlerDeps = { config: c, client: fakeClient(calls), state: new State(tmpState()), protocol: v2lookup('issuer') };
  const d = await handleProposal(deps, proposal({ my: { seat: 'issuer', canConfirm: true } }));
  assert.equal(d?.decision, 'refuse');
  assert.equal(d?.failed?.condition, 'attestor-quorum');
  assert.match(d!.failed!.reason, /5 of 7/);
});

test('a 5xx from CrossDesk is not recorded, so the next poll retries', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls, 502), state: new State(tmpState()), protocol: v2lookup('venue') };
  await handleProposal(deps, proposal());
  assert.equal(deps.state.size(), 0);
  await handleProposal(deps, proposal());
  assert.equal(calls.filter((c) => c.path.endsWith('/confirm')).length, 2);
});

test('a 403 "no signer seat" is a credential problem, not a verdict: not recorded, the next poll retries', async () => {
  // The desk refuses a confirm from a credential with no seat (an admin key used directly on a
  // host running AUTH_MODE=firebase). Recording it would mean the seat never confirms again even
  // after the key is fixed - the one failure mode that cannot be recovered from a state file.
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls, 403), state: new State(tmpState()), protocol: v2lookup('venue') };
  await handleProposal(deps, proposal());
  assert.equal(deps.state.size(), 0, '403 is not terminal');
  await handleProposal(deps, proposal());
  assert.equal(calls.filter((c) => c.path.endsWith('/confirm')).length, 2, 'retried');
});

test('a 401 and a 429 retry; a 422 and a 409 are terminal', async () => {
  for (const status of [401, 429]) {
    const deps: HandlerDeps = { config: venueConfig(), client: fakeClient([], status), state: new State(tmpState()), protocol: v2lookup('venue') };
    await handleProposal(deps, proposal());
    assert.equal(deps.state.size(), 0, `${status} must not be recorded`);
  }
  for (const status of [422, 409]) {
    const deps: HandlerDeps = { config: venueConfig(), client: fakeClient([], status), state: new State(tmpState()), protocol: v2lookup('venue') };
    await handleProposal(deps, proposal());
    assert.equal(deps.state.get('00aa:0')?.decision, 'rejected', `${status} is the desk's verdict`);
  }
});

test('X-Act-As rides alongside the credential so one admin key can drive every seat', async () => {
  const calls: Call[] = [];
  const seen: Array<Record<string, string>> = [];
  const fetchImpl = (async (url: string | URL | Request, init?: RequestInit) => {
    seen.push(init?.headers as Record<string, string>);
    calls.push({ method: init?.method ?? 'GET', path: String(url).replace(/^https?:\/\/[^/]+/, '') });
    return new Response(
      JSON.stringify({ version: 'SIGNER_PROTOCOL v2', roles: [{ key: 'venue', conditions: [], requiresObservedRange: true }] }),
      { status: 200, headers: { 'content-type': 'application/json' } },
    );
  }) as typeof fetch;
  const client = new CrossDeskClient({ baseUrl: 'https://x.test', apiKey: 'ck_admin', actAs: 'venue@sandbox.crossdesk', fetchImpl });
  assert.deepEqual(client.authHeaders(), { authorization: 'Bearer ck_admin', 'x-act-as': 'venue@sandbox.crossdesk' });
  assert.equal(client.actingAs, 'venue@sandbox.crossdesk');
  await client.signerProtocol('CBTC');
  assert.equal(seen[0]['x-act-as'], 'venue@sandbox.crossdesk');
  assert.equal(seen[0].authorization, 'Bearer ck_admin');
  // the sandbox header path carries it too
  const sandbox = new CrossDeskClient({ baseUrl: 'https://x.test', sandboxUser: 'admin@x', actAs: 'lender@sandbox.crossdesk', fetchImpl });
  assert.deepEqual(sandbox.authHeaders(), { 'x-sandbox-user': 'admin@x', 'x-act-as': 'lender@sandbox.crossdesk' });
  // and without it nothing changes
  const plain = new CrossDeskClient({ baseUrl: 'https://x.test', apiKey: 'ck_x', fetchImpl });
  assert.deepEqual(plain.authHeaders(), { authorization: 'Bearer ck_x' });
  assert.equal(plain.actingAs, undefined);
});
