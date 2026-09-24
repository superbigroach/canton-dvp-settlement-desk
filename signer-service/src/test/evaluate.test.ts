import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DEFAULT_TOLERANCES } from '../config';
import { MissingEvidence, RULES, isEmptyTape, satisfiedAlternative, type EvalContext } from '../evaluate';

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
  assert.throws(() => r({ quorumSigners: 5, quorumThreshold: 0 }, ctx('issuer')), MissingEvidence, 'a zero threshold is unevaluable, not a pass');
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
  assert.throws(() => cover({ reserves: -1, supply: 1000 }, ctx('issuer')), MissingEvidence, 'a negative reserve is garbage, not a refusal');
  const q = RULES.issuer['redemption-queue-clear'].rule;
  assert.equal(q({ queueDepth: 0 }, ctx('issuer')).pass, true);
  const bad = q({ queueDepth: 3 }, ctx('issuer', 65000, { maxQueueDepth: 1 }));
  assert.equal(bad.pass, false);
  assert.deepEqual(bad.evidence, { queueDepth: 3, maxQueueDepth: 1 });
});

test('issuer: maxQueueDepth comes from the tolerances only - a source value is ignored', () => {
  const q = RULES.issuer['redemption-queue-clear'].rule;
  // A source that claims maxQueueDepth: 99 must not widen the check.
  const r = q({ queueDepth: 3, maxQueueDepth: 99 }, ctx('issuer', 65000, { maxQueueDepth: 0 }));
  assert.equal(r.pass, false);
  assert.equal(r.evidence.maxQueueDepth, 0, 'the evidence carries the configured tolerance');
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
  assert.equal(b({ acceptedAt: false }, ctx('lender')).pass, false, 'explicit no is a refusal');
});

