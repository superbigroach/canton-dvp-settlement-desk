// The "Your seat" guide: what a committee member's seat asserts, the evidence it files,
// how to automate it, and the trust level it signs at.
//
// TWO SOURCES, KEPT APART ON PURPOSE.
//   1. Condition names, `passesWhen`, the evidence fields and the rule come from
//      `GET /api/signer-protocol?instrument=<id>` at render time (`fetchSeatProtocol`).
//      They are never copied here, so the panel cannot drift from what the confirm route
//      accepts — an issuer on an on-chain-verifiable asset is shown three conditions, not
//      four, because the API said so.
//   2. What the API does not carry — the one-sentence assertion, the TradFi analogue, an
//      example value per field, the refuse path, the checker quickstart, the trust ladder —
//      is prose from docs/SIGNER_PROTOCOL.md, BUSINESS/5-RUN-THE-COMMITTEE/6-RULEBOOK.md
//      §6.7, BUSINESS/4-ONBOARD-CLIENT/3-ASSET-ISSUER-PACK.md and signer-service/README.md,
//      keyed by seat. See frontend/src/pages/sign/ONBOARDING.md for the line-by-line map.
import type { SignerCondition, SignerProtocolResponse, SignerRole } from './api';
import { authHeaders } from './auth/token';
import { desk } from './desk';

// ---- the wire shape, including the `evidence` block api.ts does not yet type ----------

/** One field of evidence: `type` is integer | number | instant (Dtos.SignerConditionView → SignerEvidence.schemaOf). */
export interface EvidenceField {
  name: string;
  type: string;
  description: string;
}

/** The evidence a condition needs. `verifiedBy`: server (the desk, before submitting) | ledger (the Daml choice) | signer (recorded, not checked). */
export interface EvidenceSchema {
  required: boolean;
  verifiedBy: string;
  rule?: string;
  fields: EvidenceField[];
}

export interface GuideCondition extends SignerCondition {
  evidence?: EvidenceSchema;
}

export interface GuideRole extends Omit<SignerRole, 'conditions'> {
  conditions: GuideCondition[];
}

export interface GuideProtocol {
  version: string;
  roles: GuideRole[];
}

/**
 * The protocol for ONE instrument. With an instrument the issuer seat is served as that
 * asset's reserve model can honestly assert it; without one the strictest (attested)
 * profile is served. `desk.signerProtocol()` has no instrument parameter, so this goes to
 * the same public route directly; if that fails (mock mode, no network) it falls back to
 * the unqualified call so the panel still renders.
 */
export async function fetchSeatProtocol(instrument?: string): Promise<GuideProtocol> {
  const path = `/api/signer-protocol${instrument ? `?instrument=${encodeURIComponent(instrument)}` : ''}`;
  try {
    const headers = await authHeaders();
    for (const k of Object.keys(headers)) if (headers[k] === '') delete headers[k];
    const res = await fetch(path, { headers });
    if (res.ok) {
      const body = (await res.json()) as GuideProtocol;
      if (body && Array.isArray(body.roles)) return body;
    }
  } catch {
    // fall through to the client's unqualified call
  }
  const r: SignerProtocolResponse = await desk.signerProtocol();
  return r as GuideProtocol;
}

// ---- seat prose ------------------------------------------------------------------------

export interface SeatGuideText {
  /** The daily assertion, one sentence (SIGNER_PROTOCOL §2a–2c headings; §2e/§2f from SignerProtocol.java comments). */
  assertion: string;
  /** Closest TradFi analogue (tradfi intel §C). */
  analogue: string;
  /** An example value per evidence field, keyed by condition name. ILLUSTRATIVE — see ONBOARDING.md. */
  examples: Record<string, Record<string, string | number>>;
  /** What happens when a condition fails. */
  refuse: string[];
  /** Is this seat one the reference checker ships rules for today (signer-service/README.md)? */
  checkerShipped: boolean;
  /** A minimal signer.yml for this seat, in the README's config shape. */
  quickstart: string;
}

