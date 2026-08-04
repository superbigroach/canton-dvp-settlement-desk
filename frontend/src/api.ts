// Typed client for the Canton DvP Settlement Desk REST API (Spring Boot :8080,
// reached through the Vite proxy at /api). Every DTO here mirrors the backend's
// `Dtos.java` / `LedgerService` view records so the UI stays honest about the wire.

// ---- DTOs -----------------------------------------------------------------

export interface Party {
  party: string;        // full on-ledger id, e.g. "Alice::1220ab…"
  displayName: string;
  label: string;        // friendly hint, e.g. "Alice"
  isLocal: boolean;
}

export interface Holding {
  contractId: string;
  issuer: string;
  instrumentId: string; // "DEMO:AAPL" | "USDC" | "cETH"
  owner: string;
  amount: number;
  disclosedTo: string[];
}

export interface Instrument {
  id: string;
  kind: string;                 // Equity | Cash | CryptoWrapped
  description: string;
  referencePrice: number | null; // the official close price (null for cash)
}

export interface TradeRequest {
  buyer: string;
  seller: string;
  assetInstrument: string;
  assetAmount: number;
  cashInstrument: string;
  cashAmount: number;
}

export interface TradeResponse {
  receiptCid: string | null;
  buyer: string;
  seller: string;
  assetInstrument: string;
  assetAmount: number;
  cashInstrument: string;
  cashAmount: number;
  unitPrice: number;
}

export type Session = 'Open' | 'Close';

// Market = unpriced market-on-close; Limit = limit-on-close at a stated price.
export type OrderType = 'Market' | 'Limit';

export interface MocOrderRequest {
  trader: string;
  side: 'Buy' | 'Sell';
  quantity: number;
  instrumentId: string;
  cashInstrument?: string; // defaults to USDC server-side
  session?: Session;       // Open (MOO) | Close (MOC); defaults to Close server-side
  // The order TYPE. Omit it and the server infers: a stated limitPrice is a Limit
  // order, and an ABSENT limit is a MARKET order (never a rejection).
  orderType?: OrderType;
  // Worst price this order accepts (Buy: max, Sell: min) — LIMIT ORDERS ONLY. Omit
  // it (or send orderType:'Market') for an unpriced market-on-close order, which
  // takes whatever the book prints and is allocated ahead of every limit order.
  // Limits AWAY from the anchor are what let the uncross discover a different print.
  //
  // Buying power for a BUY is quantity * the worst price the order can execute at:
  // that is the LIMIT for a limit order, and the TOP OF THE VENUE'S PRICE COLLAR
  // for a market order (max(anchor * 1.10, anchor + 0.50)) — never quantity * anchor.
  limitPrice?: number;
}

export interface MocOrderResponse {
  orderCid: string;
  // The auction contract id AFTER the submission. SubmitOrder is consuming — it
  // archives the auction and re-creates it with the book count incremented — so
  // the cid the order was sent to is already dead.
  auctionCid: string;
  openedAuction: boolean;
  // The auction's published ANCHOR (and this order's limit when none was given).
  // NOT where the cross will print: that is discovered from the book at the close.
  closingPrice: number;
}

export interface MocOrderView {
  contractId: string;
  trader: string;
  side: 'Buy' | 'Sell';
  quantity: number;
  // NULL for an unpriced market-on-close order. That is the order TYPE showing
  // through, not missing data — render it as "MOC", never as blank, 0, or the anchor.
  limitPrice: number | null;
  orderType: 'MOC' | 'LOC';
}

export interface MocState {
  auctionCid: string | null;
  instrumentId: string;
  cashInstrument: string;
  session: string;              // "Open" | "Close"
  referencePrice: number | null; // the venue's published ANCHOR, not the print
  isOpen: boolean;
  orders: MocOrderView[];       // filtered by the ledger to the acting party's view
  othersResting: number;        // OTHER sealed orders hidden from a trader (0 for venue)
}

export interface ClearBookResponse {
  cleared: number;
}

