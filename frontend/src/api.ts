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

// ---- CIP-56 token standard: assets issued by SOMEBODY ELSE'S registry ------
//
// A transfer from a foreign registry (BitSafe's CBTC faucet, say) does not arrive as
// a balance. It arrives as a PENDING TransferInstruction addressed to the receiver,
// and it is not a holding — and appears in no balance anywhere — until the receiver
// exercises the standard's `TransferInstruction_Accept`. That accept is the whole
// point of this panel: it is what turns a promise from another issuer into an asset
// this party actually owns.
//
// The backend finds these by querying the ACS BY INTERFACE
// (`Splice.Api.Token.TransferInstructionV1:TransferInstruction`), never by template —
// the contract is on the foreign registry's own template, in a package this project
// has never seen.

export interface PendingTransfer {
  instructionCid: string;
  /** inbound = this party is the RECEIVER (can accept); outbound = the SENDER (can withdraw). */
  direction: 'inbound' | 'outbound' | 'observed';
  /** The choice this party controls: the standard fixes who may do what. */
  action: 'accept' | 'withdraw';
  canAct: boolean;
  sender: string;
  receiver: string;
  instrumentId: string;
  /** The registry that administers the instrument — WHOSE asset this is. */
  instrumentAdmin: string;
  amount: number;
  /** "PendingReceiverAcceptance" means it is waiting on the receiver, i.e. on you. */
  status: string;
  requestedAt: string;
  executeBefore: string;
  /** True when THIS desk administers the instrument (a self-issued stand-in). */
  ourRegistry: boolean;
  /**
   * The configured off-ledger registry for a FOREIGN instrument, or null. Null is
   * the thing to fix if an accept is refused for want of a choice context.
   */
  registryUrl: string | null;
  expired: boolean;
}

export interface PendingTransfersResponse {
  party: string;
  direction: string;
  pending: PendingTransfer[];
  note?: string;
}

/**
 * The result of a standard choice. `created` lists what appeared on the ledger — for
 * an accept, that is the received holding, ON THE FOREIGN REGISTRY'S TEMPLATE, which
 * is the proof that the asset is not self-issued.
 *
 * `choiceContext.source` is deliberately reported: "accepted with an empty context"
 * and "accepted with BitSafe's context" are different claims and only one of them is
 * repeatable for a registry that requires one.
 */
export interface InstructionOutcome {
  choice: string;
  instructionCid: string;
  updateId: string;
  created: string[];
  choiceContext: { source: string; values: number; disclosedContracts: number };
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
//
// THE RULE HERE: whatever went wrong, the user reads a SENTENCE. Never `undefined`,
// never `[object Object]`, never a bare status number when the server took the trouble
// to explain itself, and never a stack trace. This is on a projector.
//
// The backend answers failures as {message, code?, hint?, commandId?, ...}. `message`
// is the sentence — for a Daml rejection it is the model's own words ("committed
// holding is the wrong instrument"), and for a refused authorization it is the one
// thing Canton lets us say plus the command id to quote to the node operator. The
// extra fields are carried on the error object for anyone who wants them, but the
// message alone is always enough to read out loud.

/** A failure the SERVER described. Carries the desk's diagnostic fields when present. */
export class ApiError extends Error {
  /** HTTP status, or 0 when the request never got a response at all. */
  readonly status: number;
  /** e.g. "PERMISSION_DENIED", "CONTRACT_NOT_FOUND", "DAML_INTERPRETATION_ERROR". */
  readonly code?: string;
  /** What the code means operationally and where to look next. */
  readonly hint?: string;
  /** The submission's command id — what a node operator greps their log for. */
  readonly commandId?: string;
  /** True when the LEDGER MODEL refused it: an expected outcome, not a fault. */
  readonly damlRejection: boolean;