const CHECKER_HEAD = (seat: string) => `crossdesk:
  baseUrl: https://crossdesk-devnet-app.web.app
  apiKey: \${CROSSDESK_API_KEY}          # from Settings → API key (shown once)
  webhookSecret: \${CROSSDESK_WEBHOOK_SECRET}
  poll: { enabled: true, intervalSeconds: 30 }

server: { port: 8787, host: 0.0.0.0, webhookPath: /webhook }
state:  { file: ./signer-state.json }   # idempotency record

seat: ${seat}
instruments: [CBTC, cETH]              # the instruments your seat covers
`;

export const SEAT_GUIDE: Record<string, SeatGuideText> = {
  issuer: {
    assertion: 'Redemption integrity — the wrapper can actually be redeemed right now.',
    analogue: 'A benchmark contributor under a code of conduct (BMR/IOSCO Principle 14); an LBMA direct participant with the ability to settle.',
    examples: {
      'attestor-quorum': { quorumSigners: 8, quorumThreshold: 7 },
      'reserves-current': { reservesAsOf: '2026-09-24T06:00:00Z' },
      'reserves-cover-supply': { reserves: 1250.5, supply: 1248.0 },
      'redemption-queue-clear': { queueDepth: 0, maxQueueDepth: 0 },
    },
    refuse: [
      'If any condition fails you do not sign at par: refuse, naming the condition that failed.',
      'The administrator either restrikes with a factor below par and a rationale naming that condition, or no fixing is struck for the day.',
    ],
    checkerShipped: true,
    quickstart: `${CHECKER_HEAD('issuer')}
tolerances:
  reservesMaxAgeHours: 24
  maxQueueDepth: 0

conditions:
  attestor-quorum:
    quorumSigners:   { kind: http, url: https://attest.internal/status, bearer: \${ATTEST_TOKEN}, pointer: /online }
    quorumThreshold: { kind: http, url: https://attest.internal/status, bearer: \${ATTEST_TOKEN}, pointer: /threshold }
  reserves-current:
    reservesAsOf:    { kind: command, command: "psql -At -c \\"select max(attested_at) from por\\"", parse: text }
  reserves-cover-supply:
    reserves: { kind: http, url: https://attest.internal/por/latest, pointer: /reserves }
    supply:   { kind: http, url: https://attest.internal/por/latest, pointer: /supply }
  redemption-queue-clear:
    queueDepth: { kind: http, url: https://redeem.internal/queue, pointer: /unfilled }
`,
  },
  lender: {
    assertion: 'The mark is safe to lend against — you will carry this number on your own book.',
    analogue: 'A financial-intermediary / auction-participant member of a price oversight committee (IBA PMOC); an LBMA direct participant with bilateral credit lines.',
    examples: {
      'independent-mark-within-tolerance': { independentMark: 65012 },
      'liquidations-consistent': { liquidationsToday: 0, worstDeviationBps: 0 },
      'book-acceptance': { acceptedAt: '2026-09-24T16:00:12Z' },
    },
    refuse: [
      'If your own mark is outside your declared tolerance, or a liquidation cleared away from the proposal, refuse naming that condition — the refusal says exactly what broke.',
      'book-acceptance is the one that carries the weight: a lender that signs it and then marks its book elsewhere has made a false statement, on-ledger, with its own signature on it.',
    ],
    checkerShipped: true,
    quickstart: `${CHECKER_HEAD('lender')}
tolerances:                            # declared once, never widened at runtime
  markToleranceBps: 25
  liquidationToleranceBps: 100
  bookAcceptanceMaxAgeMinutes: 60

conditions:
  independent-mark-within-tolerance:
    independentMark: { kind: http, url: "https://risk.internal/marks/{instrument}", bearer: \${RISK_TOKEN}, pointer: /mark }
  liquidations-consistent:
    liquidationsToday: { kind: http, url: "https://risk.internal/liquidations/{instrument}/today", pointer: /count }
    worstDeviationBps: { kind: http, url: "https://risk.internal/liquidations/{instrument}/today", pointer: /worstBps }
  book-acceptance:
    acceptedAt: { kind: command, command: "riskctl accept-mark {instrument} {price} --json", pointer: /acceptedAt }
`,
  },
  venue: {
    assertion: 'The mark sits where the asset traded — inside the range your own book printed in the window.',
    analogue: 'A constituent exchange under CF Benchmarks’ Constituent Exchange Criteria; a regulated electronic trading venue at the first tier of ICE Swap Rate’s waterfall.',
    examples: {
      'traded-range': { low: 64935, high: 65078 },
      'spread-within-tolerance': { bid: 64967.5, ask: 65032.5, spreadBps: 10 },
      'sufficient-volume': { volume: 12.5 },
      'no-prints-attested': { bestBid: 64950, bestAsk: 65050 },
    },
    refuse: [
      'If the proposal is outside the range your book traded, refuse naming traded-range — the ledger would refuse the confirmation anyway.',
      'If your book showed no trades in the window, do not refuse and do not stay silent: attest the absence with no-prints-attested and your best bid/ask at the strike. It is a third act, and the band widens to carry it.',
      'You may never claim both traded-range and no-prints-attested on the same proposal.',
    ],
    checkerShipped: true,
    quickstart: `${CHECKER_HEAD('venue')}
tolerances:
  maxSpreadBps: 50
  minVolume: 0

conditions:
  traded-range:
    prints: { kind: http, url: "https://book.internal/trades?symbol={instrument}&window=strike", bearer: \${BOOK_TOKEN}, pointer: /trades }
  spread-within-tolerance:
    bid: { kind: http, url: "https://book.internal/quote?symbol={instrument}", pointer: /bid }
    ask: { kind: http, url: "https://book.internal/quote?symbol={instrument}", pointer: /ask }
  sufficient-volume:
    volume: { kind: http, url: "https://book.internal/trades?symbol={instrument}&window=strike", pointer: /volume }
`,
  },
  custodian: {
    assertion: 'What is actually in the account — the holdings are current, cover the issued supply, and are unencumbered.',
    analogue: 'An LBMA direct participant with the ability to settle and clear; the custodian and securities-lending communities FTSE Russell draws its committees from.',
    examples: {
      'holdings-current': { statementAsOf: '2026-09-24T05:30:00Z' },
      'holdings-cover-supply': { holdings: 100000, supply: 100000 },
      'no-encumbrance': { encumbered: 0 },
    },
    refuse: [
      'A stale statement, holdings below supply, or any pledged or lent units: refuse naming the condition.',
      'A missed statement moves the fixing to EXCEPTIONAL, then NO FIXING (chain-event policy §2.1). Notify any lien or rehypothecation immediately.',
    ],
    checkerShipped: false,
    quickstart: `${CHECKER_HEAD('custodian')}
# The reference checker ships seat rules for issuer, lender and venue today. A custodian
# checker sends the same body — { checks: [names], evidence: { "<condition>": { field: value } } } —
# to POST /api/proposals/{cid}/confirm with the fields the panel lists.
conditions:
  holdings-current:
    statementAsOf: { kind: http, url: "https://custody.internal/statements/latest?asset={instrument}", pointer: /asOf }
  holdings-cover-supply:
    holdings: { kind: http, url: "https://custody.internal/statements/latest?asset={instrument}", pointer: /units }
    supply:   { kind: http, url: "https://registry.internal/supply/{instrument}", pointer: /issued }
  no-encumbrance:
    encumbered: { kind: http, url: "https://custody.internal/statements/latest?asset={instrument}", pointer: /encumbered }
`,
  },
  'transfer-agent': {
    assertion: 'The share register — shares outstanding match the ledger at the strike, and the fees deducted are the ones the schedule requires.',
    analogue: 'A calculation or dissemination agent overseen under a benchmark oversight committee’s terms of reference (WMR / IBA PMOC); an outsourced service FTSE Russell’s oversight covers.',
    examples: {
      'shares-outstanding-reconciled': { registerShares: 1000000, ledgerShares: 1000000 },
      'fees-accrued': { accruedFees: 412.5 },
    },
    refuse: [
      'A register that does not reconcile to the ledger: refuse naming shares-outstanding-reconciled and report the break the same day.',
      'An unreconciled register is a class-F NO FIXING (rulebook §5.8).',
    ],
    checkerShipped: false,
    quickstart: `${CHECKER_HEAD('transfer-agent')}
# The reference checker ships seat rules for issuer, lender and venue today. A transfer-agent
# checker sends the same body — { checks: [names], evidence: { "<condition>": { field: value } } } —
# to POST /api/proposals/{cid}/confirm with the fields the panel lists.
conditions:
  shares-outstanding-reconciled:
    registerShares: { kind: command, command: "ta-register shares {instrument} --json", pointer: /outstanding }
    ledgerShares:   { kind: http, url: "https://etpfoundry.internal/ledger/shares/{instrument}", pointer: /outstanding }
  fees-accrued:
    accruedFees: { kind: command, command: "ta-register fees {instrument} --json", pointer: /accrued }
`,
  },
  operator: {
    assertion: 'The proposal itself — every input is disclosed with it and the composition reconciles to the ledger.',
    analogue: 'None. The administrator proposes and publishes and, since package 3.0.0, cannot be a committee member; this seat exists only to make a pilot exception visible.',
    examples: {},
    refuse: [
      'This seat is tolerated at the very start of a pilot and must be exited as soon as a fourth party exists.',
    ],
    checkerShipped: false,
    quickstart: '# The operator seat is not automated: it is a disclosed pilot exception, not a checker.\n',
  },
};

