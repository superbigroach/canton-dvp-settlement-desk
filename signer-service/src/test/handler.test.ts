import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { parseConfig } from '../config';
import { CrossDeskClient, type Proposal } from '../client';
import { State } from '../state';
import { evaluateProposal, handleProposal, type HandlerDeps } from '../handler';
import * as log from '../log';

log.setLevel('error');

function tmpState(): string {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'crossdesk-signer-'));
  return path.join(dir, 'state.json');
}

interface Call { method: string; path: string; body?: unknown }

/** A fake CrossDesk that records what it was sent. */
function fakeClient(calls: Call[], confirmStatus = 200) {
  const fetchImpl = (async (url: string | URL | Request, init?: RequestInit) => {
    const u = String(url);
    const p = u.replace(/^https?:\/\/[^/]+/, '');
    calls.push({ method: init?.method ?? 'GET', path: p, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    const status = p.endsWith('/confirm') ? confirmStatus : 200;
    return new Response(JSON.stringify({ ok: true, path: p }), { status, headers: { 'content-type': 'application/json' } });
  }) as typeof fetch;
  return new CrossDeskClient({ baseUrl: 'https://x.test', apiKey: 'ck_test', fetchImpl });
}

const venueConfig = () => parseConfig({
  crossdesk: { baseUrl: 'https://x.test', apiKey: 'ck_test' },
  seat: 'venue',
  instruments: ['CBTC'],
  tolerances: { minVolume: 1 },
  conditions: {
    'traded-range': { prints: { kind: 'static', value: [64950, 65020, 65100] } },
    'spread-within-tolerance': { bid: 64990, ask: 65010 },
    'sufficient-volume': { volume: 4 },
  },
}, {});

const proposal = (over: Partial<Proposal> = {}): Proposal => ({
  cid: '00aa:1', rootCid: '00aa:0', instrument: 'CBTC', price: 65000,
  conditions: ['traded-range', 'spread-within-tolerance', 'sufficient-volume'],
  requiresObservedRange: true, my: { seat: 'venue', action: 'pending', canConfirm: true }, mine: null, ...over,
});

test('venue: all conditions pass -> one confirm with checks and evidence {low, high, spreadBps, volume}', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), role: undefined };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'confirm');
  const confirm = calls.find((c) => c.path.endsWith('/confirm'));
  assert.ok(confirm, 'confirm was sent');
  assert.equal(confirm!.path, '/api/proposals/00aa%3A1/confirm');
  const body = confirm!.body as { checks: string[]; evidence: Record<string, number> };
  assert.deepEqual(body.checks, ['traded-range', 'spread-within-tolerance', 'sufficient-volume']);
  assert.equal(body.evidence.low, 64950, 'venue range hoisted to the top level for the ledger');
  assert.equal(body.evidence.high, 65100);
  const blocks = body.evidence as unknown as Record<string, Record<string, number>>;
  assert.deepEqual(blocks['traded-range'], { low: 64950, high: 65100 });
  assert.equal(blocks['sufficient-volume'].volume, 4);
  assert.ok(blocks['spread-within-tolerance'].spreadBps > 0);
  assert.ok(deps.state.has('00aa:0'), 'recorded under the root cid');
});

test('evidence numbers are rounded to Numeric 10 so the ledger accepts them', async () => {
  const calls: Call[] = [];
  const cfg = venueConfig();
  cfg.conditions['traded-range'] = { prints: { kind: 'static', value: [77292.955 * 0.999, 77292.955 * 1.0012] } };
  const deps: HandlerDeps = { config: cfg, client: fakeClient(calls), state: new State(tmpState()), role: undefined };
  await handleProposal(deps, proposal({ price: 77292.955 }));
  const body = calls.find((c) => c.path.endsWith('/confirm'))!.body as { evidence: Record<string, number> };
  assert.equal(body.evidence.high, 77385.706546);
  assert.equal(String(body.evidence.low).split('.')[1].length <= 10, true);
});

test('idempotent: the same proposal is never acted on twice, across cids and restarts', async () => {
  const calls: Call[] = [];
  const file = tmpState();
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(file), role: undefined };
  await handleProposal(deps, proposal());
  await handleProposal(deps, proposal({ cid: '00aa:2' }));
  const reloaded: HandlerDeps = { ...deps, state: new State(file) };
  await handleProposal(reloaded, proposal({ cid: '00aa:3' }));
  assert.equal(calls.filter((c) => c.method === 'POST').length, 1);
});

test('a failed condition -> refuse naming it, with the numbers; nothing confirmed', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), role: undefined };
  const d = await handleProposal(deps, proposal({ price: 66000 }));
  assert.equal(d?.decision, 'refuse');
  assert.equal(calls.some((c) => c.path.endsWith('/confirm')), false);
  const refuse = calls.find((c) => c.path.endsWith('/refuse'));
  assert.deepEqual(refuse!.body, { condition: 'traded-range', reason: 'proposed 66000 outside traded range 64950-65100 (3 print(s))' });
});

