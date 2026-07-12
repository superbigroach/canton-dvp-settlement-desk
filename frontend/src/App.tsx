import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  api,
  ApiError,
  type Holding,
  type Instrument,
  type MocState,
  type Party,
} from './api';

const CASH = 'USDC';

// A settlement proof we show the trader: either a bilateral DvP receipt or one
// fill from a Market-on-Close batch. Accumulated newest-first.
interface Receipt {
  key: string;
  kind: 'DvP' | 'MOC';
  time: string;
  headline: string; // "Bob bought 3 DEMO:AAPL" etc.
  asset: string;
  quantity: number;
  cashAmount: number;
  unitPrice: number;
  counterpartyLine: string;
  cid: string; // receipt cid (DvP) or settlement-batch cid (MOC)
}

type Side = 'Buy' | 'Sell';
type Mode = 'DvP' | 'MOC';

const fmt = (n: number) =>
  n.toLocaleString(undefined, { maximumFractionDigits: 4 });

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
  const refPriceOf = useCallback(
    (id: string) => instruments.find((i) => i.id === id)?.referencePrice ?? null,
    [instruments],
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

  const loadMoc = useCallback(async (assetId: string) => {
    if (!assetId) return;
    try {
      setMocState(await api.mocState(assetId, CASH));
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
    void loadMoc(asset);
  }, [asset, loadMoc]);

  // When the asset changes, seed the DvP price with its published reference.
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
    await Promise.all([loadHoldings(acting), loadMoc(asset)]);
  }

  async function doMocOrder() {
    if (!acting || !asset) return;
    const res = await runAction(() =>
      api.mocOrder({ trader: acting, side, quantity: qtyNum, instrumentId: asset }),
    );
    if (!res) return;
    flash(
      `Sealed ${side.toUpperCase()} order sent to the close ` +
        `(crosses at ${fmt(res.closingPrice)} ${CASH}).`,
    );
    await Promise.all([loadHoldings(acting), loadMoc(asset)]);
  }

  async function doRunClose() {
    if (!mocState?.auctionCid) return;
    const auctionCid = mocState.auctionCid;
    const res = await runAction(() => api.mocClose(auctionCid));
    if (!res) return;
    setReceipts((r) => [
      ...res.fills.map((f, idx) => ({
        key: `moc-${res.settlementBatchCid}-${idx}`,
        kind: 'MOC' as const,
        time: new Date().toLocaleTimeString(),
        headline: `${f.trader} ${f.side === 'Buy' ? 'bought' : 'sold'} ${fmt(f.quantity)} ${mocState.instrumentId}`,
        asset: mocState.instrumentId,
        quantity: f.quantity,
        cashAmount: f.quantity * f.price,
        unitPrice: f.price,
        counterpartyLine: `Market-on-Close cross · venue-matched at the uniform close`,
        cid: res.settlementBatchCid,
      })),
      ...r,
    ]);
    flash(`Close crossed ${res.fills.length} fill(s) at ${fmt(res.closingPrice)} ${CASH}.`);
    await Promise.all([loadHoldings(acting), loadMoc(asset)]);
  }

  const actingIsVenue = acting.toLowerCase() === 'venue';
  const canDvP = !busy && qtyNum > 0 && priceNum > 0 && !!counterparty && counterparty !== acting;
  const canMoc = !busy && qtyNum > 0 && !!asset;

  // ---- render -------------------------------------------------------------

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="logo">◈</span>
          <div>
            <h1>Canton DvP Settlement Desk</h1>
            <p className="tagline">Atomic delivery-versus-payment &amp; sealed Market-on-Close · USDC cash on Canton</p>
          </div>
        </div>
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
      </header>

      {error && (
        <div className="banner error" onClick={() => setError('')}>
          ⚠ {error} <span className="dismiss">(dismiss)</span>
        </div>
      )}
      {toast && <div className="banner ok">✓ {toast}</div>}

      <main className="grid">
        {/* -------- Position / holdings -------- */}
        <section className="card position">
          <h2>{acting}&rsquo;s position</h2>
          <p className="hint">Your current holdings on the ledger — spot only, no shorting.</p>
          {holdings.length === 0 ? (
            <p className="empty">No holdings.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Instrument</th>
                  <th className="num">Amount</th>
                </tr>
              </thead>
              <tbody>
                {aggregate(holdings).map((h) => (
                  <tr key={h.instrumentId}>
                    <td>
                      <span className={`pill ${h.instrumentId === CASH ? 'cash' : 'asset'}`}>
                        {h.instrumentId}
                      </span>
                    </td>
                    <td className="num strong">{fmt(h.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <button className="ghost" disabled={busy} onClick={() => loadHoldings(acting)}>
            Refresh
          </button>
        </section>

        {/* -------- Trade panel -------- */}
        <section className="card trade">
          <h2>Trade</h2>

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
              Settle now (DvP)
            </button>
            <button className={mode === 'MOC' ? 'on' : ''} onClick={() => setMode('MOC')}>
              Send to Close (MOC)
            </button>
          </div>

          {mode === 'DvP' ? (
            <>
              <p className="hint">
                <strong>DvP</strong> = an agreed bilateral atomic swap with a known counterparty.
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
                    type="number"
                    min="0"
                    step="any"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                  />
                </label>
              </div>
              <div className="summary">
                {side === 'Buy' ? 'You pay' : 'You receive'} <strong>{fmt(dvpCash)} {CASH}</strong>{' '}
                for {fmt(qtyNum)} {asset} @ {fmt(priceNum)}
              </div>
              {spotWarning && <div className="warn">{spotWarning}</div>}
              <button className="primary" disabled={!canDvP} onClick={doDvP}>
                {busy ? 'Settling…' : `${side} ${fmt(qtyNum)} ${asset} · Settle now`}
              </button>
            </>
          ) : (
            <>
              <p className="hint">
                <strong>MOC</strong> = an anonymous sealed order that crosses at the venue&rsquo;s
                official close. No counterparty, no price you set — the close price is what it is.
              </p>
              <div className="summary">
                Crosses at the published close{' '}
                <strong>{closePrice != null ? `${fmt(closePrice)} ${CASH}` : '—'}</strong>
                {closePrice != null && (
                  <>
                    {' '}· {side === 'Buy' ? 'commits' : 'delivers'}{' '}
                    <strong>
                      {side === 'Buy' ? `${fmt(mocCash)} ${CASH}` : `${fmt(qtyNum)} ${asset}`}
                    </strong>
                  </>
                )}
              </div>
              {spotWarning && <div className="warn">{spotWarning}</div>}
              <button className="primary" disabled={!canMoc} onClick={doMocOrder}>
                {busy ? 'Sending…' : `Send ${side.toUpperCase()} ${fmt(qtyNum)} ${asset} to Close`}
              </button>
            </>
          )}
        </section>

        {/* -------- Close panel (venue view) -------- */}
        <section className="card close">
          <h2>The Close · {asset}</h2>
          <p className="hint">
            The venue&rsquo;s call auction. Sealed orders rest privately, then cross together at the
            uniform close price. Run it as <strong>Venue</strong>.
          </p>
          {mocState?.auctionCid ? (
            <>
              <div className="close-meta">
                <span className="pill asset">{mocState.instrumentId}</span>
                <span>
                  close @ <strong>{mocState.referencePrice != null ? fmt(mocState.referencePrice) : '—'} {CASH}</strong>
                </span>
                <span className="tag open">{mocState.orders.length} resting order(s)</span>
              </div>
              {mocState.orders.length > 0 && (
                <table className="orders">
                  <thead>
                    <tr>
                      <th>Trader</th>
                      <th>Side</th>
                      <th className="num">Qty</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mocState.orders.map((o) => (
                      <tr key={o.contractId}>
                        <td>{o.trader}</td>
                        <td>
                          <span className={`side ${o.side.toLowerCase()}`}>{o.side}</span>
                        </td>
                        <td className="num">{fmt(o.quantity)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {!actingIsVenue && (
                <p className="warn subtle">Switch to <strong>Venue</strong> to run the close.</p>
              )}
              <button
                className="primary venue"
                disabled={busy || !actingIsVenue || mocState.orders.length === 0}
                onClick={doRunClose}
              >
                {busy ? 'Crossing…' : 'Run the Close'}
              </button>
            </>
          ) : (
            <p className="empty">No open auction for {asset}. Send a MOC order to open one.</p>
          )}
          <button className="ghost" disabled={busy} onClick={() => loadMoc(asset)}>
            Refresh
          </button>
        </section>

        {/* -------- Receipts -------- */}
        <section className="card receipts">
          <h2>Settlement receipts</h2>
          <p className="hint">Immutable proof written on-ledger inside each atomic settlement.</p>
          {receipts.length === 0 ? (
            <p className="empty">No settlements yet. Execute a trade to see its receipt.</p>
          ) : (
            <ul className="receipt-list">
              {receipts.map((r) => (
                <li key={r.key} className={`receipt ${r.kind.toLowerCase()}`}>
                  <div className="receipt-head">
                    <span className={`badge ${r.kind.toLowerCase()}`}>{r.kind}</span>
                    <span className="receipt-headline">{r.headline}</span>
                    <span className="receipt-time">{r.time}</span>
                  </div>
                  <div className="receipt-body">
                    <span>
                      {fmt(r.quantity)} {r.asset} @ <strong>{fmt(r.unitPrice)}</strong> ={' '}
                      <strong>{fmt(r.cashAmount)} {CASH}</strong>
                    </span>
                    <span className="cp">{r.counterpartyLine}</span>
                    <code className="cid" title={r.cid}>
                      {r.kind === 'DvP' ? 'receipt' : 'batch'}: {shortCid(r.cid)}
                    </code>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>

      <footer className="foot">
        Live against a local Canton sandbox via the Daml Java bindings. Cash token{' '}
        <code>USDC</code> · assets <code>DEMO:AAPL</code>, <code>cETH</code>. Contract-id plumbing is
        auto-resolved server-side.
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
