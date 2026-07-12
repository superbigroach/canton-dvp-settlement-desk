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
};
