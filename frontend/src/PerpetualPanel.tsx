// PerpetualPanel — LEVERAGED LONG AND SHORT, cash-settled.
//
// The rest of the desk moves real assets: the auction delivers them, the fund
// wraps them. This is the one place you take a VIEW without holding anything —
// post USDC, get exposure, settle in cash. It is what lets a holder hedge, and
// what lets an arbitrageur take the other side of a share that has drifted from
// NAV without first funding the whole basket.
//
// TWO THINGS ON SCREEN ARE THE PRODUCT:
//   * THE LIQUIDATION PRICE. The number a leveraged trader most wants and that
//     no venue shows prominently enough. It is computed from the same formula
//     the ledger liquidates on, so it is not an estimate.
//   * THE SKEW. openLong − openShort: how lopsided the venue's book is, and
//     therefore how much directional risk the pool is carrying. Published rather
//     than hidden, because the funding rate is the lever that closes it and a
//     trader is entitled to know which way it is pointing before paying it.

import { useCallback, useEffect, useState } from 'react';
import {
  api,
  errorMessage,
  type Instrument,
  type PerpMarket,
  type PerpPosition,
  type PerpSide,
  type Party,
} from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  acting: string;
  onChanged: () => void;
  flash: (m: string) => void;
}

const CASH = 'USDC';
const fmt = (n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 6 });
const px = (n: number | null | undefined) =>
  n === null || n === undefined
    ? '—'
    : n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const signed = (n: number) =>
  `${n > 0 ? '+' : ''}${n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

export default function PerpetualPanel({ parties, instruments, acting, onChanged, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  // Only an instrument with an attested mark can be indexed against.
  const assets = instruments.filter((i) => i.kind !== 'Cash' && i.referencePrice != null);

  const [instrumentId, setInstrumentId] = useState<string>('');
  const [markets, setMarkets] = useState<PerpMarket[]>([]);
  const [positions, setPositions] = useState<PerpPosition[]>([]);
  const [viewAs, setViewAs] = useState<string>(acting || 'Alice');

  // the ticket
  const [trader, setTrader] = useState<string>(acting || 'Alice');
  const [side, setSide] = useState<PerpSide>('Long');
  const [size, setSize] = useState<string>('10');
  const [collateral, setCollateral] = useState<string>('200');

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');

  const market = markets.find((m) => m.instrumentId === instrumentId) ?? null;

  useEffect(() => {
    if (!instrumentId && assets.length > 0) setInstrumentId(assets[0].id);
  }, [assets, instrumentId]);

  useEffect(() => {
    setTrader(acting || 'Alice');
    setViewAs(acting || 'Alice');
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
    try {
      const [ms, ps] = await Promise.all([
        api.perpMarkets('Venue'),
        api.perpPositions(viewAs),
      ]);
      setMarkets(ms);
      setPositions(ps);
      setErr('');
    } catch (e) {
      const m = errorMessage(e);
      setMarkets([]);
      setPositions([]);
      setErr(/no open perpetual market/i.test(m) ? '' : m);
    }
  }, [viewAs]);

  // Positions are marked to a moving index, so this is polled: a liquidation
  // price you have to refresh by hand is a liquidation price you find out about
  // afterwards.
  useEffect(() => {
    void refresh();
    const t = setInterval(refresh, 10_000);
    return () => clearInterval(t);
  }, [refresh]);

  async function openMarket() {
    const r = await run(() => api.openPerpMarket({ instrumentId, cashInstrument: CASH }));
    if (r) {
      flash(`Perpetual market open on ${instrumentId} — index ${px(r.indexPrice)}, ${fmt(r.maxLeverage)}x max`);
      void refresh();
    }
  }

  async function fundPool() {
    const r = await run(() => api.fundPerpInsurance(instrumentId, 100000));
    if (r) {
      flash('Insurance pool funded — the venue can now pay profits');
      onChanged();
      void refresh();
    }
  }

  async function syncIndex() {
    const r = await run(() => api.setPerpIndex(instrumentId, null));
    if (r) {
      flash(`Index re-read from the attested mark: ${px(r.indexPrice)}`);
      void refresh();
    }
  }

  async function open() {
    const sz = Number(size);
    const col = Number(collateral);
    if (!Number.isFinite(sz) || sz <= 0) return setErr('size must be a positive number');
    if (!Number.isFinite(col) || col <= 0) return setErr('collateral must be a positive number');
    const r = await run(() =>
      api.openPerpPosition({ trader, side, size: sz, instrumentId, collateral: col, cashInstrument: CASH }),
    );
    if (r) {
      flash(
        `${trader} ${side.toLowerCase()} ${fmt(r.size)} ${instrumentId} at ${px(r.entryPrice)} · ` +
          `${r.leverage ?? '—'}x · liquidation ${px(r.liquidationPrice)}`,
      );
      onChanged();
      void refresh();
    }
  }

  async function close(p: PerpPosition) {
    const r = await run(() => api.closePerpPosition(p.contractId, p.trader));
    if (r) {
      flash(
        `Closed ${r.side.toLowerCase()} ${fmt(r.size)} at ${px(r.exitPrice)} — ` +
          `P&L ${signed(r.realisedPnl)} ${CASH}, paid out ${px(r.payout)}`,
      );
      onChanged();
      void refresh();
    }
  }

  async function topUp(p: PerpPosition) {
    const r = await run(() => api.addPerpCollateral(p.contractId, p.trader, 100));
    if (r) {
      flash(`Collateral now ${px(r.collateral)} — liquidation moved to ${px(r.liquidationPrice)}`);
      onChanged();
      void refresh();
    }
  }

  async function liquidate(p: PerpPosition) {
    const r = await run(() => api.liquidatePerpPosition(p.contractId));
    if (r) {
      flash(`Liquidated — P&L ${signed(r.realisedPnl)} ${CASH}, returned ${px(r.payout)}`);
      onChanged();
      void refresh();
    }
  }

  return (
    <section className="card perp" aria-label="Leveraged long and short">
      <div className="card-head">
        <h2>Leverage</h2>
      </div>
      <p className="hint">
        Cash-settled perpetuals — post <strong>{CASH}</strong>, take exposure{' '}
        <strong>without holding the asset</strong>. Marked against the same{' '}
        <strong>attested</strong> mark the rest of the desk values against, because a
        leveraged position is liquidated against that number.
      </p>

      <div className="row wrap gap">
        <label>
          Market
          <select value={instrumentId} onChange={(e) => setInstrumentId(e.target.value)} disabled={busy}>
            {assets.length === 0 && <option value="">no marked instrument</option>}
            {assets.map((i) => (
              <option key={i.id} value={i.id}>{i.id}</option>
            ))}
          </select>
        </label>
        <label>
          Viewing as
          <select value={viewAs} onChange={(e) => setViewAs(e.target.value)} disabled={busy}>
            <option value="Venue">Venue (sees every position)</option>
            {people.map((p) => (
              <option key={p.party} value={p.label}>{p.label}</option>
            ))}
          </select>
        </label>
        <button type="button" onClick={() => void refresh()} disabled={busy}>Refresh</button>
        {market && <button type="button" onClick={() => void syncIndex()} disabled={busy}>Sync index</button>}
      </div>

      {!market && instrumentId && (
        <div className="empty">
          <p>No perpetual market on <strong>{instrumentId}</strong> yet.</p>
          <button type="button" onClick={() => void openMarket()} disabled={busy}>Open the market</button>
        </div>
      )}

      {market && (
        <>
          <div className="book-meta row wrap gap">
            <span>index <strong>{px(market.indexPrice)}</strong></span>
            <span>max <strong>{fmt(market.maxLeverage)}x</strong></span>
            <span>maintenance <strong>{fmt(market.maintenanceMarginBps / 100)}%</strong></span>
            <span
              className={market.skew > 0 ? 'skew long' : market.skew < 0 ? 'skew short' : 'skew'}
              title="openLong − openShort: the directional risk the venue's pool is carrying"
            >
              skew <strong>{signed(market.skew)}</strong>
            </span>
            <span title="Derived from the perp's own price against the index, and capped">
              funding <strong>{(market.fundingRate * 100).toFixed(4)}%</strong>
            </span>
            {!market.insured && <span className="pill halted">no pool — profits unpayable</span>}
          </div>

          {!market.insured && (
            <div className="empty">
              <p>
                The venue has no insurance pool, so a winning position cannot be paid and the
                ledger refuses the close rather than settling at zero.
              </p>
              <button type="button" onClick={() => void fundPool()} disabled={busy}>
                Fund the pool
              </button>
            </div>
          )}

          {/* ---- the ticket ---- */}
          <div className="row wrap gap ticket">
            <label>
              Trader
              <select value={trader} onChange={(e) => setTrader(e.target.value)} disabled={busy}>
                {people.map((p) => (
                  <option key={p.party} value={p.label}>{p.label}</option>
                ))}
              </select>
            </label>
            <label>
              Side
              <select value={side} onChange={(e) => setSide(e.target.value as PerpSide)} disabled={busy}>
                <option value="Long">Long — gains the rise</option>
                <option value="Short">Short — gains the fall</option>
              </select>
            </label>
            <label>
              Size
              <input inputMode="decimal" value={size} onChange={(e) => setSize(e.target.value)} disabled={busy} />
            </label>
            <label>
              Collateral ({CASH})
              <input
                inputMode="decimal"
                value={collateral}
                onChange={(e) => setCollateral(e.target.value)}
                disabled={busy}
              />
            </label>
            <button type="button" className="ticket-submit" onClick={() => void open()} disabled={busy || !market.isOpen}>
              {busy ? 'Working…' : 'Open position'}
            </button>
          </div>
          {Number(size) > 0 && Number(collateral) > 0 && (
            <p className="note">
              Notional <strong>{px(Number(size) * market.indexPrice)}</strong> ·{' '}
              <strong>{(market.indexPrice * Number(size) / Number(collateral)).toFixed(2)}x</strong>{' '}
              against a {fmt(market.maxLeverage)}x ceiling
            </p>
          )}

          {/* ---- positions ---- */}
          <h3>
            Positions <span className="sub">{viewAs}</span>
          </h3>
          <p className="note">
            A position is <strong>private to its trader</strong> — signed by the venue and
            that trader, observed by the auditor, and by nobody else. On a leveraged
            product that matters more, not less: a visible liquidation price is an
            invitation to push the market into it.
          </p>
          {positions.length === 0 ? (
            <p className="muted">no open positions visible to {viewAs}</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Side</th><th>Size</th><th>Entry</th><th>Mark</th>
                  <th>Lev</th><th>P&amp;L</th><th>Equity</th><th>Liq. price</th><th />
                </tr>
              </thead>
              <tbody>
                {positions.map((p) => (
                  <tr key={p.contractId} className={p.liquidatable ? 'at-risk' : undefined}>
                    <td className={p.side === 'Long' ? 'maker' : 'taker'}>{p.side}</td>
                    <td>{fmt(p.size)}</td>
                    <td>{px(p.entryPrice)}</td>
                    <td>{px(p.markPrice)}</td>
                    <td className="muted">{p.leverage ?? '—'}x</td>
                    <td className={p.unrealisedPnl >= 0 ? 'maker' : 'taker'}>{signed(p.unrealisedPnl)}</td>
                    <td>{px(p.equity)}</td>
                    <td className={p.liquidatable ? 'taker' : 'muted'}>
                      {px(p.liquidationPrice)}
                      {p.liquidatable && <div className="basis">below maintenance</div>}
                    </td>
                    <td>
                      <button type="button" className="link" onClick={() => void close(p)} disabled={busy}>
                        close
                      </button>
                      <button type="button" className="link" onClick={() => void topUp(p)} disabled={busy}>
                        +100
                      </button>
                      {p.liquidatable && viewAs === 'Venue' && (
                        <button type="button" className="link" onClick={() => void liquidate(p)} disabled={busy}>
                          liquidate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {err && <p className="error">{err}</p>}
    </section>
  );
}