test('lender: a null acceptance stamp is unevaluable (halt), not a refusal', () => {
  const b = RULES.lender['book-acceptance'].rule;
  assert.throws(() => b({ acceptedAt: null }, ctx('lender')), MissingEvidence);
  assert.throws(() => b({ acceptedAt: undefined }, ctx('lender')), MissingEvidence);
  assert.throws(() => b({ acceptedAt: '' }, ctx('lender')), MissingEvidence);
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

test('venue: no-prints-attested - quoted book brackets the price, one-sided or empty book is not a contradiction', () => {
  const r = RULES.venue['no-prints-attested'].rule;
  const ok = r({ bestBid: 64950, bestAsk: 65050 }, ctx('venue'));
  assert.equal(ok.pass, true);
  assert.deepEqual(ok.evidence, { bestBid: 64950, bestAsk: 65050 });
  const out = r({ bestBid: 65100, bestAsk: 65200 }, ctx('venue'));
  assert.equal(out.pass, false);
  assert.match(out.reason, /outside best bid\/ask 65100\/65200/);
  assert.equal(r({ bestBid: 0, bestAsk: 0 }, ctx('venue')).pass, true, 'empty book');
  assert.equal(r({ bestBid: 64000, bestAsk: 0 }, ctx('venue')).pass, true, 'one-sided book');
  assert.throws(() => r({ bestBid: 65100, bestAsk: 65000 }, ctx('venue')), MissingEvidence, 'crossed quote is unevaluable');
  assert.throws(() => r({ bestBid: -1, bestAsk: 65000 }, ctx('venue')), MissingEvidence);
  assert.throws(() => r({ bestBid: 64950 }, ctx('venue')), MissingEvidence);
});

test('isEmptyTape: only an explicit empty list is "nothing traded"', () => {
  assert.equal(isEmptyTape([]), true);
  assert.equal(isEmptyTape([65000]), false);
  assert.equal(isEmptyTape(null), false);
  assert.equal(isEmptyTape(undefined), false);
  assert.equal(isEmptyTape({}), false);
});

test('custodian: holdings-current, holdings-cover-supply, no-encumbrance', () => {
  const cur = RULES.custodian['holdings-current'].rule;
  const fresh = cur({ statementAsOf: '2026-09-02T02:00:00Z' }, ctx('custodian'));
  assert.equal(fresh.pass, true);
  assert.deepEqual(fresh.evidence, { statementAsOf: '2026-09-02T02:00:00.000Z' });
  const stale = cur({ statementAsOf: '2026-08-30T00:00:00Z' }, ctx('custodian'));
  assert.equal(stale.pass, false);
  assert.match(stale.reason, /holdings statement .* 88h old \(max 24h\)/);
  assert.equal(cur({ statementAsOf: '2026-08-30T00:00:00Z' }, ctx('custodian', 65000, { holdingsMaxAgeHours: 96 })).pass, true, 'slower custody cycle declared in config');
  const cover = RULES.custodian['holdings-cover-supply'].rule;
  assert.equal(cover({ holdings: 1000, supply: 1000 }, ctx('custodian')).pass, true);
  const short = cover({ holdings: 999, supply: 1000 }, ctx('custodian'));
  assert.equal(short.pass, false);
  assert.deepEqual(short.evidence, { holdings: 999, supply: 1000 });
  const enc = RULES.custodian['no-encumbrance'].rule;
  assert.equal(enc({ encumbered: 0 }, ctx('custodian')).pass, true);
  const pledged = enc({ encumbered: 12.5 }, ctx('custodian'));
  assert.equal(pledged.pass, false);
  assert.match(pledged.reason, /12.5 unit\(s\) pledged/);
  assert.throws(() => enc({ encumbered: -1 }, ctx('custodian')), MissingEvidence);
});

test('transfer-agent: shares-outstanding-reconciled is exact; fees-accrued is non-negative', () => {
  const rec = RULES['transfer-agent']['shares-outstanding-reconciled'].rule;
  assert.equal(rec({ registerShares: 2500000, ledgerShares: 2500000 }, ctx('transfer-agent')).pass, true);
  const off = rec({ registerShares: 2500000, ledgerShares: 2499990 }, ctx('transfer-agent'));
  assert.equal(off.pass, false);
  assert.deepEqual(off.evidence, { registerShares: 2500000, ledgerShares: 2499990 });
  assert.match(off.reason, /register shows 2500000 shares but the ledger shows 2499990/);
  assert.throws(() => rec({ registerShares: 1 }, ctx('transfer-agent')), MissingEvidence);
  const fees = RULES['transfer-agent']['fees-accrued'].rule;
  assert.equal(fees({ accruedFees: 0 }, ctx('transfer-agent')).pass, true);
  assert.equal(fees({ accruedFees: 1834.22 }, ctx('transfer-agent')).evidence.accruedFees, 1834.22);
  assert.equal(fees({ accruedFees: -5 }, ctx('transfer-agent')).pass, false);
  assert.throws(() => fees({ accruedFees: 'tbd' }, ctx('transfer-agent')), MissingEvidence);
});

test('every v2 condition of every seat has a rule', () => {
  const v2: Record<string, string[]> = {
    issuer: ['attestor-quorum', 'reserves-current', 'reserves-cover-supply', 'redemption-queue-clear'],
    lender: ['independent-mark-within-tolerance', 'liquidations-consistent', 'book-acceptance'],
    venue: ['traded-range', 'spread-within-tolerance', 'sufficient-volume', 'no-prints-attested'],
    custodian: ['holdings-current', 'holdings-cover-supply', 'no-encumbrance'],
    'transfer-agent': ['shares-outstanding-reconciled', 'fees-accrued'],
  };
  for (const [seat, names] of Object.entries(v2)) {
    assert.deepEqual(Object.keys(RULES[seat as EvalContext['seat']]).sort(), names.sort(), seat);
  }
});

test('missing evidence is a MissingEvidence error, never a pass', () => {
  assert.throws(() => RULES.issuer['attestor-quorum'].rule({ quorumSigners: 9 }, ctx('issuer')), MissingEvidence);
  assert.throws(() => RULES.venue['sufficient-volume'].rule({ volume: 'n/a' }, ctx('venue')), MissingEvidence);
  assert.throws(() => RULES.custodian['holdings-current'].rule({ statementAsOf: 'soon' }, ctx('custodian')), MissingEvidence);
});

test('satisfiedAlternative picks the configured field set', () => {
  assert.deepEqual(satisfiedAlternative(RULES.venue['traded-range'], ['low', 'high']), ['low', 'high']);
  assert.deepEqual(satisfiedAlternative(RULES.venue['traded-range'], ['prints']), ['prints']);
  assert.equal(satisfiedAlternative(RULES.venue['traded-range'], ['low']), null);
});
