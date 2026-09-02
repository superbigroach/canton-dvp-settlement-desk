import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { parsePayload, signature, verifySignature } from '../webhook';

const secret = 'whsec_test';
const body = Buffer.from('{"type":"proposal.created","instrument":"CBTC","proposalCid":"00ab:12","price":65000}');

test('signature matches the backend format sha256=<hex hmac over body bytes>', () => {
  const expected = 'sha256=' + createHmac('sha256', secret).update(body).digest('hex');
  assert.equal(signature(secret, body), expected);
});

test('verify accepts a good signature and rejects a bad, missing or mis-secret one', () => {
  const good = signature(secret, body);
  assert.equal(verifySignature(secret, body, good), true);
  assert.equal(verifySignature(secret, body, good.toUpperCase()), true, 'hex case is not significant');
  assert.equal(verifySignature(secret, body, good.slice(0, -1) + '0'), false);
  assert.equal(verifySignature(secret, body, undefined), false);
  assert.equal(verifySignature('other', body, good), false);
  assert.equal(verifySignature(undefined, body, good), false, 'no secret configured never verifies');
  assert.equal(verifySignature(secret, Buffer.from(body.toString() + ' '), good), false, 'exact bytes matter');
});

test('parsePayload requires type, proposalCid and instrument', () => {
  const p = parsePayload(body);
  assert.equal(p.type, 'proposal.created');
  assert.equal(p.proposalCid, '00ab:12');
  assert.throws(() => parsePayload(Buffer.from('{"type":"x"}')));
  assert.throws(() => parsePayload(Buffer.from('not json')));
});
