import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as log from '../log';
import { inferReserveModel, protocolFor } from '../protocol';

test('redact masks registered secrets, including inside a JSON string literal', () => {
  log.clearSecrets();
  log.registerSecrets(['ck_live_0123456789abcdef', 'short', 'wh"sec/ret_8']);
  const line = JSON.stringify({ error: 'GET https://x -> 401 with Bearer ck_live_0123456789abcdef', s: 'wh"sec/ret_8', ok: 'short stays' });
  const out = log.redact(line);
  assert.doesNotMatch(out, /ck_live_0123456789abcdef/);
  assert.doesNotMatch(out, /sec\/ret_8/, 'a secret with characters JSON escapes is still masked');
  assert.match(out, /short stays/, 'values under 8 chars are not registered');
  assert.doesNotThrow(() => JSON.parse(out), 'the line is still JSON');
  log.clearSecrets();
});

test('redact masks credential-shaped text even when nothing was registered', () => {
  log.clearSecrets();
  const cases: Array<[string, RegExp]> = [
    ['curl -H "Authorization: Bearer eyJhbGciOi.xxx" https://x', /eyJhbGciOi/],
    ['https://svc/x?token=abcd1234efgh&y=1', /abcd1234efgh/],
    ['psql postgres://app:S3cretPw@db.internal:5432/por', /S3cretPw/],
    ["export API_KEY='ak_9988776655'", /ak_9988776655/],
    ['{"password":"hunter22"}', /hunter22/],
  ];
  for (const [text, secret] of cases) {
    const out = log.redact(text);
    assert.doesNotMatch(out, secret, text);
    assert.match(out, /\*\*\*/, text);
  }
  assert.equal(log.redact('{"authMode":"apikey","secretConfigured":true}'), '{"authMode":"apikey","secretConfigured":true}', 'ordinary keys are untouched');
  assert.equal(log.redact('postgres://app@db.internal/por'), 'postgres://app@db.internal/por', 'userinfo without a password is untouched');
});

test('log() writes redacted JSON lines', () => {
  log.clearSecrets();
  log.registerSecrets(['tok_secret_value_1234']);
  const chunks: string[] = [];
  const orig = process.stdout.write;
  process.stdout.write = ((c: string | Uint8Array) => { chunks.push(String(c)); return true; }) as typeof process.stdout.write;
  try {
    log.setLevel('info');
    log.error('decision', { error: 'conditions.x.y: command failed (exit 1): auth tok_secret_value_1234 rejected' });
  } finally {
    process.stdout.write = orig;
    log.setLevel('error');
    log.clearSecrets();
  }
  assert.equal(chunks.length, 1);
  const parsed = JSON.parse(chunks[0]) as { event: string; error: string };
  assert.equal(parsed.event, 'decision');
  assert.doesNotMatch(parsed.error, /tok_secret_value_1234/);
  assert.match(parsed.error, /auth \*\*\* rejected/);
});

test('inferReserveModel reads the issuer profile the backend serves for an instrument', () => {
  const p = (conds: Array<{ name: string; passesWhen?: string }>) => ({ version: 'SIGNER_PROTOCOL v2', roles: [{ key: 'issuer', conditions: conds }] });
  assert.equal(inferReserveModel(p([{ name: 'attestor-quorum' }, { name: 'reserves-current' }])), 'attested');
  assert.equal(inferReserveModel(p([{ name: 'reserves-current', passesWhen: 'Your on-chain verification of the locked reserve is less than 24h old' }])), 'onchain-verifiable');
  assert.equal(inferReserveModel(p([{ name: 'reserves-current', passesWhen: "The custodian's most recent holdings statement is less than 24h old" }])), 'custodial');
  assert.equal(inferReserveModel(p([{ name: 'reserves-current' }])), 'unknown');
  assert.equal(inferReserveModel({ version: 'v', roles: [] }), 'unknown');
  assert.throws(() => protocolFor('custodian', 'CBTC', p([{ name: 'attestor-quorum' }])), /no 'custodian' role/);
});