/** Prose for a seat, or a neutral fallback for a key the guide does not know. */
export function seatGuideText(seatKey: string | undefined | null): SeatGuideText | null {
  if (!seatKey) return null;
  return SEAT_GUIDE[seatKey.trim().toLowerCase()] ?? null;
}

// ---- the trust ladder (SIGNER_PROTOCOL §4a; rulebook §6.7; issuer pack §5) --------------

export interface TrustLevel {
  level: 'L1' | 'L2' | 'L3';
  name: string;
  key: string;
  forgeable: string;
  cost: string;
  /** What this level requires from the signer. */
  requires: string;
}

export const TRUST_LADDER: TrustLevel[] = [
  {
    level: 'L1',
    name: 'Hosted party',
    key: 'On the administrator’s participant; the API key authorises and the administrator exercises the choice as your party.',
    forgeable: 'Yes, technically',
    cost: 'Zero',
    requires: 'Your e-mail on the roster (e-mail-verified sign-in). Nothing to run.',
  },
  {
    level: 'L2',
    name: 'External signing',
    key: 'Held by you, off-ledger; the administrator submits a transaction you have already signed.',
    forgeable: 'No',
    cost: 'Moderate',
    requires: 'An API key and a checker on your infrastructure (the reference signer-service or your own), signing off-ledger.',
  },
  {
    level: 'L3',
    name: 'Own participant',
    key: 'On your own Canton participant.',
    forgeable: 'No',
    cost: 'High — you run Canton',
    requires: 'Your own participant party id on the roster; the choice is exercised from your node.',
  },
];

/**
 * The level a seat signs at today. `/api/me` carries no trust level yet, and every seat
 * issued so far is a hosted party, so this is L1 unless the roster says otherwise.
 */
export const DEFAULT_TRUST_LEVEL: TrustLevel['level'] = 'L1';

/** The confirm body an automated signer sends — README "The conditions and their evidence, per seat". */
export function exampleConfirmBody(role: GuideRole, text: SeatGuideText | null): string {
  const checks = role.conditions
    .filter((c) => c.name !== 'no-prints-attested') // the thin-market alternative, not the default claim
    .map((c) => c.name);
  const evidence: Record<string, Record<string, string | number>> = {};
  for (const name of checks) {
    const ex = text?.examples[name];
    if (ex && Object.keys(ex).length) evidence[name] = ex;
  }
  const body: Record<string, unknown> = { checks, evidence };
  // For the venue, low/high are also present at the top level of `evidence`: that is the
  // range the ledger enforces.
  if (role.requiresObservedRange && evidence['traded-range']) {
    (body.evidence as Record<string, unknown>).low = evidence['traded-range'].low;
    (body.evidence as Record<string, unknown>).high = evidence['traded-range'].high;
  }
  return JSON.stringify(body, null, 2);
}