test('a source that cannot be evaluated -> halt: nothing sent, nothing recorded, so it retries', async () => {
  const calls: Call[] = [];
  const cfg = venueConfig();
  cfg.conditions['sufficient-volume'] = { volume: { kind: 'http', url: 'https://down.test/v', pointer: '/volume' } };
  const fetcher = async () => ({ ok: false, status: 503, text: async () => 'down' });
  const deps: HandlerDeps = { config: cfg, client: fakeClient(calls), state: new State(tmpState()), role: undefined, fetcher };
  const d = await handleProposal(deps, proposal());
  assert.equal(d?.decision, 'halt');
  assert.match(d!.halt!, /source failed/);
  assert.equal(calls.filter((c) => c.method === 'POST').length, 0);
  assert.equal(deps.state.size(), 0);
});

test('a condition with no configured source is a halt, not a confirm', async () => {
  const cfg = venueConfig();
  delete cfg.conditions['sufficient-volume'];
  const deps: HandlerDeps = { config: cfg, client: fakeClient([]), state: new State(tmpState()), role: undefined };
  const d = await evaluateProposal(deps, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /no data source configured for 'sufficient-volume'/);
});

test('protocol-declared evidence the config cannot produce is a halt', async () => {
  const cfg = venueConfig();
  // The backend's schema shape: { required, fields: [{ name, type, description }], verifiedBy }.
  const role = { key: 'venue', conditions: [
    { name: 'traded-range', evidence: { required: true, fields: [{ name: 'low', type: 'number' }, { name: 'high', type: 'number' }], verifiedBy: 'ledger' } },
    { name: 'spread-within-tolerance', evidence: { required: true, fields: [{ name: 'spreadBps', type: 'number' }, { name: 'depthAtTouch', type: 'number' }], verifiedBy: 'signer' } },
    { name: 'sufficient-volume', evidence: { required: false, fields: [], verifiedBy: 'signer' } },
  ], requiresObservedRange: true };
  const deps: HandlerDeps = { config: cfg, client: fakeClient([]), state: new State(tmpState()), role };
  const d = await evaluateProposal(deps, proposal());
  assert.equal(d.decision, 'halt');
  assert.match(d.halt!, /depthAtTouch/);
});

test('already confirmed on CrossDesk -> recorded, not re-sent', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls), state: new State(tmpState()), role: undefined };
  const d = await handleProposal(deps, proposal({ mine: { action: 'confirmed' } }));
  assert.equal(d, null);
  assert.equal(calls.length, 0);
  assert.equal(deps.state.get('00aa:0')?.decision, 'already-confirmed');
});

test('lender: evidence carries independentMark, deviation, liquidations and acceptedAt; command source runs', async () => {
  const calls: Call[] = [];
  const cfg = parseConfig({
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
  const deps: HandlerDeps = { config: cfg, client: fakeClient(calls), state: new State(tmpState()), role: undefined, fetcher, runner };
  const d = await handleProposal(deps, proposal({ conditions: ['independent-mark-within-tolerance', 'liquidations-consistent', 'book-acceptance'], my: { seat: 'lender', canConfirm: true } }));
  assert.equal(d?.decision, 'confirm');
  assert.deepEqual(seen, ['https://risk.test/marks/CBTC', 'accept CBTC 65000']);
  const body = calls.find((c) => c.path.endsWith('/confirm'))!.body as { evidence: Record<string, Record<string, unknown>> };
  assert.equal(body.evidence['independent-mark-within-tolerance'].independentMark, 65050);
  assert.equal(body.evidence['liquidations-consistent'].liquidationsToday, 1);
  assert.equal(body.evidence['liquidations-consistent'].worstDeviationBps, 12);
  assert.ok(typeof body.evidence['book-acceptance'].acceptedAt === 'string');
  assert.equal('low' in body.evidence, false, 'no range for a lender');
});

test('issuer: quorum below threshold refuses with "5 of 7"', async () => {
  const calls: Call[] = [];
  const cfg = parseConfig({
    crossdesk: { baseUrl: 'https://x.test', apiKey: 'k' },
    seat: 'issuer', instruments: ['CBTC'],
    conditions: {
      'attestor-quorum': { quorumSigners: 5, quorumThreshold: 7 },
      'reserves-current': { reservesAsOf: new Date().toISOString() },
      'reserves-cover-supply': { reserves: 10, supply: 9 },
      'redemption-queue-clear': { queueDepth: 0 },
    },
  }, {});
  const deps: HandlerDeps = { config: cfg, client: fakeClient(calls), state: new State(tmpState()), role: undefined };
  const d = await handleProposal(deps, proposal({ conditions: ['attestor-quorum', 'reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'], my: { seat: 'issuer', canConfirm: true } }));
  assert.equal(d?.decision, 'refuse');
  assert.equal(d?.failed?.condition, 'attestor-quorum');
  assert.match(d!.failed!.reason, /5 of 7/);
});

test('a 5xx from CrossDesk is not recorded, so the next poll retries', async () => {
  const calls: Call[] = [];
  const deps: HandlerDeps = { config: venueConfig(), client: fakeClient(calls, 502), state: new State(tmpState()), role: undefined };
  await handleProposal(deps, proposal());
  assert.equal(deps.state.size(), 0);
  await handleProposal(deps, proposal());
  assert.equal(calls.filter((c) => c.path.endsWith('/confirm')).length, 2);
});
