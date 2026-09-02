import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DEFAULT_TOLERANCES } from '../config';
import { MissingEvidence, RULES, satisfiedAlternative, type EvalContext } from '../evaluate';

const now = new Date('2026-09-02T16:00:00Z');
const ctx = (seat: EvalContext['seat'], price = 65000, tol: Record<string, number> = {}): EvalContext => ({
  seat, instrument: 'CBTC', price, cid: '00aa:1', now, tolerances: { ...DEFAULT_TOLERANCES, ...tol },
});

test('issuer: attestor-quorum passes at threshold, fails below, carries both numbers', () => {
  const r = RULES.issuer['attestor-quorum'].rule;
  assert.equal(r({ quorumSigners: 7, quorumThreshold: 7 }, ctx('issuer')).pass, true);
  const f = r({ quorumSigners: '5', quorumThreshold: 10 }, ctx('issuer'));
  assert.equal(f.pass, false);
  assert.deepEqual(f.evidence, { quorumSigners: 5, quorumThreshold: 10 });
  assert.match(f.reason, /5 of 10/);
});

test('issuer: reserves-current uses the tolerance, never widens it', () => {
  const r = RULES.issuer['reserves-current'].rule;
  assert.equal(r({ reservesAsOf: '2026-09-02T00:00:00Z' }, ctx('issuer')).pass, true);
  const stale = r({ reservesAsOf: '2026-08-31T00:00:00Z' }, ctx('issuer'));
  assert.equal(stale.pass, false);
  assert.match(stale.reason, /64h old \(max 24h\)/);
  assert.equal(r({ reservesAsOf: 1788307200 }, ctx('issuer')).pass, true, 'epoch seconds accepted');
  assert.throws(() => r({ reservesAsOf: 'yesterday' }, ctx('issuer')), MissingEvidence);
});

test('issuer: reserves-cover-supply and redemption-queue-clear', () => {
  const cover = RULES.issuer['reserves-cover-supply'].rule;
  assert.equal(cover({ reserves: 1000, supply: 1000 }, ctx('issuer')).pass, true);
  assert.equal(cover({ reserves: 999.9, supply: 1000 }, ctx('issuer')).pass, false);
  const q = RULES.issuer['redemption-queue-clear'].rule;
  assert.equal(q({ queueDepth: 0 }, ctx('issuer')).pass, true);
  const bad = q({ queueDepth: 3, maxQueueDepth: 1 }, ctx('issuer'));
  assert.equal(bad.pass, false);
  assert.deepEqual(bad.evidence, { queueDepth: 3, maxQueueDepth: 1 });
});

test('lender: independent mark within 25bp passes, 26bp fails, deviation is evidence', () => {
  const r = RULES.lender['independent-mark-within-tolerance'].rule;
  const ok = r({ independentMark: 65100 }, ctx('lender', 65000));
  assert.equal(ok.pass, true);
  assert.ok((ok.evidence.deviationBps as number) < 25);
  const no = r({ independentMark: 65000 }, ctx('lender', 65200));
  assert.equal(no.pass, false);
  assert.match(no.reason, /30.77bp \(tolerance 25bp\)/);
  assert.equal(r({ independentMark: 65200 }, ctx('lender', 65000, { markToleranceBps: 40 })).pass, true);
});

test('lender: liquidations-consistent and book-acceptance', () => {
  const l = RULES.lender['liquidations-consistent'].rule;
  assert.equal(l({ liquidationsToday: 0 }, ctx('lender')).pass, true);
  assert.equal(l({ liquidationsToday: 2, worstDeviationBps: 80 }, ctx('lender')).pass, true);
  assert.equal(l({ liquidationsToday: 2, worstDeviationBps: 180 }, ctx('lender')).pass, false);
  const b = RULES.lender['book-acceptance'].rule;
  const fresh = b({ acceptedAt: '2026-09-02T15:50:00Z' }, ctx('lender'));
  assert.equal(fresh.pass, true);
  assert.deepEqual(fresh.evidence, { acceptedAt: '2026-09-02T15:50:00.000Z' });
  assert.equal(b({ acceptedAt: '2026-09-02T10:00:00Z' }, ctx('lender')).pass, false, 'stale acceptance');
  assert.equal(b({ acceptedAt: false }, ctx('lender')).pass, false, 'explicit no');
});

test('venue: traded-range computes low/high from prints and enforces containment', () => {
  const r = RULES.venue['traded-range'].rule;
  const ok = r({ prints: [64900, { price: 65020 }, { px: 65110 }] }, ctx('venue'));
  assert.equal(ok.pass, true);
  assert.deepEqual(ok.evidence, { low: 64900, high: 65110 });
  const out = r({ prints: [64000, 64500] }, ctx('venue'));
  assert.equal(out.pass, false);
  assert.match(out.reason, /outside traded range 64000-64500/);
  assert.equal(r({ low: 64990, high: 65010 }, ctx('venue')).pass, true);
  assert.throws(() => r({ prints: [] }, ctx('venue')), MissingEvidence);
  assert.throws(() => r({ low: 66000, high: 65000 }, ctx('venue')), /inverted/);
});

test('venue: spread and volume', () => {
  const s = RULES.venue['spread-within-tolerance'].rule;
  const fromBook = s({ bid: 64990, ask: 65010 }, ctx('venue'));
  assert.equal(fromBook.pass, true);
  assert.equal(fromBook.evidence.spreadBps, 3.08);
  assert.equal(s({ spreadBps: 51 }, ctx('venue')).pass, false);
  const v = RULES.venue['sufficient-volume'].rule;
  assert.equal(v({ volume: 0.5 }, ctx('venue', 65000, { minVolume: 1 })).pass, false);
  assert.equal(v({ volume: 2 }, ctx('venue', 65000, { minVolume: 1 })).pass, true);
});

test('missing evidence is a MissingEvidence error, never a pass', () => {
  assert.throws(() => RULES.issuer['attestor-quorum'].rule({ quorumSigners: 9 }, ctx('issuer')), MissingEvidence);
  assert.throws(() => RULES.venue['sufficient-volume'].rule({ volume: 'n/a' }, ctx('venue')), MissingEvidence);
});

test('satisfiedAlternative picks the configured field set', () => {
  assert.deepEqual(satisfiedAlternative(RULES.venue['traded-range'], ['low', 'high']), ['low', 'high']);
  assert.deepEqual(satisfiedAlternative(RULES.venue['traded-range'], ['prints']), ['prints']);
  assert.equal(satisfiedAlternative(RULES.venue['traded-range'], ['low']), null);
});