// The NET imbalance of a sealed book — the MANDATED LIQUIDITY PROVIDER view.
// `disclosed` is true ONLY when the acting party holds a LIVE LiquidityMandate over
// this book (or is the venue); anyone else gets disclosed=false with HTTP 403. It
// carries side + magnitude ONLY — never any individual order or trader identity.
//
// `mandateRequired` is the interesting one: it means the caller was refused because
// it has not TAKEN THE SEAT, not because of who it is. The panel turns that into an
// Accept action rather than a dead end — the privilege is contestable, and that is
// the whole point of the redesign.
export interface MocImbalance {
  disclosed: boolean;
  instrumentId: string;
  cashInstrument: string;
  session: string;                       // "Open" | "Close"
  netSide: 'Buy' | 'Sell' | 'Flat' | null;
  netQuantity: number | null;            // magnitude of the imbalance (>= 0)
  referencePrice: number | null;
  liquidityProvider: string | null;      // label of the provider it was disclosed to
  mandateRequired: boolean;              // true = no live mandate; accept terms to see it
  note: string | null;
}

// The venue's OPEN OFFER of the liquidity seat for one book. Posted, observable by
// every eligible participant, and takeable by any of them — several providers may
// hold live mandates over the same book at once and all see the same number.
export interface MandateTerms {
  contractId: string;
  instrumentId: string;
  cashInstrument: string;
  session: string;                       // "Open" | "Close"
  anchorPrice: number;                   // the published anchor the band is measured from
  commitmentSize: number;                // units of imbalance a provider undertakes to absorb
  maxBandBps: number;                    // how far from the anchor it undertakes to stand
  expiresAt: string;                     // ISO-8601
  eligible: string[];                    // labels: the venue's registered participants
  accepted: string[];                    // labels: who already holds a mandate off these terms
  barred: string[];                      // labels: who missed a commitment this session
  openToActingParty: boolean;
  note: string | null;
}

// The acting party's OWN signed obligation over a book. `held=false` means no
// mandate — and therefore no imbalance. `expired=true` distinguishes "you never took
// the seat" from "your seat ran out", which are different problems.
export interface LiquidityMandate {
  held: boolean;
  contractId: string | null;
  provider: string;                      // label
  instrumentId: string;
  cashInstrument: string;
  session: string;
  anchorPrice: number | null;
  commitmentSize: number | null;
  maxBandBps: number;
  expiresAt: string | null;
  shownSide: 'Buy' | 'Sell' | 'Flat' | null;  // side of the PEAK imbalance shown
  peakShownQty: number | null;           // the largest imbalance this mandate has seen
  disclosuresSeen: number;
  expired: boolean;
  note: string | null;
}

export interface MocFill {
  trader: string;
  side: 'Buy' | 'Sell';
  quantity: number;
  price: number;
}

export interface MocCloseResponse {
  settlementBatchCid: string;
  session: string;              // "Open" | "Close"
  // The DISCOVERED uniform price: the volume-maximising uncross of the sealed
  // book. May legitimately print above or below the auction's anchor.
  closingPrice: number;
  fills: MocFill[];
}

// ---- Decentralised operator: K-of-N committee-attested NAV ----------------

export interface CidResponse {
  contractId: string;
}

// ---- Continuous accrual: the committee attests, the ledger computes -------
//
// EVERY DECIMAL BELOW IS A STRING, AND THAT IS THE POINT. Daml `Decimal` is
// fixed-point at exactly ten decimal places; a JSON number would arrive here as a
// float64, which is a DIFFERENT value. The backend serialises the ledger's own digits
// and `accrual.ts` re-parses them into exact fixed-point integers, so the figure that
// ticks on screen is the number the ledger would compute rather than one near it.

/** "ACT/360" (USD money market) | "ACT/365F" (GBP/AUD/NZD/HKD/SGD) | "NONE" (snapshot). */
export type DayCountConvention = 'ACT/360' | 'ACT/365F' | 'NONE';

/** A proposal echoed back with the RECIPE each member is being asked to confirm. */
export interface FixingProposalResponse {
  contractId: string;
  instrumentId: string;
  cashInstrument: string;
  session: Session;
  basePrice: string;
  ratePerAnnum: string;
  dayCount: DayCountConvention;
  accrualFrom: string;               // ISO-8601 — ATTESTED, not the ledger clock
  accrualFromEpochMicros: number;
  accruing: boolean;
}

