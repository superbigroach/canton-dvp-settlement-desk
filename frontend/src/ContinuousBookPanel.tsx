// ContinuousBookPanel — THE CONTINUOUS SESSION, the auction's other half.
//
// The closing cross does not manufacture a price out of nothing: it inherits one
// from the ladder that accumulates all day. This is that ladder. Limit interest
// RESTS here, is matched by price then time, and settles bilaterally at the
// MAKER's price the instant it is crossed — one atomic transaction per sweep.
//
// Two things on screen are worth pointing at, because neither is obtainable on a
// public chain:
//   * THE BOOK IS DARK PRE-TRADE. A RestingOrder is signed by operator + trader
//     and observed by NOBODY — so a trader sees only its own orders, and even the
//     AUDITOR sees none of them while they rest. Switch the "viewing as" selector
//     to watch the same ledger disclose different books.
//   * THE TAPE IS PUBLIC AND ANONYMOUS. Every fill prints price, size and time to
//     the whole session and names no one. Dark pre-trade, lit post-trade — which
//     is the whole of MiFID II's waiver structure, not a shortcut.

import { useCallback, useEffect, useState } from 'react';
import {
  api,
  errorMessage,
  type BookConfirm,
  type BookOrderResponse,
  type BookSideName,
  type BookState,
  type BookTape,
  type Instrument,
  type Party,
  type TimeInForce,
} from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  acting: string;
  onChanged: () => void; // the desk's holdings move on every fill
  flash: (m: string) => void;
}