  constructor(
    message: string,
    status = 0,
    extra: { code?: string; hint?: string; commandId?: string; damlRejection?: boolean } = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = extra.code;
    this.hint = extra.hint;
    this.commandId = extra.commandId;
    this.damlRejection = extra.damlRejection ?? false;
  }
}

/** A string, or '' — never 'undefined', 'null' or '[object Object]'. */
function asText(value: unknown): string {
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return '';
}

/**
 * The sentence to show for ANY caught value. Use this in every `catch` — it is the
 * single place that guarantees the three forbidden renderings can never reach the
 * screen, including for values that are not Errors at all.
 */
/** One leg valued twice: at the attested mark, and as of right now. */
export interface IndicativeNavLeg {
  instrumentId: string;
  unitsPerShare: number;
  officialPrice: number | null;
  officialValue: number | null;
  indicativePrice: number | null;
  indicativeValue: number | null;
  /** Where the current number came from: a named feed, an accrual recipe, or the mark. */
  basis: string;
}

/**
 * The two NAVs a fund actually has — and a real ETF runs both.
 *
 * `official` is struck from marks the committee signed; it is what create/redeem
 * legally settles at, and it moves only when the committee strikes again.
 * `indicative` is what the fund is worth right now, recomputed from current data and
 * binding on nobody — the on-chain equivalent of the iNAV an exchange disseminates
 * every ~15 seconds. `driftBps` is the gap, which is the honest measure of how stale
 * the official strike has become.
 */
export interface IndicativeNav {
  basketId: string;
  cashInstrument: string;
  officialNavPerShare: number | null;
  indicativeNavPerShare: number | null;
  driftBps: number | null;
  legs: IndicativeNavLeg[];
  complete: boolean;
  /** False when nothing could be revalued — every leg fell back to its mark. */
  live: boolean;
  asOf: string;
}

/**
 * A CANDIDATE mark from an outside feed — NOT an official price.
 *
 * The desk has no oracle: a price is official only once a committee has attested it,
 * and the signatures are what make it provable. This exists so a member proposes
 * today's real number instead of one typed from memory. The feed proposes; the
 * committee disposes.
 */
export interface LiveMark {
  instrumentId: string;
  symbol: string;
  price: number;
  source: string;
  asOf: string;
  /** The assumption being made — e.g. a wrapped token marked at its underlying's spot. */
  note: string;
}

// ---- THE CONTINUOUS SESSION (daml/ContinuousBook.daml) --------------------

export type BookSideName = 'Bid' | 'Ask';
/**
 * GTC rests; IOC fills what it can now and cancels the rest; FOK fills the WHOLE
 * order now or none of it.
 *
 * AON also refuses a partial fill but MAY REST, so FOK is "all of it, now, or never"
 * and AON is "all of it, whenever". All four are values of the ledger's own enum and
 * every rule is enforced in the choice body: a partial fill of a FOK or an AON aborts
 * the whole match, and a resting AON cannot be nibbled by a smaller aggressor either.
 */
export type TimeInForce = 'GTC' | 'IOC' | 'FOK' | 'AON';

export interface BookSessionRequest {
  instrumentId: string;
  cashInstrument?: string;
  /** Defaults to the instrument's published mark. */
  referencePrice?: number | null;
  /** Half-width of the price band as a fraction. Defaults to 0.10 (±10%). */
  bandFraction?: number | null;
}

export interface BookOrderRequest {
  trader: string;
  side: BookSideName;
  quantity: number;
  instrumentId: string;
  cashInstrument?: string;
  /**
   * ABSENT = an unpriced MARKET order: it takes whatever the book shows and its
   * remainder is killed. It may never rest — the ledger refuses it outright,
   * because a resting market order is a free option to whoever next crosses it.
   */
  limitPrice?: number | null;
  timeInForce?: TimeInForce | null;
}

/** One fill, at the MAKER's posted price. The contra is never named. */
export interface BookFill {
  price: number;
  quantity: number;
  cashAmount: number;
  buyer: string;
  seller: string;
  aggressor: string;
  maker: string;
}

export interface BookOrderResponse {
  /** null when the order filled completely. */
  orderCid: string | null;
  /** The SUCCESSOR book — every choice on the book is consuming. */
  bookCid: string;
  openedNewBook: boolean;
  referencePrice: number;
  fills: BookFill[];
  filledQuantity: number;
  restingQuantity: number;
}

export interface BookOrderView {
  contractId: string;
  trader: string;
  side: BookSideName;
  quantity: number;
  /** null would mean an unpriced order, which cannot rest — so always a number here. */
  limitPrice: number | null;
  timeInForce: TimeInForce;
  seqNo: number;
}

export interface BookState {
  bookCid: string;
  instrumentId: string;
  cashInstrument: string;
  referencePrice: number;
  bandLow: number;
  bandHigh: number;
  isOpen: boolean;
  liveCount: number;
  nextSeq: number;
  bids: BookOrderView[];
  asks: BookOrderView[];
  bestBid: number | null;
  bestAsk: number | null;
}

/** A public print. Note there is no trader field — that is the design, not an omission. */
export interface BookTape {
  contractId: string;
  instrumentId: string;
  cashInstrument: string;
  price: number;
  quantity: number;
  printedAt: string;
  matchSeq: number;
}

export interface BookConfirm {
  contractId: string;
  trader: string;
  instrumentId: string;
  cashInstrument: string;
  side: BookSideName;
  quantity: number;
  price: number;
  cashAmount: number;
  /** 'Maker' | 'Taker' — the field every fee schedule in the industry is built on. */
  liquidity: string;
  tradedAt: string;
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message;
  if (e instanceof Error) return asText(e.message) || e.name || 'the request failed';
  return asText(e) || 'the request failed';
}

/** Parse, or null. A malformed body must not turn into a JSON syntax error on screen. */
function parseJson(text: string): unknown {
  if (!text) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

/** A readable sentence for a status the server did not explain itself. */
function statusSentence(status: number, path: string): string {
  if (status === 0) return `no response from the settlement desk (${path})`;
  if (status === 404) return `the settlement desk has no ${path}`;
  if (status === 502 || status === 503 || status === 504) {
    return 'the settlement desk or its Canton participant is not responding — check GET /api/diag';
  }
  return `the settlement desk returned HTTP ${status} for ${path}`;
}

/** Build the error for a non-OK response, preferring the server's own words. */
function errorFrom(res: Response, body: unknown, rawText: string, path: string): ApiError {
  const b = body && typeof body === 'object' ? (body as Record<string, unknown>) : null;
  const message = b ? asText(b.message) : '';
  const error = b ? asText(b.error) : '';
  // A non-JSON body (a proxy's HTML page) is not worth rendering; a plain-text one is.
  const plain = !rawText.includes('<') ? asText(rawText).slice(0, 300) : '';
  return new ApiError(
    message || error || plain || statusSentence(res.status, path),
    res.status,
    {
      code: b ? asText(b.code) || undefined : undefined,
      hint: b ? asText(b.hint) || undefined : undefined,
      commandId: b ? asText(b.commandId) || undefined : undefined,
      damlRejection: b ? b.damlRejection === true : false,
    },
  );
}

/** GET/POST the desk. Rejects ONLY with an {@link ApiError} carrying a real sentence. */
async function req<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`/api${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    });
  } catch (e) {
    // fetch only rejects for network-level failures, and its own message ("Failed to
    // fetch") says nothing about WHAT was unreachable. Say what was being called.
    throw new ApiError(
      `cannot reach the settlement desk at /api${path} — ${errorMessage(e)}`,
      0,
    );
  }
  const text = await res.text();
  const body = parseJson(text);
  if (!res.ok) throw errorFrom(res, body, text, path);
  if (text && body === null) {
    throw new ApiError(
      `the settlement desk returned a non-JSON response for ${path}`,
      res.status,
    );
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
    const path =
      `/moc/imbalance?instrumentId=${encodeURIComponent(instrumentId)}` +
      `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
      `&session=${encodeURIComponent(session)}` +
      (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : '');
    let res: Response;
    try {
      res = await fetch(`/api${path}`);
    } catch (e) {
      throw new ApiError(
        `cannot reach the settlement desk at /api${path} — ${errorMessage(e)}`,
        0,
      );
    }
    const text = await res.text();
    const body = parseJson(text);
    // 403/409 are ANSWERS ("you hold no mandate" / "nobody holds one"), not errors —
    // but only when the body is actually the shaped answer. A 403 from something other
    // than the mandate gate (a proxy, say) must still surface as a readable error.
    if ((res.status === 403 || res.status === 409) && body && typeof body === 'object'
        && 'mandateRequired' in (body as Record<string, unknown>)) {
      return body as MocImbalance;
    }
    if (!res.ok) throw errorFrom(res, body, text, path);
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

  // ---- CIP-56 token standard: claim assets from another registry -----------
  //
  // `party` may be a label the desk knows OR a full `name::namespace` party id — the
  // backend accepts a fully-qualified id verbatim, which matters here precisely
  // because a foreign transfer is addressed to a party id somebody else typed.

  /** Live TransferInstructions this party is a stakeholder of, whoever issued them. */
  pendingTransfers: (party: string, direction: 'inbound' | 'outbound' | 'all' = 'inbound') =>
    req<PendingTransfersResponse>(
      `/token-standard/pending?party=${encodeURIComponent(party)}` +
        `&direction=${encodeURIComponent(direction)}`,
    ),

  /** Claim an inbound transfer — the receiver exercises TransferInstruction_Accept. */
  acceptTransfer: (party: string, instructionCid: string) =>
    req<InstructionOutcome>('/token-standard/accept', {
      method: 'POST',
      body: JSON.stringify({ party, instructionCid }),
    }),

  /** Decline an inbound transfer — the funds return to the sender. */
  rejectTransfer: (party: string, instructionCid: string) =>
    req<InstructionOutcome>('/token-standard/reject', {
      method: 'POST',
      body: JSON.stringify({ party, instructionCid }),
    }),

  /** Pull back an outbound transfer the receiver has not accepted (sender only). */
  withdrawTransfer: (party: string, instructionCid: string) =>
    req<InstructionOutcome>('/token-standard/withdraw', {
      method: 'POST',
      body: JSON.stringify({ party, instructionCid }),
    }),

  /**
   * Candidate marks from an outside feed, for pre-filling a proposal. Writes nothing
   * to the ledger. An empty list means the feed is unreachable — type the mark in.
   */
  liveMarks: () => req<LiveMark[]>('/marks/live'),

  /**
   * Official vs indicative NAV, side by side. Poll this — the indicative side is
   * meant to move, and the drift between the two is the point.
   */
  basketIndicativeNav: (basketId: string, actingAs = '') =>
    req<IndicativeNav>(
      `/basket/nav/indicative?basketId=${encodeURIComponent(basketId)}` +
        (actingAs ? `&actingAs=${encodeURIComponent(actingAs)}` : ''),
    ),

  // ---- THE CONTINUOUS SESSION ------------------------------------------
  // The auction's counterpart: limit interest that RESTS and is matched by
  // price then time. The ladder is filtered server-side BY THE ACTING PARTY,
  // exactly like the sealed book — and here even the auditor sees nothing,
  // because a RestingOrder has no observers at all.

  /** Open a session for an instrument, or return the one already open. */
  openBookSession: (body: BookSessionRequest) =>
    req<BookState>('/book/session', { method: 'POST', body: JSON.stringify(body) }),

  /** Place an order — and cross it immediately if it is aggressive. */
  placeBookOrder: (body: BookOrderRequest) =>
    req<BookOrderResponse>('/book/order', { method: 'POST', body: JSON.stringify(body) }),

  /** The ladder as ONE party may see it. */
  bookState: (instrumentId: string, as = 'Venue', cashInstrument = 'USDC') =>
    req<BookState>(
      `/book/state?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}` +
        `&as=${encodeURIComponent(as)}`,
    ),

  /** The public tape: price, size, time — and no identities. */
  bookTape: (instrumentId: string, as = 'Venue') =>
    req<BookTape[]>(
      `/book/tape?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&as=${encodeURIComponent(as)}`,
    ),

  /** Your bilateral confirms (Maker / Taker). */
  bookConfirms: (as: string, instrumentId = '') =>
    req<BookConfirm[]>(
      `/book/confirms?as=${encodeURIComponent(as)}` +
        (instrumentId ? `&instrumentId=${encodeURIComponent(instrumentId)}` : ''),
    ),

  /** Pull an order off the book; its reserved backing returns to the trader. */
  cancelBookOrder: (orderCid: string, trader: string) =>
    req<BookState>(`/book/order/${encodeURIComponent(orderCid)}/cancel`, {
      method: 'POST',
      body: JSON.stringify({ trader }),
    }),

  /** Halt the session. Cancellation stays open, as in a real halt. */
  closeBookSession: (instrumentId: string, cashInstrument = 'USDC') =>
    req<BookState>(
      `/book/close?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}`,
      { method: 'POST' },
    ),

  /** Resume a halted session. */
  reopenBookSession: (instrumentId: string, cashInstrument = 'USDC') =>
    req<BookState>(
      `/book/open?instrumentId=${encodeURIComponent(instrumentId)}` +
        `&cashInstrument=${encodeURIComponent(cashInstrument)}`,
      { method: 'POST' },
    ),
};
