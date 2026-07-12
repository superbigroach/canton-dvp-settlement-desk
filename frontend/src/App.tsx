import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  api,
  ApiError,
  type Holding,
  type Instrument,
  type MocState,
  type Party,
  type Session,
} from './api';

const CASH = 'USDC';

// A settlement proof we show the trader: either a bilateral DvP receipt or one
// fill from an opening/closing cross batch. Accumulated newest-first.
interface Receipt {
  key: string;
  kind: 'DvP' | 'Open' | 'Close';
  time: string;
  headline: string; // "Bob bought 3 DEMO:AAPL" etc.
  asset: string;
  quantity: number;
  cashAmount: number;
  unitPrice: number;
  counterpartyLine: string;
  cid: string; // receipt cid (DvP) or settlement-batch cid (cross)
}

type Side = 'Buy' | 'Sell';
type Mode = 'DvP' | 'Auction';

const fmt = (n: number) =>
  n.toLocaleString(undefined, { maximumFractionDigits: 4 });
// Price/NAV formatting — always two decimals so the gold ticker reads like a quote.
const fmt2 = (n: number) =>
  n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const sessionLabel = (s: Session) =>
  s === 'Open' ? 'Official Open' : 'Official Close · NAV';

export default function App() {
  const [parties, setParties] = useState<Party[]>([]);
  const [instruments, setInstruments] = useState<Instrument[]>([]);
  const [acting, setActing] = useState<string>(''); // party label
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [receipts, setReceipts] = useState<Receipt[]>([]);

  const [mode, setMode] = useState<Mode>('DvP');
  const [asset, setAsset] = useState<string>('');
  const [side, setSide] = useState<Side>('Buy');
  const [quantity, setQuantity] = useState<string>('1');
  const [price, setPrice] = useState<string>(''); // DvP only
  const [counterparty, setCounterparty] = useState<string>('');
  const [session, setSession] = useState<Session>('Close');

  const [mocState, setMocState] = useState<MocState | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>('');
  const [toast, setToast] = useState<string>('');

  const tradableAssets = useMemo(
    () => instruments.filter((i) => i.kind !== 'Cash'),
    [instruments],
  );
  const tradedParties = useMemo(
    () => parties.filter((p) => p.label.toLowerCase() !== 'sandbox'),
    [parties],
  );
  const instrumentOf = useCallback(
    (id: string) => instruments.find((i) => i.id === id) ?? null,
    [instruments],
  );
  const refPriceOf = useCallback(
    (id: string) => instrumentOf(id)?.referencePrice ?? null,
    [instrumentOf],
  );

  const flash = (msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(''), 4000);
  };

  // ---- loaders ------------------------------------------------------------

  const loadHoldings = useCallback(async (label: string) => {
    if (!label) return;
    try {
      setHoldings(await api.holdings(label));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, []);

  const loadMoc = useCallback(async (assetId: string, sess: Session, actingAs: string) => {
    if (!assetId) return;
    try {
      // Query the book AS the acting party — the ledger filters it (dark pool).
      setMocState(await api.mocState(assetId, sess, actingAs, CASH));
    } catch {
      setMocState(null);
    }
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const [ps, ins] = await Promise.all([api.parties(), api.instruments()]);
        setParties(ps);
        setInstruments(ins);
        const first = ps.find((p) => p.label === 'Alice') ?? ps[0];
        setActing(first?.label ?? '');
        const firstAsset = ins.find((i) => i.kind !== 'Cash');
        setAsset(firstAsset?.id ?? '');
        setPrice(firstAsset?.referencePrice ? String(firstAsset.referencePrice) : '');
        const cp = ps.find((p) => p.label === 'Bob') ?? ps.find((p) => p.label !== first?.label);
        setCounterparty(cp?.label ?? '');
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    })();
  }, []);

  useEffect(() => {
    void loadHoldings(acting);
  }, [acting, loadHoldings]);
  useEffect(() => {
    void loadMoc(asset, session, acting);
  }, [asset, session, acting, loadMoc]);

  // When the asset changes, seed the DvP price with its published reference so the
  // "You pay X" line computes immediately (still editable — DvP is negotiated).
  useEffect(() => {
    const rp = refPriceOf(asset);
    if (rp != null) setPrice(String(rp));
  }, [asset, refPriceOf]);

  // ---- derived position (spot-only framing) -------------------------------

  const positionOf = useCallback(
    (instrumentId: string) =>
      holdings
        .filter((h) => h.instrumentId === instrumentId)
        .reduce((s, h) => s + h.amount, 0),
    [holdings],
  );
  const cashPosition = positionOf(CASH);
  const assetPosition = positionOf(asset);

  const qtyNum = Number(quantity) || 0;
  const priceNum = Number(price) || 0;
  const closePrice = refPriceOf(asset);
  const dvpCash = qtyNum * priceNum;
  const mocCash = qtyNum * (closePrice ?? 0);
  const selectedInstrument = instrumentOf(asset);

  // Spot guard: a Sell must be covered by the asset; a Buy by cash. (The ledger
  // also enforces this — there is no shorting and no negative position.)
  const spotWarning = useMemo(() => {
    if (qtyNum <= 0) return '';
    if (side === 'Sell' && qtyNum > assetPosition + 1e-9)
      return `You hold ${fmt(assetPosition)} ${asset} — cannot sell ${fmt(qtyNum)}.`;
    const needCash = mode === 'DvP' ? dvpCash : mocCash;
    if (side === 'Buy' && needCash > cashPosition + 1e-9)
      return `Costs ${fmt(needCash)} ${CASH} — you hold ${fmt(cashPosition)} ${CASH}.`;
    return '';
  }, [side, qtyNum, assetPosition, asset, mode, dvpCash, mocCash, cashPosition]);

  // ---- actions ------------------------------------------------------------

  async function runAction<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setError('');
    try {
      return await fn();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : e instanceof Error ? e.message : String(e));
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  async function doDvP() {
    if (!acting || !counterparty || !asset) return;
    const buyer = side === 'Buy' ? acting : counterparty;
    const seller = side === 'Buy' ? counterparty : acting;
    const res = await runAction(() =>
      api.trade({
        buyer,
        seller,
        assetInstrument: asset,
        assetAmount: qtyNum,
        cashInstrument: CASH,
        cashAmount: dvpCash,
      }),
    );
    if (!res) return;
    setReceipts((r) => [
      {
        key: `dvp-${res.receiptCid ?? Date.now()}`,
        kind: 'DvP',
        time: new Date().toLocaleTimeString(),
        headline: `${buyer} bought ${fmt(res.assetAmount)} ${res.assetInstrument} from ${seller}`,
        asset: res.assetInstrument,
        quantity: res.assetAmount,
        cashAmount: res.cashAmount,
        unitPrice: res.unitPrice,
        counterpartyLine: `${seller} (seller) ⇄ ${buyer} (buyer) · atomic DvP`,
        cid: res.receiptCid ?? '(no receipt)',
      },
      ...r,
    ]);
    flash('Trade executed — both legs settled atomically.');
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting)]);
  }

  async function doMocOrder() {
    if (!acting || !asset) return;
    const res = await runAction(() =>
      api.mocOrder({ trader: acting, side, quantity: qtyNum, instrumentId: asset, session }),
    );
    if (!res) return;
    flash(
      `Sealed ${side.toUpperCase()} order sent to the ${session.toLowerCase()} cross ` +
        `(crosses at ${fmt2(res.closingPrice)} ${CASH}).`,
    );
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting)]);
  }

  async function doRunClose() {
    if (!mocState?.auctionCid) return;
    const auctionCid = mocState.auctionCid;
    const res = await runAction(() => api.mocClose(auctionCid));
    if (!res) return;
    const kind = res.session === 'Open' ? 'Open' : 'Close';
    const crossName = res.session === 'Open' ? 'opening cross' : 'closing cross';
    setReceipts((r) => [
      ...res.fills.map((f, idx) => ({
        key: `x-${res.settlementBatchCid}-${idx}`,
        kind: kind as 'Open' | 'Close',
        time: new Date().toLocaleTimeString(),
        headline: `${f.trader} ${f.side === 'Buy' ? 'bought' : 'sold'} ${fmt(f.quantity)} ${mocState.instrumentId}`,
        asset: mocState.instrumentId,
        quantity: f.quantity,
        cashAmount: f.quantity * f.price,
        unitPrice: f.price,
        counterpartyLine: `Uniform-price ${crossName} · venue-matched at the ${sessionLabel(kind)}`,
        cid: res.settlementBatchCid,
      })),
      ...r,
    ]);
    flash(
      `${kind === 'Open' ? 'Opening' : 'Closing'} cross printed ${res.fills.length} fill(s) ` +
        `at ${fmt2(res.closingPrice)} ${CASH}.`,
    );
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting)]);
  }

  async function doWithdraw(orderCid: string) {
    const res = await runAction(() => api.withdrawOrder(orderCid, acting));
    if (!res) return;
    flash('Order withdrawn — your reserved balance is unlocked.');
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting)]);
  }

  async function doClearBook() {
    if (!mocState?.auctionCid) return;
    const res = await runAction(() => api.clearBook(asset, session, CASH));
    if (!res) return;
    flash(`Book cleared — ${res.cleared} resting order(s) cancelled.`);
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting)]);
  }

  const actingIsVenue = acting.toLowerCase() === 'venue';
  const canDvP = !busy && qtyNum > 0 && priceNum > 0 && !!counterparty && counterparty !== acting;
  const canMoc = !busy && qtyNum > 0 && !!asset;

  // ---- render -------------------------------------------------------------

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo" aria-hidden>◈</span>
          <div className="brand-text">
            <span className="brand-name">CANTON DvP DESK</span>
            <span className="brand-sub">Delivery-versus-Payment · Sealed Opening &amp; Closing Cross</span>
          </div>
        </div>
        <div className="topbar-right">
          <span className="live" title="Connected to the local Canton ledger API">
            <span className="dot" /> live · ledger localhost:6900
          </span>
          <label className="party-switch">
            <span>Acting as</span>
            <select value={acting} onChange={(e) => setActing(e.target.value)}>
              {tradedParties.map((p) => (
                <option key={p.party} value={p.label}>
                  {p.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </header>

      {error && (
        <div className="banner error" onClick={() => setError('')}>
          <span>⚠ {error}</span> <span className="dismiss">dismiss</span>
        </div>
      )}
      {toast && <div className="banner ok">✓ {toast}</div>}

      {/* -------- Official Open / Close · NAV quote card -------- */}
      <section className="quote" aria-label="Official price quote">
        <div className="quote-left">
          <div className="quote-instrument">
            <span className={`pill ${asset === CASH ? 'cash' : 'asset'}`}>{asset || '—'}</span>
            {selectedInstrument && <span className="quote-kind">{selectedInstrument.kind}</span>}
          </div>
          <p className="quote-desc">{selectedInstrument?.description ?? 'Select an instrument'}</p>
        </div>
        <div className="quote-right">
          <span className="quote-label">{sessionLabel(session)}</span>
          <span className="quote-price">
            {closePrice != null ? (
              <>
                <span className="mono nav">{fmt2(closePrice)}</span>
                <span className="quote-ccy">{CASH}</span>
              </>
            ) : (
              <span className="mono nav muted">—</span>
            )}
          </span>
          <span className="quote-note">Exogenous reference · every fill prints here</span>
        </div>
      </section>

      <main className="grid">
        {/* -------- Position / holdings -------- */}
        <section className="card position">
          <div className="card-head">
            <h2>Position</h2>
            <span className="who">{acting}</span>
          </div>
          <p className="hint">Holdings on the ledger — spot only, no shorting.</p>
          {holdings.length === 0 ? (
            <p className="empty">No holdings.</p>
          ) : (
            <table className="blotter">
              <thead>
                <tr>
                  <th>Instrument</th>
                  <th className="num">Amount</th>
                  <th className="num">Value ({CASH})</th>
                </tr>
              </thead>
              <tbody>
                {aggregate(holdings).map((h) => {
                  const rp = refPriceOf(h.instrumentId);
                  const val = h.instrumentId === CASH ? h.amount : rp != null ? h.amount * rp : null;
                  return (
                    <tr key={h.instrumentId}>
                      <td>
                        <span className={`pill ${h.instrumentId === CASH ? 'cash' : 'asset'}`}>
                          {h.instrumentId}
                        </span>
                      </td>
                      <td className="num mono strong">{fmt(h.amount)}</td>
                      <td className="num mono muted">{val != null ? fmt2(val) : '—'}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
          <button className="ghost" disabled={busy} onClick={() => loadHoldings(acting)}>
            Refresh
          </button>
        </section>

        {/* -------- Trade panel -------- */}
        <section className="card trade">
          <div className="card-head">
            <h2>Trade</h2>
          </div>

          <div className="row">
            <label className="field">
              <span>Asset</span>
              <select value={asset} onChange={(e) => setAsset(e.target.value)}>
                {tradableAssets.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.id}
                  </option>
                ))}
              </select>
            </label>
            <div className="field">
              <span>Side</span>
              <div className="segmented">
                <button className={side === 'Buy' ? 'on buy' : ''} onClick={() => setSide('Buy')}>
                  Buy
                </button>
                <button className={side === 'Sell' ? 'on sell' : ''} onClick={() => setSide('Sell')}>
                  Sell
                </button>
              </div>
            </div>
            <label className="field small">
              <span>Quantity</span>
              <input
                type="number"
                min="0"
                step="any"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
              />
            </label>
          </div>

          <div className="mode-toggle">
            <button className={mode === 'DvP' ? 'on' : ''} onClick={() => setMode('DvP')}>
              Settle now · DvP
            </button>
            <button className={mode === 'Auction' ? 'on' : ''} onClick={() => setMode('Auction')}>
              Send to Auction
            </button>
          </div>

          {mode === 'DvP' ? (
            <>
              <p className="hint">
                <strong>DvP</strong> — an agreed bilateral atomic swap with a named counterparty.
                Both legs move in one transaction, or neither does.
              </p>
              <div className="row">
                <label className="field">
                  <span>Counterparty</span>
                  <select value={counterparty} onChange={(e) => setCounterparty(e.target.value)}>
                    {tradedParties
                      .filter((p) => p.label !== acting)
                      .map((p) => (
                        <option key={p.party} value={p.label}>
                          {p.label}
                        </option>
                      ))}
                  </select>
                </label>
                <label className="field small">
                  <span>Price ({CASH})</span>
                  <input
                    className="mono"
                    type="number"
                    min="0"
                    step="any"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                  />
                </label>
              </div>
              <div className="summary">
                <span>{side === 'Buy' ? 'You pay' : 'You receive'}</span>
                <strong className="mono">{fmt2(dvpCash)} {CASH}</strong>
                <span className="summary-sub mono">
                  {fmt(qtyNum)} {asset} @ {fmt2(priceNum)}
                </span>
              </div>
              {spotWarning && <div className="warn">{spotWarning}</div>}
              <button className="primary" disabled={!canDvP} onClick={doDvP}>
                {busy ? 'Settling…' : `${side} ${fmt(qtyNum)} ${asset} · Settle now (DvP)`}
              </button>
            </>
          ) : (
            <>
              <p className="hint">
                <strong>Auction</strong> — an anonymous sealed order that crosses at the venue&rsquo;s
                official price. No counterparty and no price you set — the cross price is what it is.
              </p>
              <div className="field">
                <span>Session</span>
                <div className="segmented session">
                  <button className={session === 'Open' ? 'on' : ''} onClick={() => setSession('Open')}>
                    Opening (MOO)
                  </button>
                  <button className={session === 'Close' ? 'on' : ''} onClick={() => setSession('Close')}>
                    Closing (MOC)
                  </button>
                </div>
              </div>
              <div className="summary">
                <span>Crosses at the {sessionLabel(session)}</span>
                <strong className="mono nav-inline">
                  {closePrice != null ? `${fmt2(closePrice)} ${CASH}` : '—'}
                </strong>
                {closePrice != null && (
                  <span className="summary-sub mono">
                    {side === 'Buy'
                      ? `commits ${fmt2(mocCash)} ${CASH}`
                      : `delivers ${fmt(qtyNum)} ${asset}`}
                  </span>
                )}
              </div>
              {spotWarning && <div className="warn">{spotWarning}</div>}
              <button className="primary" disabled={!canMoc} onClick={doMocOrder}>
                {busy
                  ? 'Sending…'
                  : `Send ${side.toUpperCase()} ${fmt(qtyNum)} ${asset} to ${session} Cross`}
              </button>
            </>
          )}
        </section>

        {/* -------- Auction / Cross panel (venue view) -------- */}
        <section className="card cross">
          <div className="card-head">
            <h2>The Cross</h2>
            <span className={`session-tag ${session.toLowerCase()}`}>{session}</span>
          </div>
          <p className="hint">
            The venue&rsquo;s sealed call auction for <strong>{asset}</strong>. Orders rest privately —
            {actingIsVenue ? (
              <> as <strong>Venue</strong> you see the FULL book and run the cross.</>
            ) : (
              <> as <strong>{acting}</strong> you see ONLY your own resting orders (the ledger hides rivals&rsquo;).</>
            )}
          </p>
          {mocState?.auctionCid ? (
            <>
              <div className="cross-meta">
                <span className="pill asset">{mocState.instrumentId}</span>
                <span className="cross-meta-price">
                  @ <strong className="mono">{mocState.referencePrice != null ? fmt2(mocState.referencePrice) : '—'}</strong> {CASH}
                </span>
                <span className="tag">
                  {actingIsVenue
                    ? `${mocState.orders.length} resting`
                    : `${mocState.orders.length} yours`}
                </span>
                {!actingIsVenue && mocState.othersResting > 0 && (
                  <span className="tag muted" title="Sealed — hidden from you by the ledger">
                    sealed · {mocState.othersResting} other{mocState.othersResting === 1 ? '' : 's'} hidden
                  </span>
                )}
              </div>
              {mocState.orders.length > 0 ? (
                <table className="blotter orders">
                  <thead>
                    <tr>
                      <th>Trader</th>
                      <th>Side</th>
                      <th className="num">Qty</th>
                      <th className="num">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mocState.orders.map((o) => (
                      <tr key={o.contractId}>
                        <td>{o.trader}</td>
                        <td>
                          <span className={`side ${o.side.toLowerCase()}`}>{o.side}</span>
                        </td>
                        <td className="num mono">{fmt(o.quantity)}</td>
                        <td className="num">
                          {o.trader === acting ? (
                            <button
                              className="ghost small"
                              disabled={busy}
                              onClick={() => doWithdraw(o.contractId)}
                            >
                              Withdraw
                            </button>
                          ) : (
                            <span className="muted">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="empty">
                  {actingIsVenue
                    ? 'No orders resting yet.'
                    : 'You have no resting orders in this cross.'}
                </p>
              )}
              {!actingIsVenue && (
                <p className="warn subtle">Switch to <strong>Venue</strong> to see the full book and run the cross.</p>
              )}
              {actingIsVenue && (
                <div className="row">
                  <button
                    className="primary venue"
                    disabled={busy || mocState.orders.length === 0}
                    onClick={doRunClose}
                  >
                    {busy ? 'Crossing…' : `Run the ${session} Cross`}
                  </button>
                  <button
                    className="ghost"
                    disabled={busy || mocState.orders.length === 0}
                    onClick={doClearBook}
                    title="Cancel every resting order for this instrument/session"
                  >
                    Clear book
                  </button>
                </div>
              )}
            </>
          ) : (
            <p className="empty">
              No open {session.toLowerCase()} auction for {asset}. Send an order to open one.
            </p>
          )}
          <button className="ghost" disabled={busy} onClick={() => loadMoc(asset, session, acting)}>
            Refresh
          </button>
        </section>

        {/* -------- Receipts -------- */}
        <section className="card receipts">
          <div className="card-head">
            <h2>Settlement Receipts</h2>
            <span className="who">{receipts.length} on-ledger</span>
          </div>
          <p className="hint">Immutable proof written on-ledger inside each atomic settlement.</p>
          {receipts.length === 0 ? (
            <p className="empty">No settlements yet. Execute a trade to see its receipt.</p>
          ) : (
            <ul className="receipt-list">
              {receipts.map((r) => (
                <li key={r.key} className={`receipt ${r.kind.toLowerCase()}`}>
                  <div className="receipt-head">
                    <span className={`badge ${r.kind.toLowerCase()}`}>
                      {r.kind === 'DvP' ? 'DvP' : r.kind === 'Open' ? 'OPEN' : 'CLOSE'}
                    </span>
                    <span className="receipt-headline">{r.headline}</span>
                    <span className="receipt-time mono">{r.time}</span>
                  </div>
                  <div className="receipt-body">
                    <span className="mono">
                      {fmt(r.quantity)} {r.asset} @ <strong>{fmt2(r.unitPrice)}</strong> ={' '}
                      <strong>{fmt2(r.cashAmount)} {CASH}</strong>
                    </span>
                    <span className="cp">{r.counterpartyLine}</span>
                    <code className="cid mono" title={r.cid}>
                      {r.kind === 'DvP' ? 'receipt' : 'batch'} · {shortCid(r.cid)}
                    </code>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>

      <footer className="foot">
        Live against a local Canton sandbox via the Daml Java bindings · cash{' '}
        <code>USDC</code> · assets <code>DEMO:AAPL</code> <code>cETH</code> <code>CBTC</code> ·
        contract-id plumbing auto-resolved server-side.
      </footer>
    </div>
  );
}

// Sum holdings per instrument so the position shows one row per token.
function aggregate(holdings: Holding[]): { instrumentId: string; amount: number }[] {
  const m = new Map<string, number>();
  for (const h of holdings) m.set(h.instrumentId, (m.get(h.instrumentId) ?? 0) + h.amount);
  return [...m.entries()]
    .map(([instrumentId, amount]) => ({ instrumentId, amount }))
    .sort((a, b) => a.instrumentId.localeCompare(b.instrumentId));
}

function shortCid(cid: string): string {
  return cid.length > 18 ? `${cid.slice(0, 10)}…${cid.slice(-6)}` : cid;
}
