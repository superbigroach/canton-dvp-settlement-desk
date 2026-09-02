import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import YAML from 'yaml';
import { loadConfig, parseConfig, substituteEnv } from '../config';
import { resolvePointer } from '../jsonpointer';
import { SourceResolver, interpolate } from '../sources';

test('env substitution: ${VAR}, ${VAR:-default}, unset -> empty', () => {
  const env = { A: 'x' };
  assert.equal(substituteEnv('${A}', env), 'x');
  assert.equal(substituteEnv('${B:-fallback}', env), 'fallback');
  assert.equal(substituteEnv('${B}', env), '');
  assert.deepEqual(substituteEnv({ k: ['${A}', 1, true] }, env), { k: ['x', 1, true] });
});

test('parseConfig: needs a credential, a seat, instruments; scalars are static sources', () => {
  assert.throws(() => parseConfig({ crossdesk: { baseUrl: 'https://x' }, seat: 'venue', instruments: ['CBTC'] }, {}), /apiKey|sandboxUser/);
  assert.throws(() => parseConfig({ crossdesk: { baseUrl: 'https://x', apiKey: 'k' }, seat: 'oracle', instruments: ['CBTC'] }, {}), /seat/);
  const c = parseConfig({ crossdesk: { baseUrl: 'https://x/', apiKey: 'k' }, seat: 'issuer', instruments: ['CBTC'], conditions: { 'attestor-quorum': { quorumSigners: 8, quorumThreshold: { kind: 'static', value: 7 } } } }, {});
  assert.equal(c.crossdesk.baseUrl, 'https://x');
  assert.deepEqual(c.conditions['attestor-quorum'].quorumSigners, { kind: 'static', value: 8 });
  assert.equal(c.tolerances.markToleranceBps, 25, 'defaults present');
  assert.equal(c.crossdesk.poll.enabled, true);
});

test('CROSSDESK_SANDBOX_USER from the environment is enough', () => {
  const c = parseConfig({ crossdesk: { baseUrl: 'https://x' }, seat: 'venue', instruments: ['CBTC'] }, { CROSSDESK_SANDBOX_USER: 'venue@sandbox.crossdesk' });
  assert.equal(c.crossdesk.sandboxUser, 'venue@sandbox.crossdesk');
});

test('the three example files parse and cover every condition of their seat', () => {
  const dir = path.join(__dirname, '..', '..', 'examples');
  const expected: Record<string, string[]> = {
    issuer: ['attestor-quorum', 'reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'],
    lender: ['independent-mark-within-tolerance', 'liquidations-consistent', 'book-acceptance'],
    venue: ['traded-range', 'spread-within-tolerance', 'sufficient-volume'],
  };
  for (const seat of Object.keys(expected)) {
    const c = loadConfig(path.join(dir, `${seat}.yml`), { CROSSDESK_SANDBOX_USER: `${seat}@sandbox.crossdesk` });
    assert.equal(c.seat, seat);
    assert.deepEqual(Object.keys(c.conditions).sort(), expected[seat].sort(), `${seat}.yml conditions`);
    assert.equal(c.crossdesk.baseUrl, 'https://crossdesk-devnet-app.web.app');
  }
  // Also: the YAML is well-formed on its own terms.
  for (const f of fs.readdirSync(dir)) YAML.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
});

test('json pointer', () => {
  const doc = { a: { 'b/c': [10, { d: 'x' }] }, n: 0 };
  assert.equal(resolvePointer(doc, ''), doc);
  assert.equal(resolvePointer(doc, '/a/b~1c/0'), 10);
  assert.equal(resolvePointer(doc, '/a/b~1c/1/d'), 'x');
  assert.equal(resolvePointer(doc, '/a/b~1c/-'), doc.a['b/c'][1]);
  assert.equal(resolvePointer(doc, '/n'), 0);
  assert.throws(() => resolvePointer(doc, '/missing'), /no key/);
  assert.throws(() => resolvePointer(doc, 'a'), /must start/);
});

test('interpolation only substitutes validated values', () => {
  const ctx = { instrument: 'CBTC', price: 65000, cid: '00aa:1', seat: 'venue' };
  assert.equal(interpolate('https://x/{instrument}?p={price}&c={cid}', ctx), 'https://x/CBTC?p=65000&c=00aa:1');
  assert.throws(() => interpolate('{instrument}', { ...ctx, instrument: 'CBTC; rm -rf /' }));
  assert.throws(() => interpolate('{cid}', { ...ctx, cid: '$(id)' }));
  assert.throws(() => interpolate('{price}', { ...ctx, price: NaN }));
});

test('SourceResolver: http fetched once for two pointers; bearer set; text parse; command', async () => {
  const urls: string[] = [];
  const fetcher = async (url: string, init: { headers: Record<string, string> }) => {
    urls.push(url + '|' + (init.headers.authorization ?? ''));
    return { ok: true, status: 200, text: async () => '{"low":1,"high":2}' };
  };
  const runner = async (cmd: string) => (cmd === 'echo 42' ? '42\n' : '{"v":"s"}');
  const r = new SourceResolver({ instrument: 'CBTC', price: 1, cid: 'c', seat: 'venue' }, fetcher, runner);
  const spec = { kind: 'http' as const, url: 'https://b/{instrument}', bearer: 't', headers: {} };
  assert.equal(await r.resolve({ ...spec, pointer: '/low' }), 1);
  assert.equal(await r.resolve({ ...spec, pointer: '/high' }), 2);
  assert.deepEqual(urls, ['https://b/CBTC|Bearer t']);
  assert.equal(await r.resolve({ kind: 'command', command: 'echo 42', parse: 'text' }), 42);
  assert.equal(await r.resolve({ kind: 'command', command: 'x', pointer: '/v' }), 's');
  assert.equal(await r.resolve({ kind: 'static', value: [1, 2] }).then((v) => (v as number[]).length), 2);
});
