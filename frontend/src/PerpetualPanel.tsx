// PerpetualPanel — LEVERAGED LONG AND SHORT, cash-settled.
//
// The rest of the desk moves real assets: the auction delivers them, the fund
// wraps them. This is the one place you take a VIEW without holding anything —
// post USDC, get exposure, settle in cash. It is what lets a holder hedge, and
// what lets an arbitrageur take the other side of a share that has drifted from
// NAV without first funding the whole basket.
//
// LAID OUT THE WAY A DERIVATIVES TICKET ACTUALLY IS, because the order of the
// fields is the order of the decision. A trader picks a DIRECTION, then how much
// they are willing to lose, then how hard to lean on it — and the position size
// falls out. Asking for size first (as the ledger's own choice does) makes the
// leverage ceiling a rejection you discover after typing rather than a limit you
// cannot exceed, which is why every venue that has ever run this asks the other
// way round.
//
// THE NUMBER THE WHOLE PANEL IS BUILT AROUND is the liquidation price, shown
// BEFORE the position is opened. It is the one figure that decides whether a
// leveraged trade was reckless, and showing it only after the fact — which is
// where most interfaces put it — tells you what you already cannot change.

import { useCallback, useEffect, useRef, useState } from 'react';
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
  /** THE DESK'S SELECTED ASSET. One picker for the whole screen: changing the
      market here changes the quote at the top and every other panel with it,
      because a desk where three cards disagree about what you are looking at is
      three demos rather than one. */
  asset: string;
  onAsset: (id: string) => void;
  onChanged: () => void;
  flash: (m: string) => void;
}