/** An official NavFixing with its accrual recipe, plus the value now. */
export interface FixingResponse {
  contractId: string;
  attestors: string[];               // the signature set IS the proof of K-of-N
  threshold: number;
  instrumentId: string;
  cashInstrument: string;
  session: Session;
  basePrice: string;                 // the ATTESTED base, as at accrualFrom
  rationale: string;
  ratePerAnnum: string;
  dayCount: DayCountConvention;
  accrualFrom: string;
  accrualFromEpochMicros: number;
  finalizedAt: string;               // when the LEDGER saw it — a different fact
  publishedTo: string[];
  accruing: boolean;
  navNow: string;
  accrued: string;
  asOf: string;
  asOfEpochMicros: number;
  elapsedMicros: number;
}

/**
 * THE ACCRUED NAV WITH ITS WORKING SHOWN — the derived value and every input it came
 * from, so the browser can REPRODUCE it rather than trust it.
 *
 * The `anchor…` fields bind it to the auction: RunClose requires the anchor to be at
 * or below the NAV accrued to the close and no more than 1bp behind, so
 * `anchorConsistent` is the ledger's own verdict rather than an assertion. `anchor` is
 * null when no live auction for this instrument/session is visible.
 */
export interface AccruedNav {
  contractId: string;
  instrumentId: string;
  cashInstrument: string;
  session: Session;
  // the four attested inputs
  basePrice: string;
  ratePerAnnum: string;
  dayCount: DayCountConvention;
  accrualFrom: string;
  accrualFromEpochMicros: number;
  // the derivation
  asOf: string;
  asOfEpochMicros: number;
  elapsedMicros: number;
  yearMicros: number;
  accrued: string;
  navNow: string;
  perDay: string;                    // what a whole day of this recipe adds
  accruing: boolean;
  // the attestation
  attestors: string[];
  threshold: number;
  // the auction binding
  anchor: string | null;
  anchorAuctionCid: string | null;
  anchorDrift: string | null;        // navNow - anchor (positive = the anchor is behind)
  staleBudget: string | null;        // navNow * 1bp
  anchorConsistent: boolean | null;
  anchorNote: string | null;
}

// ---- Basket / ETF builder -------------------------------------------------

export interface BasketComponent {
  instrumentId: string;  // an underlying, e.g. "cETH"
  unitsPerShare: number; // units of it per basket share
}

export interface Basket {
  basketCid: string;
  basketId: string;      // the basket token symbol, e.g. "LX1"
  administrator: string; // fund administrator / custodian
  cashInstrument: string;
  components: BasketComponent[];
  participants: string[];
}

export interface BasketCreateResponse {
  receiptCid: string | null;
  mintedSharesCid: string | null;
  shares: number;
  navPerShare: number | null;
}

export interface BasketRedeemResponse {
  receiptCid: string | null;
  shares: number;
  returnedHoldingCids: string[];
}

export interface NavLeg {
  instrumentId: string;
  unitsPerShare: number;
  price: number | null;  // the underlying's official close mark
  value: number | null;  // unitsPerShare × price
}

export interface NavResponse {
  basketId: string;
  navPerShare: number | null; // Σ value; null if any mark is missing
  cashInstrument: string;
  legs: NavLeg[];
  complete: boolean;
}

// A receipt as the acting party sees it, with WHO can see it. Queried as the acting
// party, so the ledger's need-to-know rules decide what comes back (an outsider gets []).
export interface LedgerReceipt {
  contractId: string;
  kind: string;         // "DvP" | "Auction fill" | "Creation" | "Redemption"
  headline: string;
  settledAt: string;
  visibleTo: string[];  // labels of every party entitled to see this receipt
}

// ---- transport ------------------------------------------------------------

/** The backend surfaces its errors as {message}. Unwrap it for a clean UI toast. */
export class ApiError extends Error {}

async function req<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const msg = (body && (body.message || body.error)) || `HTTP ${res.status}`;
    throw new ApiError(msg);
  }
  return body as T;
}

// ---- endpoints ------------------------------------------------------------

