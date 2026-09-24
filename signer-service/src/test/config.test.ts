import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import YAML from 'yaml';
import { loadConfig, parseConfig, secretsOf, substituteEnv } from '../config';
import { resolvePointer } from '../jsonpointer';
import { SourceResolver, interpolate } from '../sources';
import * as log from '../log';

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
  assert.deepEqual(c.conditions['attestor-quorum'].quorumSigners, { kind: 'static', label: 'conditions.attestor-quorum.quorumSigners', value: 8 });
  assert.equal(c.tolerances.markToleranceBps, 25, 'defaults present');
  assert.equal(c.tolerances.holdingsMaxAgeHours, 24, 'custodian default present');
  assert.equal(c.crossdesk.poll.enabled, true);
  assert.equal(c.venue.attestNoPrints, true, 'no-prints attestation is on by default');
  assert.equal(c.crossdesk.protocolCacheSeconds, 300);
});

test('all five v2 seats are accepted', () => {
  for (const seat of ['issuer', 'lender', 'venue', 'custodian', 'transfer-agent']) {
    const c = parseConfig({ crossdesk: { baseUrl: 'https://x', apiKey: 'k' }, seat, instruments: ['CBTC'] }, {});
    assert.equal(c.seat, seat);
  }
});

test('a tolerance can never come from a data source: maxQueueDepth under conditions is refused', () => {
  assert.throws(
    () => parseConfig({ crossdesk: { baseUrl: 'https://x', apiKey: 'k' }, seat: 'issuer', instruments: ['CBTC'], conditions: { 'redemption-queue-clear': { queueDepth: 0, maxQueueDepth: 99 } } }, {}),
    /maxQueueDepth.*tolerances\.maxQueueDepth only/,
  );
  assert.throws(
    () => parseConfig({ crossdesk: { baseUrl: 'https://x', apiKey: 'k' }, seat: 'issuer', instruments: ['CBTC'], conditions: { 'redemption-queue-clear': { queueDepth: 0, maxQueueDepth: { kind: 'http', url: 'https://ops/x', pointer: '/max' } } } }, {}),
    /maxQueueDepth/,
  );
});

test('venue.attestNoPrints can be switched off', () => {
  const c = parseConfig({ crossdesk: { baseUrl: 'https://x', apiKey: 'k' }, seat: 'venue', instruments: ['CBTC'], venue: { attestNoPrints: false } }, {});
  assert.equal(c.venue.attestNoPrints, false);
});

test('CROSSDESK_SANDBOX_USER from the environment is enough', () => {
  const c = parseConfig({ crossdesk: { baseUrl: 'https://x' }, seat: 'venue', instruments: ['CBTC'] }, { CROSSDESK_SANDBOX_USER: 'venue@sandbox.crossdesk' });
  assert.equal(c.crossdesk.sandboxUser, 'venue@sandbox.crossdesk');
});

test('the six example files parse and cover every condition of their seat under their model', () => {
  const dir = path.join(__dirname, '..', '..', 'examples');
  const expected: Record<string, { seat: string; conditions: string[] }> = {
    'issuer-attested': { seat: 'issuer', conditions: ['attestor-quorum', 'reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'] },
    'issuer-onchain': { seat: 'issuer', conditions: ['reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'] },
    lender: { seat: 'lender', conditions: ['independent-mark-within-tolerance', 'liquidations-consistent', 'book-acceptance'] },
    venue: { seat: 'venue', conditions: ['traded-range', 'spread-within-tolerance', 'sufficient-volume', 'no-prints-attested'] },
    custodian: { seat: 'custodian', conditions: ['holdings-current', 'holdings-cover-supply', 'no-encumbrance'] },
    'transfer-agent': { seat: 'transfer-agent', conditions: ['shares-outstanding-reconciled', 'fees-accrued'] },
  };
  for (const [file, want] of Object.entries(expected)) {
    const c = loadConfig(path.join(dir, `${file}.yml`), { CROSSDESK_SANDBOX_USER: `${want.seat}@sandbox.crossdesk` });
    assert.equal(c.seat, want.seat, `${file}.yml seat`);
    assert.deepEqual(Object.keys(c.conditions).sort(), want.conditions.sort(), `${file}.yml conditions`);
    assert.equal(c.crossdesk.baseUrl, 'https://crossdesk-devnet-app.web.app');
  }
  assert.deepEqual(fs.readdirSync(dir).sort(), Object.keys(expected).map((f) => `${f}.yml`).sort(), 'one example per seat/model, nothing else');
  // Also: the YAML is well-formed on its own terms.
  for (const f of fs.readdirSync(dir)) YAML.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
});

test('secretsOf collects the key, the webhook secret and every source bearer / header value', () => {
  const c = parseConfig({
    crossdesk: { baseUrl: 'https://x', apiKey: 'ck_0123456789abcdef', webhookSecret: 'whsec_abcdefgh' },
    seat: 'issuer', instruments: ['CBTC'],
    conditions: { 'attestor-quorum': {
      quorumSigners: { kind: 'http', url: 'https://a', bearer: 'tok_attest_123456', headers: { 'x-api-key': 'hdr_secret_value' }, pointer: '/n' },
      quorumThreshold: 7,
    } },
  }, {});
  assert.deepEqual(secretsOf(c).sort(), ['ck_0123456789abcdef', 'hdr_secret_value', 'tok_attest_123456', 'whsec_abcdefgh']);
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

test('a failed command source names its config path, never its command line; secrets in stderr are masked', async () => {
  log.clearSecrets();
  log.registerSecrets(['tok_inline_secret_9f8e7d']);
  const runner = async (cmd: string) => {
    // A shell that echoes the command line back on failure, as many do.
    throw new Error(`command failed (exit 127): sh: curl -H 'Authorization: Bearer tok_inline_secret_9f8e7d' https://x: not found; ${cmd}`);
  };
  const r = new SourceResolver({ instrument: 'CBTC', price: 1, cid: 'c', seat: 'issuer' }, undefined, runner);
  const spec = { kind: 'command' as const, label: 'conditions.attestor-quorum.quorumSigners', command: 'curl -H "Authorization: Bearer tok_inline_secret_9f8e7d" https://x?token=abcdefghijkl {instrument}', pointer: '/n' };
  await assert.rejects(r.resolve(spec), (e: Error) => {
    assert.match(e.message, /^conditions\.attestor-quorum\.quorumSigners: command failed \(exit 127\)/);
    assert.doesNotMatch(e.message, /tok_inline_secret_9f8e7d/, 'registered secret masked');
    assert.doesNotMatch(e.message, /token=abcdefghijkl/, 'token=... masked by shape');
    return true;
  });
  log.clearSecrets();
});

test('the real runner never puts the command line into the error (exec would)', async () => {
  const { defaultRunner } = await import('../sources');
  await assert.rejects(defaultRunner('exit 3', 5000), (e: Error) => {
    assert.match(e.message, /command failed \(exit 3\)/);
    assert.doesNotMatch(e.message, /exit 3\b.*exit 3/, 'the command text is not echoed');
    assert.doesNotMatch(e.message, /Command failed:/, "exec's own message (which carries the command line) is not used");
    return true;
  });
});
