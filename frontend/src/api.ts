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

export interface MocOrderRequest {
  trader: string;
  side: 'Buy' | 'Sell';
  quantity: number;
  instrumentId: string;
  cashInstrument?: string; // defaults to USDC server-side
  session?: Session;       // Open (MOO) | Close (MOC); defaults to Close server-side
}

export interface MocOrderResponse {
  orderCid: string;
  auctionCid: string;
  openedAuction: boolean;
  closingPrice: number;
}

export interface MocOrderView {
  contractId: string;
  trader: string;
  side: 'Buy' | 'Sell';
  quantity: number;
  limitPrice: number;
}

export interface MocState {
  auctionCid: string | null;
  instrumentId: string;
  cashInstrument: string;
  session: string;              // "Open" | "Close"
  referencePrice: number | null;
  isOpen: boolean;
  orders: MocOrderView[];       // filtered by the ledger to the acting party's view
  othersResting: number;        // OTHER sealed orders hidden from a trader (0 for venue)
}

export interface ClearBookResponse {
  cleared: number;
}

// The NET imbalance of a sealed book — the Designated Liquidity Provider view.
// `disclosed` is true ONLY when the acting party is entitled to see the aggregate
// (the DLP, or the venue); a normal trader gets disclosed=false (HTTP 403). It
// carries side + magnitude ONLY — never any individual order or trader identity.
export interface MocImbalance {
  disclosed: boolean;
  instrumentId: string;
  cashInstrument: string;
  session: string;                       // "Open" | "Close"
  netSide: 'Buy' | 'Sell' | 'Flat' | null;
  netQuantity: number | null;            // magnitude of the imbalance (>= 0)
  referencePrice: number | null;
  liquidityProvider: string | null;      // the DLP's label (who may offset)
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
  closingPrice: number;
  fills: MocFill[];
}

// ---- Decentralised operator: K-of-N committee-attested NAV ----------------

export interface CidResponse {
  contractId: string;
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

  // Designated Liquidity Provider view: the NET imbalance, disclosed BY THE LEDGER
  // only to the DLP (and the venue). A normal trader is denied (HTTP 403) — we treat
  // that as `disclosed:false` rather than an error, so the LP panel simply hides.
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
    // 403 carries a disclosed:false body — the ledger denied the acting party.
    if (res.status === 403) return body as MocImbalance;
    if (!res.ok) throw new ApiError((body && (body.message || body.error)) || `HTTP ${res.status}`);
    return body as MocImbalance;
  },

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