export const api = {
  parties: () => req<Party[]>('/parties'),
  instruments: () => req<Instrument[]>('/instruments'),
  holdings: (party: string) =>
    req<Holding[]>(`/holdings?party=${encodeURIComponent(party)}`),

  // One-click bilateral DvP: propose → accept → settle, server-orchestrated.
  trade: (body: TradeRequest) =>
    req<TradeResponse>('/trade', { method: 'POST', body: JSON.stringify(body) }),

  // Market-on-Close: lodge a sealed order (no price), inspect the book, run the close.
  mocOrder: (body: MocOrderRequest) =>
    req<MocOrderResponse>('/moc/order', { method: 'POST', body: JSON.stringify(body) }),
  // The book is filtered server-side BY THE ACTING PARTY (the dark-pool property):
  // a trader sees only their own resting orders; the venue sees the full book.
  mocState: (
    instrumentId: string,
    session: Session = 'Close',
    actingAs = '',
    cashInstrument = 'USDC',
  ) =>
    req<MocState>(
      `/moc/state?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
        `&session=${encodeURIComponent(session)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    ),
  mocClose: (auctionCid: string) =>
    req<MocCloseResponse>(`/moc/${encodeURIComponent(auctionCid)}/close`, {
      method: 'POST',
    }),

  // Withdraw a resting order (trader pulls their own; reserved backing unlocked).
  withdrawOrder: (orderCid: string, trader: string) =>
    req<{ contractId: string }>(
      `/moc/order/${encodeURIComponent(orderCid)}/withdraw`,
      { method: 'POST', body: JSON.stringify({ trader }) },
    ),

  // Venue clears the whole resting book for an instrument/session.
  clearBook: (instrumentId: string, session: Session = 'Close', cashInstrument = 'USDC') =>
    req<ClearBookResponse>('/moc/clear', {
      method: 'POST',
      body: JSON.stringify({ instrumentId, cashInstrument, session }),
    }),

  // Mandated-provider view: the NET imbalance, disclosed BY THE LEDGER only to a
  // party holding a live LiquidityMandate over this book (and to the venue).
  //
  // A caller without one is denied — 403 for a participant that has not taken the
  // seat, 409 for the venue when NOBODY has taken it. Both carry a
  // `disclosed:false, mandateRequired:true` body, and both are returned rather than
  // thrown: "you need a mandate" is an ANSWER the panel renders as an Accept action,
  // not an error. Only a genuinely broken call throws.
  imbalance: async (
    instrumentId: string,
    session: Session = 'Close',
    actingAs = '',
    cashInstrument = 'USDC',
  ): Promise<MocImbalance> => {
    const res = await fetch(
      `/api/moc/imbalance?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
        `&session=${encodeURIComponent(session)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    );
    const text = await res.text();
    const body = text ? JSON.parse(text) : null;
    if (res.status === 403 || res.status === 409) return body as MocImbalance;
    if (!res.ok) throw new ApiError((body && (body.message || body.error)) || `HTTP ${res.status}`);
    return body as MocImbalance;
  },

  // ---- The contestable liquidity mandate ----------------------------------
  // The seat that buys sight of the residual is POSTED, not awarded: read the open
  // terms, accept them, and the same number the incumbent sees is yours.

  /** The venue's open offers of the liquidity seat for a book, as this party sees them. */
  mandateTerms: (
    instrumentId: string,
    session: Session = 'Close',
    actingAs = '',
    cashInstrument = 'USDC',
  ) =>
    req<MandateTerms[]>(
      `/moc/mandate/terms?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
        `&session=${encodeURIComponent(session)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    ),

  /**
   * Take up the seat — the ACTING PARTY becomes an obligated liquidity provider.
   *
   * Acceptance is blind: nothing about the book is visible until the mandate exists,
   * so you commit before you can see. That ordering is the property, not a limitation.
   */
  acceptMandate: (body: {
    provider: string;
    instrumentId: string;
    cashInstrument?: string;
    session?: Session;
    termsCid?: string;
  }) =>
    req<{ contractId: string }>('/moc/mandate/accept', {
      method: 'POST',
      body: JSON.stringify({ cashInstrument: 'USDC', session: 'Close', ...body }),
    }),

  /** The acting party's own live obligation over a book (size, band, expiry). */
  myMandate: (
    instrumentId: string,
    session: Session = 'Close',
    actingAs = '',
    cashInstrument = 'USDC',
  ) =>
    req<LiquidityMandate>(
      `/moc/mandate?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
        `&session=${encodeURIComponent(session)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    ),

  // ---- Decentralised operator: committee-attested NAV ----------------------
  // Stand up a K-of-N committee, propose a fix, gather member confirmations, then
  // finalise into an official NavFixing that no single party could have produced.
  createCommittee: (body: {
    admin: string;
    members: string[];
    threshold: number;
    auditor?: string;
    label?: string;
  }) => req<CidResponse>('/committee', { method: 'POST', body: JSON.stringify(body) }),

  proposeFixing: (
    committeeCid: string,
    body: {
      proposer: string;
      instrumentId: string;
      price: number;
      cashInstrument?: string;
      session?: Session;
      rationale?: string;
    },
  ) =>
    req<CidResponse>(`/committee/${encodeURIComponent(committeeCid)}/propose`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  /**
   * Propose an ACCRUING fix — attest the INPUTS to a value that keeps moving.
   *
   * A SEPARATE CALL from `proposeFixing`, exactly as `ProposeAccruingFixing` is a
   * separate Daml choice: the snapshot path is untouched and still produces a
   * non-accruing mark. Four things a human agrees, because everything else is
   * derivable — base, rate, day-count convention, and the instant the mark applies
   * from. An unsupported convention comes back as a 400 with the reason, because the
   * ledger refuses one rather than defaulting it and this desk refuses it first.
   */
  proposeAccruingFixing: (
    committeeCid: string,
    body: {
      proposer: string;
      instrumentId: string;
      price: number | string;          // the BASE NAV the accrual starts from
      ratePerAnnum: number | string;    // 0.036 = 3.6%/yr; may be negative
      dayCount: DayCountConvention;
      cashInstrument?: string;
      session?: Session;
      rationale?: string;
      accrualFrom?: string;             // ISO-8601; omitted = the desk's clock
    },
  ) =>
    req<FixingProposalResponse>(
      `/committee/${encodeURIComponent(committeeCid)}/propose-accruing`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  /** Official fixes visible to a party, each with its recipe and the value now. */
  fixings: (actingAs = '') =>
    req<FixingResponse[]>(
      `/fixings${actingAs ? `?actingAs=${encodeURIComponent(actingAs)}` : ''}`,
    ),

  /**
   * The accrued NAV for one fix, WITH ITS WORKING. Poll this rarely (it is a ledger
   * read); tick the number between polls with `accrual.ts`, which reproduces the
   * ledger's arithmetic exactly rather than approximating it.
   */
  accruedNav: (fixCid: string, opts: { actingAs?: string; at?: string; anchor?: string } = {}) =>
    req<AccruedNav>(
      `/fixing/${encodeURIComponent(fixCid)}/nav` +
        (opts.actingAs ? `?actingAs=${encodeURIComponent(opts.actingAs)}` : '?') +
        (opts.at ? `&at=${encodeURIComponent(opts.at)}` : '') +
        (opts.anchor ? `&anchor=${encodeURIComponent(opts.anchor)}` : ''),
    ),

  // Each confirmation returns the NEW proposal cid (accumulating multisig).
  confirmFixing: (proposalCid: string, member: string) =>
    req<CidResponse>(`/fixing/${encodeURIComponent(proposalCid)}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ member }),
    }),

  finalizeFixing: (proposalCid: string, proposer: string, publishTo: string[]) =>
    req<CidResponse>(`/fixing/${encodeURIComponent(proposalCid)}/finalize`, {
      method: 'POST',
      body: JSON.stringify({ proposer, publishTo }),
    }),

  // ---- Basket / ETF builder ------------------------------------------------
  baskets: (actingAs = '') =>
    req<Basket[]>(`/baskets${actingAs ? `?actingAs=${encodeURIComponent(actingAs)}` : ''}`),

  defineBasket: (body: {
    administrator: string;
    basketId: string;
    components: BasketComponent[];
    participants: string[];
    auditor?: string;
    description?: string;
    cashInstrument?: string;
  }) => req<Basket>('/basket', { method: 'POST', body: JSON.stringify(body) }),

  // One-click in-kind creation (deliver underlyings → mint shares, atomic).
  basketCreate: (body: { basketId: string; ap: string; shares: number }) =>
    req<BasketCreateResponse>('/basket/create', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  // One-click in-kind redemption (burn shares → receive underlyings, atomic).
  basketRedeem: (body: { basketId: string; ap: string; shares: number }) =>
    req<BasketRedeemResponse>('/basket/redeem', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  basketNav: (basketId: string, actingAs = '') =>
    req<NavResponse>(
      `/basket/nav?basketId=${encodeURIComponent(basketId)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    ),

  // Receipts VISIBLE to a party (need-to-know — the ledger filters, an outsider gets []).
  receiptsFor: (party: string) =>
    req<LedgerReceipt[]>(`/receipts?party=${encodeURIComponent(party)}`),
};
