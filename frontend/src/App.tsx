import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  api,
  errorMessage,
  type Holding,
  type Instrument,
  type LedgerReceipt,
  type LiquidityMandate,
  type MandateTerms,
  type MocImbalance,
  type MocState,
  type OrderType,
  type Party,
  type Session,
} from './api';
import CommitteePanel from './CommitteePanel';
import FundPanel from './FundPanel';
import PendingTransfersPanel from './PendingTransfersPanel';

const CASH = 'USDC';

type Side = 'Buy' | 'Sell';
type Mode = 'DvP' | 'Auction';

// ---- The venue price collar (MIRRORED from daml/MarketOnClose.daml) ---------
//
// Kept in step with `collarBps` / `collarFloor` / `collarBand` on the ledger. The UI
// needs them for ONE thing: an unpriced BUY's buying power. A market order has no
// limit of its own, so the only bound on what it can be asked to pay is the collar —
// RunClose clamps the print into anchor ± collarBand(anchor) and cannot settle
// outside it. Nasdaq's construction: the GREATER of a percentage and an absolute
// floor, because a pure percentage is meaningless on a low-priced instrument.
const COLLAR_BPS = 1000;
const COLLAR_FLOOR = 0.5;
const collarBand = (anchor: number) => Math.max(COLLAR_FLOOR, (anchor * COLLAR_BPS) / 10000);
/** The HIGHEST price this auction can legally print — an unpriced buy's worst case. */
const collarHigh = (anchor: number) => anchor + collarBand(anchor);

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
  // Receipts VISIBLE to the acting party — a real per-party ledger query (need-to-know audit),
  // so switching parties shows exactly what that party is entitled to see (Eve sees nothing).
  const [ledgerReceipts, setLedgerReceipts] = useState<LedgerReceipt[]>([]);

  const [mode] = useState<Mode>('Auction'); // auction-only desk (DvP engine still powers the cross)
  const [asset, setAsset] = useState<string>('');
  const [side, setSide] = useState<Side>('Buy');
  const [quantity, setQuantity] = useState<string>('1');
  const [price, setPrice] = useState<string>(''); // DvP only
  // Auction only. THE ORDER TYPE.
  //   Market — unpriced market-on-close. No limit at all: it takes whatever the book
  //            prints, counts at every candidate price, and is allocated AHEAD of
  //            every limit order. This is what the overwhelming majority of real
  //            closing volume is (index funds and rebalancers whose mandate IS the
  //            official print), so it is the default.
  //   Limit  — limit-on-close at a stated worst price (Buy: max, Sell: min). Limits
  //            set AWAY from the anchor are what let the uncross discover a different
  //            print — that is the whole point of price discovery.
  const [orderType, setOrderType] = useState<OrderType>('Market');
  const [limitPrice, setLimitPrice] = useState<string>('');
  const [counterparty, setCounterparty] = useState<string>('');
  const [session, setSession] = useState<Session>('Close');

  const [mocState, setMocState] = useState<MocState | null>(null);
  const [imbalance, setImbalance] = useState<MocImbalance | null>(null);
  // THE SEAT. `mandate` is the acting party's own signed obligation over this book
  // (null / held=false when it holds none); `terms` are the venue's OPEN offers of
  // that seat. A party with no mandate is shown the terms instead of the imbalance —
  // it must commit before it can see, and it CAN commit, which is the whole point.
  const [mandate, setMandate] = useState<LiquidityMandate | null>(null);
  const [terms, setTerms] = useState<MandateTerms[]>([]);
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
      setError(errorMessage(e));
    }
  }, []);

  // Load the receipts the acting party is entitled to see — a real per-party ledger query.
  const loadReceipts = useCallback(async (label: string) => {
    if (!label) return;
    try {
      setLedgerReceipts(await api.receiptsFor(label));
    } catch {
      setLedgerReceipts([]);
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

  // The MANDATED-PROVIDER view of the NET imbalance. The ledger discloses it only to
  // a party holding a live LiquidityMandate over this book (and to the venue); anyone
  // else gets disclosed=false with mandateRequired=true, which the panel renders as
  // the open terms and an Accept action rather than as a refusal.
  const loadImbalance = useCallback(async (assetId: string, sess: Session, actingAs: string) => {
    if (!assetId || !actingAs) {
      setImbalance(null);
      return;
    }
    try {
      setImbalance(await api.imbalance(assetId, sess, actingAs, CASH));
    } catch {
      setImbalance(null);
    }
  }, []);

  // The seat itself: what this party has committed to (if anything), and what the
  // venue is currently offering anyone who wants to commit.
  const loadMandate = useCallback(async (assetId: string, sess: Session, actingAs: string) => {
    if (!assetId || !actingAs) {
      setMandate(null);
      setTerms([]);
      return;
    }
    const [m, t] = await Promise.all([
      api.myMandate(assetId, sess, actingAs, CASH).catch(() => null),
      api.mandateTerms(assetId, sess, actingAs, CASH).catch(() => [] as MandateTerms[]),
    ]);
    setMandate(m);
    setTerms(t);
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
        setError(errorMessage(e));
      }
    })();
  }, []);

  useEffect(() => {
    void loadHoldings(acting);
  }, [acting, loadHoldings]);
  useEffect(() => {
    void loadReceipts(acting);
  }, [acting, loadReceipts]);
  useEffect(() => {
    void loadMoc(asset, session, acting);
  }, [asset, session, acting, loadMoc]);
  useEffect(() => {
    void loadImbalance(asset, session, acting);
  }, [asset, session, acting, loadImbalance]);
  useEffect(() => {
    void loadMandate(asset, session, acting);
  }, [asset, session, acting, loadMandate]);

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
  // BUYING POWER — quantity * the worst price the order can LEGALLY execute at.
  // This must match the ledger's own reservation exactly or the ticket rejects orders
  // the venue would have accepted (and vice versa):
  //   * LIMIT buy  -> quantity * limit. A buy can never execute above its own limit,
  //     so the limit is the exact worst case. NOT the anchor — the cross is discovered
  //     from the book and can print above it.
  //   * MARKET buy -> quantity * collarHigh(anchor), the TOP OF THE PRICE COLLAR. An
  //     unpriced buy has no limit, so the collar is the only bound on what it can be
  //     asked to pay. Sizing a market order at the ANCHOR would under-state the
  //     requirement by the collar's whole width and wrongly reject fundable orders.
  // A SELL is price-independent either way: it delivers `quantity` of the asset.
  const isMarketOrder = orderType === 'Market';
  const limitNum = Number(limitPrice) || 0;
  const anchor = closePrice ?? 0;
  // The worst price this ticket can be filled at, and the number the summary quotes.
  const worstPrice = isMarketOrder ? collarHigh(anchor) : limitNum;
  const mocCash = qtyNum * worstPrice;
  const selectedInstrument = instrumentOf(asset);

  // Spot guard: a Sell must be covered by the asset; a Buy by cash. (The ledger
  // also enforces this — there is no shorting and no negative position.)
  //
  // `mocCash` is already the collar-aware figure for a MARKET buy, so this check
  // agrees with the ledger's own reservation instead of under-stating it at the
  // anchor and letting the ticket through only for SubmitOrder to abort.
  const spotWarning = useMemo(() => {
    if (qtyNum <= 0) return '';
    if (side === 'Sell' && qtyNum > assetPosition + 1e-9)
      return `You hold ${fmt(assetPosition)} ${asset} — cannot sell ${fmt(qtyNum)}.`;
    const needCash = mode === 'DvP' ? dvpCash : mocCash;
    if (side === 'Buy' && needCash > cashPosition + 1e-9)
      return mode === 'Auction' && isMarketOrder
        ? `An unpriced buy reserves ${fmt(needCash)} ${CASH} (the collar ceiling, refunded down ` +
            `to the print) — you hold ${fmt(cashPosition)} ${CASH}.`
        : `Costs ${fmt(needCash)} ${CASH} — you hold ${fmt(cashPosition)} ${CASH}.`;
    return '';
  }, [side, qtyNum, assetPosition, asset, mode, dvpCash, mocCash, cashPosition, isMarketOrder]);

  // ---- actions ------------------------------------------------------------

  async function runAction<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setError('');
    try {
      return await fn();
    } catch (e) {
      // ONE formatter for every failure path — see errorMessage() in api.ts.
      setError(errorMessage(e));
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
    flash('Trade executed — both legs settled atomically.');
    await Promise.all([loadHoldings(acting), loadReceipts(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  async function doMocOrder() {
    if (!acting || !asset) return;
    const res = await runAction(() =>
      api.mocOrder({
        trader: acting,
        side,
        quantity: qtyNum,
        instrumentId: asset,
        session,
        orderType,
        // A MARKET order carries NO limit at all — the field is omitted, not zeroed
        // and not defaulted to the anchor. Omitting it is what makes it unpriced.
        ...(isMarketOrder ? {} : { limitPrice: limitNum }),
      }),
    );
    if (!res) return;
    flash(
      isMarketOrder
        ? `Sealed MARKET ${side.toUpperCase()} order sent to the ${session.toLowerCase()} cross ` +
            `— unpriced, filled ahead of every limit order at whatever the book prints.`
        : `Sealed LIMIT ${side.toUpperCase()} order sent to the ${session.toLowerCase()} cross ` +
            `(limit ${fmt2(limitNum)} ${CASH} — the print is discovered at the cross).`,
    );
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  async function doRunClose() {
    if (!mocState?.auctionCid) return;
    const auctionCid = mocState.auctionCid;
    const res = await runAction(() => api.mocClose(auctionCid));
    if (!res) return;
    flash(
      `${res.session === 'Open' ? 'Opening' : 'Closing'} cross printed ${res.fills.length} fill(s) ` +
        `at the discovered price of ${fmt2(res.closingPrice)} ${CASH}.`,
    );
    await Promise.all([loadHoldings(acting), loadReceipts(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  async function doWithdraw(orderCid: string) {
    const res = await runAction(() => api.withdrawOrder(orderCid, acting));
    if (!res) return;
    flash('Order withdrawn — your reserved balance is unlocked.');
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  async function doClearBook() {
    if (!mocState?.auctionCid) return;
    const res = await runAction(() => api.clearBook(asset, session, CASH));
    if (!res) return;
    flash(`Book cleared — ${res.cleared} resting order(s) cancelled.`);
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  // OFFSET — the designated LP clears the disclosed imbalance by lodging a sealed
  // order on the OPPOSITE side for the net quantity (net BUY book → LP SELLs, and
  // vice versa). That order is as private as any other; it simply makes the cross
  // balance so it prints in full.
  async function doOffset() {
    if (!imbalance?.disclosed || !imbalance.netSide || !imbalance.netQuantity) return;
    const offsetSide: Side = imbalance.netSide === 'Buy' ? 'Sell' : 'Buy';
    const qty = imbalance.netQuantity;
    // Unpriced by design: the LP is offsetting the imbalance at whatever the book
    // prints, not expressing a view on price.
    const res = await runAction(() =>
      api.mocOrder({
        trader: acting,
        side: offsetSide,
        quantity: qty,
        instrumentId: asset,
        session,
        orderType: 'Market',
      }),
    );
    if (!res) return;
    flash(
      `Offset sent — sealed ${offsetSide.toUpperCase()} ${fmt(qty)} ${asset} to clear the ` +
        `net ${imbalance.netSide.toLowerCase()} imbalance.`,
    );
    await Promise.all([loadHoldings(acting), loadMoc(asset, session, acting), loadImbalance(asset, session, acting)]);
  }

  const actingIsVenue = acting.toLowerCase() === 'venue';
  // THE SEAT, AND WHO IS IN IT.
  //
  // The panel used to key off a single designated party named in a field. It now keys
  // off a SIGNED OBLIGATION — and, crucially, it also shows to a party that holds no
  // mandate but COULD take one, because "you are not the DLP" was a dead end and
  // "here are the terms, take them" is the contestability made visible.
  const holdsMandate = !!mandate?.held;
  const openTerms = terms.find((t) => t.openToActingParty) ?? null;
  const showLpPanel = !actingIsVenue && (holdsMandate || !!openTerms);
  const isMandatedLp = holdsMandate && !!imbalance?.disclosed;
  const hasImbalance = isMandatedLp && imbalance?.netSide != null && imbalance.netSide !== 'Flat';

  // ACCEPT THE MANDATE — take up the posted seat and become an obligated provider.
  //
  // Deliberately available to a party that can see NOTHING about the book: the terms
  // state the commitment and the band, and that is all anyone gets to know before
  // committing. Commit first, see second — there is no read-then-decide, and no
  // separate application, capital test or fee between a participant and the seat.
  async function doAcceptMandate() {
    if (!openTerms) return;
    const seat = openTerms;
    const res = await runAction(() =>
      api.acceptMandate({
        provider: acting,
        instrumentId: asset,
        session,
        cashInstrument: CASH,
        termsCid: seat.contractId,
      }),
    );
    if (!res) return;
    flash(
      `Mandate accepted — ${acting} is now obligated to absorb up to ` +
        `${fmt(seat.commitmentSize)} ${asset} within ${seat.maxBandBps}bps ` +
        `of ${fmt2(seat.anchorPrice)}. The residual is now visible to you.`,
    );
    await Promise.all([
      loadMandate(asset, session, acting),
      loadImbalance(asset, session, acting),
    ]);
  }
  const canDvP = !busy && qtyNum > 0 && priceNum > 0 && !!counterparty && counterparty !== acting;
  // A LIMIT order is only well-formed once it names a limit; a MARKET order never
  // needs one (that is the point) and is always ready to send.
  const canMoc = !busy && qtyNum > 0 && !!asset && (isMarketOrder || limitNum > 0);

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
          <span className="live" title="Connected to the Canton Ledger API">
            <span className="dot" /> live · {window.location.hostname === 'localhost'
              ? 'local sandbox ledger'
              : 'Canton devnet · hackcanton-01'}
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
          <span className="quote-note">Published anchor · the cross is discovered from the book</span>
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
                <strong>Auction</strong> — an anonymous sealed order. No counterparty, and no price
                the venue gets to hand down: at the cross the ledger <strong>uncrosses the sealed
                book</strong> and prints the one price that trades the most, so every fill settles
                at a price the orders themselves support.
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
              {/* ORDER TYPE, not session. The buttons are deliberately just "Market" and
                  "Limit": the session toggle immediately above already spends the
                  letters MOO/MOC on the SESSION, and an unpriced order in the opening
                  session is a market-on-OPEN. The type is the thing being chosen here. */}
              <div className="field">
                <span>Order type</span>
                <div className="segmented order-type">
                  <button
                    className={isMarketOrder ? 'on' : ''}
                    onClick={() => setOrderType('Market')}
                    title="Unpriced. Fills at whatever the cross prints, ahead of every limit order."
                  >
                    Market
                  </button>
                  <button
                    className={!isMarketOrder ? 'on' : ''}
                    onClick={() => setOrderType('Limit')}
                    title="Priced. Names the worst price you will accept and walks away from anything worse."
                  >
                    Limit
                  </button>
                </div>
              </div>
              {isMarketOrder ? (
                <p className="hint subtle">
                  <strong>Unpriced.</strong> You are buying the print itself: this order counts at
                  every candidate price, is allocated <strong>ahead of every limit order</strong>,
                  and is never cancelled for being away from the cross. It is what index funds and
                  rebalancers send, and it is most of the real closing volume.
                </p>
              ) : (
                <label className="field small">
                  <span>Limit ({CASH})</span>
                  <input
                    className="mono"
                    type="number"
                    min="0"
                    step="any"
                    placeholder={closePrice != null ? `${fmt2(closePrice)} (anchor)` : 'anchor'}
                    value={limitPrice}
                    onChange={(e) => setLimitPrice(e.target.value)}
                  />
                  <span className="summary-sub">
                    {side === 'Buy' ? 'most you will pay' : 'least you will accept'} — set it away
                    from the anchor to let the book find its own price.
                  </span>
                </label>
              )}
              <div className="summary">
                <span>{sessionLabel(session)} anchor</span>
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
                {/* WHY a market BUY commits more than quantity * anchor. It has no limit
                    of its own, so the venue's price collar is the only bound on what it
                    can be asked to pay — the close is clamped into anchor ± band and
                    cannot print above the top of it. Any unspent cash comes straight
                    back as change at settlement. */}
                {closePrice != null && side === 'Buy' && isMarketOrder && (
                  <span className="summary-sub">
                    unpriced — reserved at the collar ceiling {fmt2(collarHigh(anchor))} {CASH}
                    {' '}(anchor + {fmt2(collarBand(anchor))}), the highest this close can print.
                    Change is returned at settlement.
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

        {/* -------- Liquidity mandate · imbalance panel --------
            Two states, one panel. WITH a live mandate it shows the residual, exactly
            as before. WITHOUT one it shows the venue's posted terms and an Accept
            action — because the seat is contestable, and a party that has not taken
            it is not shut out, it simply has not committed yet. */}
        {showLpPanel && !holdsMandate && openTerms && (
          <section className="card lp-imbalance" aria-label="Liquidity mandate on offer">
            <div className="card-head">
              <h2>Imbalance · LP View</h2>
              <span className="lp-tag">seat open · no mandate</span>
            </div>
            <p className="hint">
              The net imbalance of <strong>{asset}</strong>&rsquo;s sealed book is disclosed only to
              a party that has <strong>signed an obligation</strong> to absorb it. The seat is
              posted, not awarded: any registered participant may take it, several may hold it at
              once, and there is no fee. You commit <em>before</em> you can see — nothing about the
              book is visible from here.
            </p>
            <div className="imbalance-figure flat">
              <span className="imbalance-side">Mandate on offer</span>
              <span className="imbalance-qty mono">
                {fmt(openTerms.commitmentSize)} {asset}
              </span>
              <span className="imbalance-at mono">
                within {openTerms.maxBandBps}bps of {fmt2(openTerms.anchorPrice)} {CASH}
              </span>
            </div>
            <p className="imbalance-note">
              {imbalance?.mandateRequired && imbalance.note ? imbalance.note : openTerms.note}
            </p>
            <button className="primary lp" disabled={busy} onClick={doAcceptMandate}>
              {busy
                ? 'Accepting…'
                : `Accept mandate · absorb up to ${fmt(openTerms.commitmentSize)} ${asset}`}
            </button>
            <button
              className="ghost"
              disabled={busy}
              onClick={() => loadMandate(asset, session, acting)}
            >
              Refresh
            </button>
          </section>
        )}

        {showLpPanel && holdsMandate && (
          <section className="card lp-imbalance" aria-label="Liquidity provider imbalance view">
            <div className="card-head">
              <h2>Imbalance · LP View</h2>
              <span className="lp-tag">mandated LP · {acting}</span>
            </div>
            <p className="hint">
              You hold a live liquidity mandate over <strong>{asset}</strong>: absorb up to{' '}
              <strong>
                {fmt(mandate!.commitmentSize ?? 0)} {asset}
              </strong>{' '}
              within <strong>{mandate!.maxBandBps}bps</strong> of{' '}
              {mandate!.anchorPrice != null ? fmt2(mandate!.anchorPrice) : '—'} {CASH}. That
              obligation — not a field the venue wrote — is what shows you the <strong>net</strong>{' '}
              imbalance below. Individual orders and trader identities stay hidden.
            </p>
            {hasImbalance ? (
              <>
                <div className={`imbalance-figure ${imbalance!.netSide!.toLowerCase()}`}>
                  <span className="imbalance-side">Net {imbalance!.netSide!.toUpperCase()} imbalance</span>
                  <span className="imbalance-qty mono">
                    {fmt(imbalance!.netQuantity!)} {asset}
                  </span>
                  <span className="imbalance-at mono">
                    @ {imbalance!.referencePrice != null ? fmt2(imbalance!.referencePrice) : '—'} {CASH}
                  </span>
                </div>
                <p className="imbalance-note">
                  The book is net {imbalance!.netSide!.toLowerCase()} — offset by{' '}
                  <strong>{imbalance!.netSide === 'Buy' ? 'SELLING' : 'BUYING'}</strong>{' '}
                  {fmt(imbalance!.netQuantity!)} {asset} to clear the cross.
                </p>
                <button className="primary lp" disabled={busy} onClick={doOffset}>
                  {busy
                    ? 'Offsetting…'
                    : `Offset · ${imbalance!.netSide === 'Buy' ? 'Sell' : 'Buy'} ${fmt(
                        imbalance!.netQuantity!,
                      )} ${asset}`}
                </button>
              </>
            ) : isMandatedLp ? (
              <div className="imbalance-figure flat">
                <span className="imbalance-side">Book balanced</span>
                <span className="imbalance-qty mono">Flat</span>
                <span className="imbalance-at">no offsetting liquidity needed</span>
              </div>
            ) : (
              /* Mandate held, but no number came back. Say THAT — never render an
                 absent disclosure as "Flat", which would read as a balanced book and
                 is a different (and false) statement. */
              <div className="imbalance-figure flat">
                <span className="imbalance-side">Residual not available</span>
                <span className="imbalance-qty mono">—</span>
                <span className="imbalance-at">
                  {imbalance?.note ?? 'the venue has not published against your mandate yet'}
                </span>
              </div>
            )}
            <button
              className="ghost"
              disabled={busy}
              onClick={() => {
                void loadImbalance(asset, session, acting);
                void loadMandate(asset, session, acting);
              }}
            >
              Refresh
            </button>
          </section>
        )}

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
                <span
                  className="cross-meta-price"
                  title="The venue's published anchor. The cross prints the volume-maximising price discovered from the sealed book, which may be above or below this."
                >
                  anchor <strong className="mono">{mocState.referencePrice != null ? fmt2(mocState.referencePrice) : '—'}</strong> {CASH}
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
                      <th className="num">Limit</th>
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
                        {/* An unpriced order has NO limit — the ledger sends null, and
                            the honest rendering of that is "MOC", not a blank cell and
                            certainly not 0 or the anchor (both read as a real price the
                            order never named). */}
                        <td className="num mono">
                          {o.limitPrice == null ? (
                            <span className="tag moc" title="Unpriced market-on-close — fills at the print, ahead of every limit order">
                              MOC
                            </span>
                          ) : (
                            fmt2(o.limitPrice)
                          )}
                        </td>
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

        {/* -------- CIP-56 · claim an asset issued by ANOTHER registry --------
            A transfer from a foreign registry arrives as a pending
            TransferInstruction and is not a holding until the receiver accepts it.
            This is where a real third-party asset (BitSafe CBTC) is claimed — the
            direct answer to "your tokens are self-issued stand-ins". */}
        <PendingTransfersPanel
          acting={acting}
          onChanged={() => {
            void loadHoldings(acting);
            void loadReceipts(acting);
          }}
          flash={flash}
        />

        {/* -------- Decentralised operator · committee-attested NAV -------- */}
        <CommitteePanel parties={parties} instruments={instruments} flash={flash} />

        {/* -------- Fund / ETF builder · in-kind create & redeem -------- */}
        <FundPanel
          parties={parties}
          instruments={instruments}
          acting={acting}
          onChanged={() => {
            void loadHoldings(acting);
            void loadReceipts(acting);
          }}
          flash={flash}
        />

        {/* -------- Receipts · PER-PARTY ledger view (need-to-know audit) -------- */}
        <section className="card receipts">
          <div className="card-head">
            <h2>Settlement Receipts</h2>
            <span className="who">{ledgerReceipts.length} visible to {acting}</span>
          </div>
          <p className="hint">
            Need-to-know audit — queried on the ledger <strong>as {acting}</strong>. The ledger itself
            decides who sees each receipt: the parties to a settlement plus the auditor. Switch to{' '}
            <strong>Auditor</strong> to see trades without holdings; switch to <strong>Eve</strong> and
            she sees nothing.
          </p>
          {ledgerReceipts.length === 0 ? (
            <p className="empty">
              {acting.toLowerCase() === 'eve'
                ? 'Eve is party to nothing — she sees zero receipts. That is the privacy model.'
                : `${acting} has no visible receipts yet. Create/redeem a basket, or run a cross.`}
            </p>
          ) : (
            <ul className="receipt-list">
              {ledgerReceipts.map((r) => {
                const cls = r.kind.toLowerCase().replace(/\s+/g, '-');
                return (
                  <li key={r.contractId} className={`receipt ${cls}`}>
                    <div className="receipt-head">
                      <span className={`badge ${cls}`}>{r.kind}</span>
                      <span className="receipt-headline">{r.headline}</span>
                    </div>
                    <div className="receipt-body">
                      <span className="cp">
                        visible to: <strong>{r.visibleTo.join(' · ')}</strong>
                      </span>
                      <code className="cid mono" title={r.contractId}>
                        {shortCid(r.contractId)}
                      </code>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
          <button className="ghost" disabled={busy} onClick={() => loadReceipts(acting)}>
            Refresh
          </button>
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