const CASH = 'USDC';
const fmt = (n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 6 });
const px = (n: number | null | undefined) =>
  n === null || n === undefined || Number.isNaN(n)
    ? '—'
    : n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const signed = (n: number) =>
  `${n > 0 ? '+' : ''}${n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const pct = (n: number) => `${n > 0 ? '+' : ''}${n.toFixed(2)}%`;

/** Preset rungs, the way a venue offers them rather than a free slider alone. */
const LEV_PRESETS = [1, 2, 3, 5, 10];

export default function PerpetualPanel({ parties, instruments, acting, asset, onAsset, onChanged, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  const assets = instruments.filter((i) => i.kind !== 'Cash' && i.referencePrice != null);

  const instrumentId = asset;
  const setInstrumentId = onAsset;
  const [markets, setMarkets] = useState<PerpMarket[]>([]);
  const [positions, setPositions] = useState<PerpPosition[]>([]);
  const [viewAs, setViewAs] = useState<string>(acting || 'Alice');

  /** `alice-crossdesk::1220…` -> `Alice`, so the blotter reads like a desk. */
  function who(raw: string): string {
    if (!raw) return raw;
    const head = raw.split('::')[0];
    const hit = people.find(
      (x) => x.party === raw || x.label === raw || x.party.split('::')[0] === head,
    );
    return hit ? hit.label : head;
  }

  const [trader, setTrader] = useState<string>(acting || 'Alice');
  const [side, setSide] = useState<PerpSide>('Long');
  const [margin, setMargin] = useState<string>('200');
  const [leverage, setLeverage] = useState<number>(2);
  const [balance, setBalance] = useState<number | null>(null);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');
  const [loaded, setLoaded] = useState(false);

  const market = markets.find((m) => m.instrumentId === instrumentId) ?? null;

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
      const [ms, ps] = await Promise.all([api.perpMarkets('Venue'), api.perpPositions(viewAs)]);
      setMarkets(ms);
      setPositions(ps);
      setLoaded(true);
      setErr('');
    } catch (e) {
      const m = errorMessage(e);
      setMarkets([]);
      setPositions([]);
      setLoaded(true);
      setErr(/no open perpetual market/i.test(m) ? '' : m);
    }
  }, [viewAs]);

  // Positions are marked to a moving index, so this polls: a liquidation price you
  // have to refresh by hand is a liquidation price you learn about afterwards.
  useEffect(() => {
    void refresh();
    const t = setInterval(refresh, 10_000);
    return () => clearInterval(t);
  }, [refresh]);

  // A MARKET OPENS BECAUSE YOU LOOKED AT IT, not because you pressed a button.
  //
  // Opening one is a ledger write, so this is guarded: each instrument is attempted
  // at most once per session (`tried`), and only after `refresh` has actually
  // reported back — otherwise the first render, before any market has loaded, would
  // read "no market" and open a duplicate. Funding the pool follows in the same
  // step, because a market that cannot pay a winner is not a market, and making the
  // trader discover that by clicking a second button is a worse failure than the
  // write itself.
  const tried = useRef<Set<string>>(new Set());
  useEffect(() => {
    if (!instrumentId || busy || !loaded) return;
    if (markets.some((m) => m.instrumentId === instrumentId)) return;
    if (tried.current.has(instrumentId)) return;
    tried.current.add(instrumentId);
    (async () => {
      try {
        await api.openPerpMarket({ instrumentId, cashInstrument: CASH });
        await api.fundPerpInsurance(instrumentId, 100000).catch(() => undefined);
        await refresh();
      } catch (e) {
        // An instrument with no attested mark and no basket NAV cannot be indexed.
        // That is a real answer, not a failure to retry against.
        setErr(errorMessage(e));
      }
    })();
  }, [instrumentId, markets, busy, loaded, refresh]);

  // Free cash, so the % buttons mean something and the ticket can refuse a margin
  // the trader does not have before the ledger has to.
  useEffect(() => {
    let alive = true;
    api
      .holdings(trader)
      .then((hs) => {
        if (!alive) return;
        setBalance(hs.filter((h) => h.instrumentId === CASH).reduce((t, h) => t + Number(h.amount), 0));
      })
      .catch(() => setBalance(null));
    return () => {
      alive = false;
    };
  }, [trader, positions]);

  // ---- the ticket's arithmetic, in one place -------------------------------
  const marginNum = Number(margin);
  const marginValid = Number.isFinite(marginNum) && marginNum > 0;
  const notional = market && marginValid ? marginNum * leverage : 0;
  // DAML DECIMAL IS `Numeric 10` — EXACTLY TEN DECIMAL PLACES, and the ledger rejects
  // anything longer outright rather than rounding it for us:
  //   COMMAND_PREPROCESSING_FAILED: Cannot represent 4.481042197630112 as (Numeric 10)
  // A margin of 1,500 over an index of 334.74355604 is precisely that division, so any
  // position whose size does not divide cleanly failed to open at all. Round HERE, at
  // the point the number is invented, so what the ticket previews is exactly what the
  // ledger is asked to sign.
  const size =
    market && notional > 0 ? Number((notional / market.indexPrice).toFixed(10)) : 0;

  /**
   * ESTIMATED LIQUIDATION PRICE, before the position exists. Solving
   * equity = maintenance for the mark, with entry = the current index:
   *   LONG  : (size·entry − margin) / (size · (1 − mmr))
   *   SHORT : (size·entry + margin) / (size · (1 + mmr))
   * Mirrors PerpetualController.liquidationPriceOf, which mirrors the ledger.
   */
  const estLiq = (() => {
    if (!market || size <= 0) return null;
    const mmr = market.maintenanceMarginBps / 10000;
    const atEntry = size * market.indexPrice;
    const denom = side === 'Long' ? size * (1 - mmr) : size * (1 + mmr);
    if (denom <= 0) return null;
    return side === 'Long' ? (atEntry - marginNum) / denom : (atEntry + marginNum) / denom;
  })();

  /** How far the index may move against you before the margin is gone. */
  const ruinMovePct = leverage > 0 ? 100 / leverage : 0;
  const overBalance = balance !== null && marginValid && marginNum > balance;
  const canOpen = !!market && market.isOpen && marginValid && size > 0 && !overBalance && !busy;



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
    if (!market) return;
    const r = await run(() =>
      api.openPerpPosition({ trader, side, size, instrumentId, collateral: marginNum, cashInstrument: CASH }),
    );
    if (r) {
      flash(
        `${trader} ${side.toLowerCase()} ${fmt(r.size)} ${instrumentId} @ ${px(r.entryPrice)} · ` +
          `${r.leverage ?? '—'}x · liquidation ${px(r.liquidationPrice)}`,
      );
      onChanged();
      void refresh();
    }
  }

  async function close(p: PerpPosition) {
    const r = await run(() => api.closePerpPosition(p.contractId, p.trader));
    if (r) {
      flash(`Closed ${r.side.toLowerCase()} ${fmt(r.size)} @ ${px(r.exitPrice)} — P&L ${signed(r.realisedPnl)} ${CASH}`);
      onChanged();
      void refresh();
    }
  }

  async function topUp(p: PerpPosition) {
    const r = await run(() => api.addPerpCollateral(p.contractId, p.trader, 100));
    if (r) {
      flash(`Margin now ${px(r.collateral)} — liquidation moved to ${px(r.liquidationPrice)}`);
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

  const isLong = side === 'Long';

  return (
    <section className="card perp" aria-label="Leveraged long and short">
      <div className="card-head">
        <h2>Leverage</h2>
        <span className="who">perpetuals · cash-settled</span>
      </div>

      {/* ---- market strip ---- */}
      <div className="row wrap gap perp-top">
        <label>
          Market
          <select value={instrumentId} onChange={(e) => setInstrumentId(e.target.value)} disabled={busy}>
            {assets.length === 0 && <option value="">no marked instrument</option>}
            {assets.map((i) => (
              <option key={i.id} value={i.id}>{i.id}-PERP</option>
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
        <button type="button" className="ghost small" onClick={() => void refresh()} disabled={busy}>Refresh</button>
        {market && (
          <button type="button" className="ghost small" onClick={() => void syncIndex()} disabled={busy}>
            Sync index
          </button>
        )}
      </div>

      {!market && instrumentId && (
        <p className="muted">
          {err ? `${instrumentId} cannot be indexed — it has no attested mark and is not a basket.`
               : `Opening the ${instrumentId} market…`}
        </p>
      )}

      {market && (
        <>
          {/* ---- the tape strip a derivatives screen leads with ---- */}
          <div className="perp-stats">
            <div className="stat">
              <span className="k">Index</span>
              <span className="v mono">{px(market.indexPrice)}</span>
            </div>
            <div className="stat">
              <span className="k">Max leverage</span>
              <span className="v mono">{fmt(market.maxLeverage)}x</span>
            </div>
            <div className="stat">
              <span className="k">Maintenance</span>
              <span className="v mono">{fmt(market.maintenanceMarginBps / 100)}%</span>
            </div>
            <div className="stat" title="Derived from the perp's own price against the index, and capped">
              <span className="k">Funding</span>
              <span className="v mono">{(market.fundingRate * 100).toFixed(4)}%</span>
            </div>
            <div className="stat" title="openLong − openShort: the directional risk the venue's pool carries">
              <span className="k">Skew</span>
              <span className={`v mono ${market.skew > 0 ? 'up' : market.skew < 0 ? 'down' : ''}`}>
                {signed(market.skew)}
              </span>
            </div>
          </div>

          {/* THE POOL IS VENUE PLUMBING, NOT A TRADER'S PROBLEM. A market opens
              funded, so this only appears if that failed — and the button to fix it
              belongs to the operator, because it is the operator's balance sheet.
              A trader still sees the WARNING, because taking leverage against a venue
              that cannot pay a winner is the one thing they are entitled to know. */}
          {!market.insured && (
            <div className="empty">
              <p>
                This market has no insurance pool, so a winning position cannot be paid — the
                ledger refuses the close rather than settling at zero.
              </p>
              {viewAs === 'Venue' && (
                <button type="button" className="primary venue" onClick={() => void fundPool()} disabled={busy}>
                  Fund the pool
                </button>
              )}
            </div>
          )}

          {/* ================= THE TICKET ================= */}
          <div className="ticket-box">
            {/* 1 — DIRECTION, first and unmistakable. */}
            <div className="side-toggle" role="group" aria-label="Direction">
              <button
                type="button"
                className={`side-btn long ${isLong ? 'on' : ''}`}
                onClick={() => setSide('Long')}
                disabled={busy}
              >
                Long
                <span>gains the rise</span>
              </button>
              <button
                type="button"
                className={`side-btn short ${!isLong ? 'on' : ''}`}
                onClick={() => setSide('Short')}
                disabled={busy}
              >
                Short
                <span>gains the fall</span>
              </button>
            </div>

            <div className="row wrap gap">
              <label>
                Trader
                <select value={trader} onChange={(e) => setTrader(e.target.value)} disabled={busy}>
                  {people.map((p) => (
                    <option key={p.party} value={p.label}>{p.label}</option>
                  ))}
                </select>
              </label>
              <label className="grow">
                <span className="lbl-row">
                  Margin ({CASH})
                  <span className="avail">
                    available <strong className="mono">{balance === null ? '—' : px(balance)}</strong>
                  </span>
                </span>
                <input
                  inputMode="decimal"
                  value={margin}
                  onChange={(e) => setMargin(e.target.value)}
                  disabled={busy}
                  className={overBalance ? 'bad' : undefined}
                />
                <span className="pcts">
                  {[25, 50, 75, 100].map((q) => (
                    <button
                      key={q}
                      type="button"
                      className="pct"
                      disabled={busy || balance === null}
                      onClick={() => balance !== null && setMargin(String(Math.floor((balance * q) / 100)))}
                    >
                      {q}%
                    </button>
                  ))}
                </span>
              </label>
            </div>

            {/* 2 — LEVERAGE. Presets plus a slider, capped by the market itself, so
                   the ceiling is a limit rather than a rejection. */}
            <div className="lev-box">
              <div className="lbl-row">
                <span>Leverage</span>
                <strong className="mono lev-val">{leverage}x</strong>
              </div>
              <input
                type="range"
                min={1}
                max={Math.max(1, Math.floor(market.maxLeverage))}
                step={1}
                value={leverage}
                onChange={(e) => setLeverage(Number(e.target.value))}
                disabled={busy}
              />
              <div className="presets">
                {LEV_PRESETS.filter((l) => l <= market.maxLeverage).map((l) => (
                  <button
                    key={l}
                    type="button"
                    className={`preset ${leverage === l ? 'on' : ''}`}
                    onClick={() => setLeverage(l)}
                    disabled={busy}
                  >
                    {l}x
                  </button>
                ))}
                <span className="hint">1x = no leverage</span>
              </div>
            </div>

            {/* 3 — WHAT YOU ARE ABOUT TO DO, in the numbers that matter. */}
            <div className="preview-grid">
              <div><span className="k">Position size</span><span className="v mono">{size > 0 ? fmt(Number(size.toFixed(6))) : '—'} {instrumentId}</span></div>
              <div><span className="k">Exposure</span><span className="v mono">{px(notional)} {CASH}</span></div>
              <div><span className="k">Entry</span><span className="v mono">{px(market.indexPrice)}</span></div>
              <div className="danger">
                <span className="k">Est. liquidation</span>
                <span className="v mono">{px(estLiq)}</span>
              </div>
              <div className="danger">
                <span className="k">Wipes out on a move of</span>
                <span className="v mono">{ruinMovePct.toFixed(1)}%</span>
              </div>
            </div>

            {overBalance && (
              <p className="error">
                {trader} holds {px(balance)} {CASH} — that margin is more than the balance.
              </p>
            )}

            <button
              type="button"
              className={`place ${isLong ? 'long' : 'short'}`}
              onClick={() => void open()}
              disabled={!canOpen}
            >
              {busy ? 'Working…' : `${side} ${instrumentId} · ${leverage}x`}
            </button>
          </div>

          {/* ================= POSITIONS ================= */}
          <h3>Positions <span className="sub">{viewAs}</span></h3>
          <p className="note">
            A position is <strong>private to its trader</strong> — signed by the venue and that
            trader, observed by the auditor, and by nobody else. On a leveraged product that
            matters more, not less: a visible liquidation price is an invitation to push the
            market into it.
          </p>
          {positions.length === 0 ? (
            <p className="muted">no open positions visible to {viewAs}</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Trader</th><th>Side</th><th>Size</th><th>Entry</th><th>Mark</th>
                  <th>Lev</th><th>Margin</th><th>P&amp;L</th><th>ROE</th><th>Liq.</th><th />
                </tr>
              </thead>
              <tbody>
                {positions.map((p) => {
                  // An UNMARKED position shows dashes, not zeros. The index was
                  // unavailable, so the honest answer is "unknown" — rendering it as
                  // flat P&L and a healthy margin would be a reassurance the data
                  // does not support.
                  const roe =
                    p.marked && p.unrealisedPnl !== null && p.collateral > 0
                      ? (p.unrealisedPnl / p.collateral) * 100
                      : null;
                  return (
                    <tr key={p.contractId} className={p.liquidatable ? 'at-risk' : undefined}>
                      {/* The venue is a signatory on every position and the auditor
                          observes them all, so either can be looking at somebody else's
                          risk. A blotter that does not say whose is unreadable to them. */}
                      <td title={p.trader}>{who(p.trader)}</td>
                      <td className={p.side === 'Long' ? 'maker' : 'taker'}>{p.side}</td>
                      <td>{fmt(p.size)}</td>
                      <td>{px(p.entryPrice)}</td>
                      <td className={p.marked ? undefined : 'muted'}>
                        {p.marked ? px(p.markPrice) : 'no index'}
                      </td>
                      <td className="muted">{p.leverage ?? '—'}x</td>
                      <td>{px(p.collateral)}</td>
                      <td className={!p.marked ? 'muted' : (p.unrealisedPnl ?? 0) >= 0 ? 'maker' : 'taker'}>
                        {p.marked && p.unrealisedPnl !== null ? signed(p.unrealisedPnl) : '—'}
                      </td>
                      {/* ROE, not just P&L: on 10x a 1% move is a 10% return, and the
                          absolute number hides that entirely. */}
                      <td className={roe === null ? 'muted' : roe >= 0 ? 'maker' : 'taker'}>
                        {roe === null ? '—' : pct(roe)}
                      </td>
                      <td className={p.liquidatable ? 'taker' : 'muted'}>
                        {px(p.liquidationPrice)}
                        {p.liquidatable && <div className="basis">below maintenance</div>}
                      </td>
                      <td className="acts">
                        <button type="button" className="link" onClick={() => void close(p)} disabled={busy}>close</button>
                        <button type="button" className="link" onClick={() => void topUp(p)} disabled={busy}>+100</button>
                        {p.liquidatable && viewAs === 'Venue' && (
                          <button type="button" className="link danger" onClick={() => void liquidate(p)} disabled={busy}>
                            liquidate
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </>
      )}

      {err && <p className="error">{err}</p>}
    </section>
  );
}