const CASH = 'USDC';
const fmt = (n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 6 });
const px = (n: number | null) =>
  n === null || n === undefined
    ? '—'
    : n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function ContinuousBookPanel({
  parties,
  instruments,
  acting,
  onChanged,
  flash,
}: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  // Only instruments with a published mark can anchor a session's price band.
  const assets = instruments.filter((i) => i.kind !== 'Cash' && i.referencePrice != null);

  const [instrumentId, setInstrumentId] = useState<string>('');
  const [viewAs, setViewAs] = useState<string>('Venue');
  const [state, setState] = useState<BookState | null>(null);
  const [tape, setTape] = useState<BookTape[]>([]);
  const [confirms, setConfirms] = useState<BookConfirm[]>([]);
  const [last, setLast] = useState<BookOrderResponse | null>(null);

  // the order ticket
  const [trader, setTrader] = useState<string>(acting || 'Alice');
  const [side, setSide] = useState<BookSideName>('Bid');
  const [quantity, setQuantity] = useState<string>('10');
  const [limitPrice, setLimitPrice] = useState<string>('');
  const [tif, setTif] = useState<TimeInForce>('GTC');

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');

  useEffect(() => {
    if (!instrumentId && assets.length > 0) setInstrumentId(assets[0].id);
  }, [assets, instrumentId]);

  useEffect(() => {
    setTrader(acting || 'Alice');
  }, [acting]);

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setErr('');
    try {
      return await fn();
    } catch (e) {
      setErr(errorMessage(e));
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  const refresh = useCallback(async () => {
    if (!instrumentId) return;
    try {
      const [s, t, c] = await Promise.all([
        api.bookState(instrumentId, viewAs, CASH),
        api.bookTape(instrumentId, viewAs),
        api.bookConfirms(viewAs, instrumentId),
      ]);
      setState(s);
      setTape(t);
      setConfirms(c);
      setErr('');
    } catch (e) {
      // No session yet is the normal cold-start state, not an error to shout about.
      const m = errorMessage(e);
      setState(null);
      setTape([]);
      setConfirms([]);
      setErr(/no continuous session/i.test(m) ? '' : m);
    }
  }, [instrumentId, viewAs]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function openSession() {
    const r = await run(() =>
      api.openBookSession({ instrumentId, cashInstrument: CASH }),
    );
    if (r) {
      setState(r);
      flash(`Continuous session open for ${instrumentId} — band ${px(r.bandLow)} … ${px(r.bandHigh)}`);
      void refresh();
    }
  }

  async function place() {
    const qty = Number(quantity);
    if (!Number.isFinite(qty) || qty <= 0) {
      setErr('quantity must be a positive number');
      return;
    }
    const hasLimit = limitPrice.trim() !== '';
    const lim = hasLimit ? Number(limitPrice) : null;
    if (hasLimit && (!Number.isFinite(lim as number) || (lim as number) <= 0)) {
      setErr('a stated limit must be a positive number — leave it blank for a market order');
      return;
    }
    const r = await run(() =>
      api.placeBookOrder({
        trader,
        side,
        quantity: qty,
        instrumentId,
        cashInstrument: CASH,
        limitPrice: lim,
        // An unpriced order is forced to IOC by the ledger whatever we send.
        timeInForce: hasLimit ? tif : 'IOC',
      }),
    );
    if (r) {
      setLast(r);
      if (r.fills.length > 0) {
        flash(
          `${trader} ${side === 'Bid' ? 'bought' : 'sold'} ${fmt(r.filledQuantity)} ` +
            `in ${r.fills.length} fill${r.fills.length > 1 ? 's' : ''}` +
            (r.restingQuantity > 0 ? `, ${fmt(r.restingQuantity)} resting` : ''),
        );
      } else {
        flash(`${trader}'s order rests on the book — nothing crossed it`);
      }
      onChanged();
      void refresh();
    }
  }

  async function cancel(orderCid: string, owner: string) {
    const r = await run(() => api.cancelBookOrder(orderCid, owner));
    if (r) {
      setState(r);
      flash('Order pulled — its reserved backing is released');
      onChanged();
      void refresh();
    }
  }

  async function toggleSession() {
    if (!state) return;
    const r = await run(() =>
      state.isOpen
        ? api.closeBookSession(instrumentId, CASH)
        : api.reopenBookSession(instrumentId, CASH),
    );
    if (r) {
      setState(r);
      flash(state.isOpen ? 'Session halted — cancellation still works' : 'Session resumed');
      void refresh();
    }
  }

  const marketOrder = limitPrice.trim() === '';

  return (
    <section className="card continuous-book">
      <div className="card-head">
        <h2>Continuous Session</h2>
      </div>
      {/* This used to sit inside card-head, which is a no-wrap flex row — on a narrow
          card the strapline collided with the heading. It is also the one thing about
          this panel a viewer needs explained, so it earns a full line. */}
      <p className="hint">
        Limit interest <strong>rests</strong> here between auctions, matched by{' '}
        <strong>price then time</strong> and settled at the maker&rsquo;s price.
        <br />
        <strong>Dark pre-trade</strong> — a resting order has no observers, so even the
        auditor cannot see the book. <strong>Lit post-trade</strong> — every fill prints
        to a public tape that names nobody.
      </p>

      <div className="row wrap gap">
        <label>
          Instrument
          <select
            value={instrumentId}
            onChange={(e) => setInstrumentId(e.target.value)}
            disabled={busy}
          >
            {assets.length === 0 && <option value="">no marked instrument</option>}
            {assets.map((i) => (
              <option key={i.id} value={i.id}>
                {i.id}
              </option>
            ))}
          </select>
        </label>

        {/* THE DISCLOSURE DEMO. Same ledger, different party, different book. */}
        <label>
          Viewing as
          <select value={viewAs} onChange={(e) => setViewAs(e.target.value)} disabled={busy}>
            <option value="Venue">Venue (operator — sees the whole book)</option>
            {people.map((p) => (
              <option key={p.party} value={p.label}>
                {p.label}
              </option>
            ))}
          </select>
        </label>

        <button type="button" onClick={() => void refresh()} disabled={busy || !instrumentId}>
          Refresh
        </button>
        {state && (
          <button type="button" onClick={() => void toggleSession()} disabled={busy}>
            {state.isOpen ? 'Halt session' : 'Resume session'}
          </button>
        )}
      </div>

      {!state && instrumentId && (
        <div className="empty">
          <p>
            No continuous session for <strong>{instrumentId}</strong> yet. Opening one anchors
            the price band on the instrument's published mark.
          </p>
          <button type="button" onClick={() => void openSession()} disabled={busy}>
            Open the session
          </button>
        </div>
      )}

      {state && (
        <>
          <div className="book-meta row wrap gap">
            <span>
              anchor <strong>{px(state.referencePrice)}</strong>
            </span>
            <span>
              band <strong>{px(state.bandLow)}</strong> … <strong>{px(state.bandHigh)}</strong>
            </span>
            <span>
              best bid <strong>{px(state.bestBid)}</strong> / ask{' '}
              <strong>{px(state.bestAsk)}</strong>
            </span>
            <span>
              resting <strong>{state.liveCount}</strong>
            </span>
            <span className={state.isOpen ? 'pill open' : 'pill halted'}>
              {state.isOpen ? 'OPEN' : 'HALTED'}
            </span>
          </div>

          {/* ---- the ticket ---- */}
          <div className="row wrap gap ticket">
            <label>
              Trader
              <select value={trader} onChange={(e) => setTrader(e.target.value)} disabled={busy}>
                {people.map((p) => (
                  <option key={p.party} value={p.label}>
                    {p.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Side
              <select
                value={side}
                onChange={(e) => setSide(e.target.value as BookSideName)}
                disabled={busy}
              >
                <option value="Bid">Bid (buy)</option>
                <option value="Ask">Ask (sell)</option>
              </select>
            </label>
            <label>
              Quantity
              <input
                inputMode="decimal"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                disabled={busy}
              />
            </label>
            <label>
              Limit <span className="hint">blank = market</span>
              <input
                inputMode="decimal"
                placeholder="market"
                value={limitPrice}
                onChange={(e) => setLimitPrice(e.target.value)}
                disabled={busy}
              />
            </label>
            <label>
              TIF
              <select
                value={marketOrder ? 'IOC' : tif}
                onChange={(e) => setTif(e.target.value as TimeInForce)}
                disabled={busy || marketOrder}
                title={
                  marketOrder
                    ? 'an unpriced order must be immediate-or-cancel — it may never rest'
                    : undefined
                }
              >
                <option value="GTC">GTC (rests)</option>
                <option value="IOC">IOC (fill now, kill remainder)</option>
              </select>
            </label>
            {/* Sized to match Refresh in the row above — the ticket's controls are all
                one line, and a button that outgrows its own row reads like an error. */}
            <button
              type="button"
              className="ticket-submit"
              onClick={() => void place()}
              disabled={busy || !state.isOpen}
            >
              {busy ? 'Working…' : 'Place order'}
            </button>
          </div>

          {last && last.fills.length > 0 && (
            <div className="fills">
              <h3>
                Filled {fmt(last.filledQuantity)}
                {last.restingQuantity > 0 && <> · {fmt(last.restingQuantity)} resting</>}
              </h3>
              <table>
                <thead>
                  <tr>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Cash</th>
                    <th>Counterparty</th>
                  </tr>
                </thead>
                <tbody>
                  {last.fills.map((f, i) => (
                    <tr key={i}>
                      <td>{fmt(f.quantity)}</td>
                      {/* THE MAKER'S price, not the taker's — that is the rule
                          walkBook enforces, and the confirm proves it. */}
                      <td>{px(f.price)}</td>
                      <td>{px(f.cashAmount)}</td>
                      <td className="muted">{f.maker}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p className="note">
                The contra is <strong>not named</strong>: the venue stood between the two
                sides as momentary counterparty, so a confirm never discloses who supplied
                the liquidity.
              </p>
            </div>
          )}

          {/* ---- the ladder, as this party may see it ---- */}
          <div className="ladder row wrap">
            <div className="side">
              <h3>Bids</h3>
              {state.bids.length === 0 && <p className="muted">none visible</p>}
              {state.bids.map((o) => (
                <div className="lvl" key={o.contractId}>
                  <span className="qty">{fmt(o.quantity)}</span>
                  <span className="px">{px(o.limitPrice)}</span>
                  <span className="who">{o.trader}</span>
                  <span className="seq" title="arrival stamp — time priority">
                    #{o.seqNo}
                  </span>
                  <button
                    type="button"
                    className="link"
                    onClick={() => void cancel(o.contractId, o.trader)}
                    disabled={busy}
                  >
                    cancel
                  </button>
                </div>
              ))}
            </div>
            <div className="side">
              <h3>Asks</h3>
              {state.asks.length === 0 && <p className="muted">none visible</p>}
              {state.asks.map((o) => (
                <div className="lvl" key={o.contractId}>
                  <span className="qty">{fmt(o.quantity)}</span>
                  <span className="px">{px(o.limitPrice)}</span>
                  <span className="who">{o.trader}</span>
                  <span className="seq" title="arrival stamp — time priority">
                    #{o.seqNo}
                  </span>
                  <button
                    type="button"
                    className="link"
                    onClick={() => void cancel(o.contractId, o.trader)}
                    disabled={busy}
                  >
                    cancel
                  </button>
                </div>
              ))}
            </div>
          </div>

          <p className="note">
            Viewing as <strong>{viewAs}</strong>.{' '}
            {viewAs === 'Venue'
              ? 'The operator signs every order, so it sees the whole ladder.'
              : 'A resting order has no observers — you see only your own, and the auditor sees none at all.'}
          </p>

          {/* ---- the public tape ---- */}
          <div className="tape">
            <h3>Tape <span className="sub">public · anonymous</span></h3>
            {tape.length === 0 && <p className="muted">nothing has printed yet</p>}
            {tape.length > 0 && (
              <table>
                <thead>
                  <tr>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Sweep</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {tape.slice(0, 12).map((t) => (
                    <tr key={t.contractId}>
                      <td>{fmt(t.quantity)}</td>
                      <td>{px(t.price)}</td>
                      <td className="muted">#{t.matchSeq}</td>
                      <td className="muted">{t.printedAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* ---- confirms for the party being viewed ---- */}
          {confirms.length > 0 && (
            <div className="confirms">
              <h3>
                Confirms <span className="sub">{viewAs}</span>
              </h3>
              <table>
                <thead>
                  <tr>
                    <th>Side</th>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Cash</th>
                    <th>Liquidity</th>
                  </tr>
                </thead>
                <tbody>
                  {confirms.slice(0, 12).map((c) => (
                    <tr key={c.contractId}>
                      <td>{c.side}</td>
                      <td>{fmt(c.quantity)}</td>
                      <td>{px(c.price)}</td>
                      <td>{px(c.cashAmount)}</td>
                      <td className={c.liquidity === 'Maker' ? 'maker' : 'taker'}>
                        {c.liquidity}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {err && <p className="error">{err}</p>}
    </section>
  );
}
