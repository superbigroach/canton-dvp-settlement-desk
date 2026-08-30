import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
  type FixingResponse,
  type Session,
} from './api';
import CommitteePanel from './CommitteePanel';
import ContinuousBookPanel from './ContinuousBookPanel';
import FundPanel from './FundPanel';
import PendingTransfersPanel from './PendingTransfersPanel';
import PerpetualPanel from './PerpetualPanel';

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

// The auction prints a PRICE. NAV is a fund concept - a basket's net asset value -
// and calling one market's close a NAV is what invites "why does cETH have a NAV?".
const sessionLabel = (s: Session) =>
  s === 'Open' ? 'Official Open' : 'Official Close';

export default function App() {
  const [parties, setParties] = useState<Party[]>([]);
  const [instruments, setInstruments] = useState<Instrument[]>([]);
  const [acting, setActing] = useState<string>(''); // party label
  const [holdings, setHoldings] = useState<Holding[]>([]);
  // Receipts VISIBLE to the acting party — a real per-party ledger query (need-to-know audit),
  // so switching parties shows exactly what that party is entitled to see (Eve sees nothing).
  const [ledgerReceipts, setLedgerReceipts] = useState<LedgerReceipt[]>([]);

  const [mode] = useState<Mode>('Auction'); // auction-only desk (DvP engine still powers the cross)
  // Same reasoning as the committee: which asset you were looking at is a session
  // fact, and losing it on every reload sent you back to a demo equity mid-demo.
  const ASSET_KEY = 'crossdesk.asset';
  const [asset, setAsset] = useState<string>(() => {
    try {
      return window.localStorage.getItem(ASSET_KEY) ?? '';
    } catch {
      return '';
    }
  });
  useEffect(() => {
    try {
      if (asset) window.localStorage.setItem(ASSET_KEY, asset);
    } catch {
      /* ignore */
    }
  }, [asset]);
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
  // Official fixes on the ledger, so a yielding instrument can show the rate that was
  // actually attested rather than a number typed into this file.
  const [accruingFixes, setAccruingFixes] = useState<FixingResponse[]>([]);

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

  // A money-market instrument is valued by ACCRUAL, not by an auction: it has a NAV
  // and a rate, not a close. Detected from the instrument kind, so a new yielding
  // instrument gets the right treatment without another special case here.
  const selectedIsYielding = useMemo(() => {
    const k = instruments.find((i) => i.id === asset)?.kind ?? '';
    return /money|mmf|fund|treasury/i.test(k);
  }, [instruments, asset]);
  const yielding = selectedIsYielding;
  // The newest ACCRUING fix for it — the rate and day count the committee signed.
  const yieldFix = useMemo(
    () =>
      accruingFixes
        .filter((f) => f.instrumentId === asset && Number(f.ratePerAnnum) !== 0)
        .slice(-1)[0] ?? null,
    [accruingFixes, asset],
  );

  // WHOSE ORDER IS THIS? The ledger answers with its own party id — `alice-crossdesk`
  // on the shared node, or a full `label::1220…` — while the desk holds the display
  // label `Alice`. Comparing those two strings is always false, which is why a trader
  // could see its own resting order and get an em-dash where Withdraw belongs: the
  // ledger was willing, the UI just never asked. Resolve through the roster instead.
  const partyLabel = useCallback(
    (raw: string) => {
      if (!raw) return raw;
      const head = raw.split('::')[0];
      const hit = parties.find(
        (p) => p.party === raw || p.label === raw || p.party.split('::')[0] === head,
      );
      return hit ? hit.label : head;
    },
    [parties],
  );
  const isMine = useCallback(
    (raw: string) => !!acting && partyLabel(raw) === acting,
    [acting, partyLabel],
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
        const [ps, rawIns] = await Promise.all([api.parties(), api.instruments()]);
        setParties(ps);
        // ONE ROW PER SYMBOL. An Instrument is a contract, and republishing a mark by
        // creating a fresh one leaves both on the ledger — so a symbol can legitimately
        // appear more than once in the query. A picker that lists LX1 twice reads as a
        // bug whatever the cause, so the desk keeps the LAST contract for each id: the
        // most recently published mark is the one that should be quoted.
        const byId = new Map<string, (typeof rawIns)[number]>();
        rawIns.forEach((i) => byId.set(i.id, i));
        const ins = [...byId.values()];
        setInstruments(ins);
        // Not fatal if it fails: an empty roster just means no fix has been struck.
        api.fixings().then(setAccruingFixes).catch(() => setAccruingFixes([]));
        const first = ps.find((p) => p.label === 'Alice') ?? ps[0];
        setActing(first?.label ?? '');
        // OPEN ON THE FUND. The desk's whole story is the basket — its NAV, its
        // premium/discount and its hedge — so landing on a demo equity meant every
        // visitor's first act was to change the selector. Fall back to the first
        // tradeable asset only if no fund is listed.
        // Keep what the operator had selected if it is still listed; only fall back
        // to the fund (then any tradeable asset) on a genuinely cold start.
        const remembered = (() => {
          try {
            return window.localStorage.getItem(ASSET_KEY) ?? '';
          } catch {
            return '';
          }
        })();
        const firstAsset =
          ins.find((i) => i.id === remembered) ??
          ins.find((i) => i.kind === 'Fund') ??
          ins.find((i) => i.kind !== 'Cash');
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

  const qtyNum = Number((Number(quantity) || 0).toFixed(10));
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

  // ONE ACTION AT A TIME, ENFORCED SYNCHRONOUSLY.
  // `disabled={busy}` is not a guard: setBusy schedules a re-render, so two clicks
  // landing in the same frame both pass the check and both submit. On "Run the Cross"
  // that means two uncross attempts against one auction — the second either double-
  // settles or dies with a contract-not-found the operator cannot interpret. A ref
  // flips the instant the first click is handled, before React paints anything.
  const inFlight = useRef(false);

  async function runAction<T>(fn: () => Promise<T>): Promise<T | undefined> {
    if (inFlight.current) return undefined;
    inFlight.current = true;
    setBusy(true);
    setError('');
    try {
      return await fn();
    } catch (e) {
      // ONE formatter for every failure path — see errorMessage() in api.ts.
      setError(errorMessage(e));
      return undefined;
    } finally {
      inFlight.current = false;
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

  // THE IMBALANCE IS A VENUE-SIDE VIEW. A trader submits orders and is told nothing
  // about the residual — which is the same posture NYSE takes toward everyone who is
  // not the DMM, and the reason the auction stays sealed to the people in it.
  //
  // The obligated-seat machinery is still on the LEDGER and still enforced there: the
  // ledger discloses an ImbalanceDisclosure only to a party holding a live
  // LiquidityMandate, several may hold one at once, and a provider that misses its
  // commitment is revoked and barred (daml/LiquidityMandate.daml). What changed is
  // only who is INVITED to take a seat from this screen. Onboarding a liquidity
  // provider is a relationship a venue enters deliberately, the way an exchange
  // approves a member firm — not a button a trader finds mid-session. Narrowing the
  // eligible roster on the posted terms is how that is expressed on the ledger.
  const showLpPanel = false;   // retired from the desk screen - see the note above
  const isMandatedLp = holdsMandate && !!imbalance?.disclosed;

  // WHAT THIS DESK SELLS, AND WHAT IT MERELY CONTAINS.
  //
  // Three jobs are the product: discover a price (the sealed auction), attest it
  // (the K-of-N committee), settle against it (in-kind create/redeem). The
  // continuous book and the cash-settled perpetuals are BUILT, TESTED and STAYING
  // — they are simply not what is sold, and a screen that offers them says
  // otherwise to every customer who opens it.
  //
  // The reason is regulatory and it is the strategy, not a limitation. Operating a
  // continuous market and running leverage are licensed activities in every
  // jurisdiction that matters. Publishing a number, and a committee signing a
  // valuation, is the part that is not. Cutting these two from the OFFERING is
  // what keeps the company on the unregulated side of that line.
  //
  // They are gated here rather than deleted, in both directions deliberately:
  //   - the Daml templates cannot be withdrawn from the package at all. `crossdesk`
  //     is package-NAME scoped (#crossdesk), so removing a template fails the
  //     upgrade check against 2.0.0 with NOT_VALID_UPGRADE_PACKAGE and orphans
  //     every contract already on a participant;
  //   - and the panels stay compiled and importable so the demo path still exists
  //     for anyone who asks to see them, which is a stronger answer than a gap.
  // Flip either flag to show that panel again. See
  // PITCH-MEETINGS-CLIENTS/04_WHAT_WE_DO_NOT_SELL.md.
  const showContinuousBook = false;  // sold by Temple Trading, a funded institutional CLOB
  const showPerpetuals = false;      // leverage is a licensed activity; not the product

  // THE VENUE'S OWN IMBALANCE — arithmetic, not a disclosure.
  //
  // The venue signs every sealed order, so it already holds the whole book; summing it
  // reveals nothing the operator was not already entitled to. That is why this needs no
  // mandate and no ledger change: an ImbalanceDisclosure exists to tell a party
  // something it CANNOT see, and the venue is not such a party.
  const venueImbalance = (() => {
    if (!actingIsVenue || !mocState?.orders?.length) return null;
    const buy = mocState.orders
      .filter((o) => o.side === 'Buy')
      .reduce((t, o) => t + o.quantity, 0);
    const sell = mocState.orders
      .filter((o) => o.side === 'Sell')
      .reduce((t, o) => t + o.quantity, 0);
    const net = buy - sell;
    return {
      buy,
      sell,
      net: Math.abs(net),
      side: net > 0 ? 'Buy' : net < 0 ? 'Sell' : 'Flat',
    };
  })();
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
        {/* The desk lives at /desk/; the marketing site is at /. Without this the only
            way back is editing the URL bar, which is how a visitor ends up stuck on an
            app with no ledger behind it and no idea what the product is. */}
        <a className="brand" href="/" title="Back to crossdesk.app" style={{ textDecoration: 'none', color: 'inherit' }}>
          <span className="logo" aria-hidden>◈</span>
          <div className="brand-text">
            <span className="brand-name">CANTON DvP DESK</span>
            <span className="brand-sub">Delivery-versus-Payment · Sealed Opening &amp; Closing Cross</span>
          </div>
        </a>
        <div className="topbar-right">
          {/* THE HEADER MUST NOT ASSERT A LEDGER IT CANNOT REACH.
              This used to read "live · Canton devnet · hackcanton-01" unconditionally,
              which stopped being true when the shared participant went offline at the
              end of Season 2 — the desk then rendered a confident "live" badge above
              panels that had nothing in them, which reads as a broken product rather
              than an absent node. `parties` is the cheapest honest signal available:
              it is the first thing loaded, and it comes back empty exactly when the
              ledger is unreachable. */}
          <span
            className="live"
            title={parties.length
              ? 'Connected to the Canton Ledger API'
              : 'No ledger reachable — see GET /api/diag'}
            style={parties.length ? undefined : { opacity: 0.55 }}
          >
            <span className="dot" style={parties.length ? undefined : { background: '#8A909C' }} />
            {parties.length
              ? `live · ${window.location.hostname === 'localhost' ? 'local sandbox ledger' : 'Canton ledger'}`
              : 'no ledger connected'}
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

      {/* -------- Official Open / Close price quote card -------- */}
      <section className="quote" aria-label="Official price quote">
        <div className="quote-left">
          <div className="quote-instrument">
            <span className={`pill ${asset === CASH ? 'cash' : 'asset'}`}>{asset || '—'}</span>
            {selectedInstrument && <span className="quote-kind">{selectedInstrument.kind}</span>}
          </div>
          <p className="quote-desc">{selectedInstrument?.description ?? 'Select an instrument'}</p>
        </div>
        <div className="quote-right">
          {/* WHAT THIS NUMBER IS DEPENDS ON THE INSTRUMENT.
              A traded asset has an official CLOSE, discovered by the auction from the
              sealed book. A money-market instrument has no auction and no close - it
              does not trade its way to a value, it EARNS one - so quoting "Official
              Close 1.00" against it is meaningless. It has a NAV, and the number a
              reader actually needs is the rate it accrues at and the convention that
              rate is quoted on. */}
          <span className="quote-label">
            {yielding ? 'NAV · accruing' : sessionLabel(session)}
          </span>
          <span className="quote-price">
            {closePrice != null ? (
              <>
                <span className="mono nav">
                  {yielding ? closePrice.toFixed(6) : fmt2(closePrice)}
                </span>
                <span className="quote-ccy">{CASH}</span>
              </>
            ) : (
              <span className="mono nav muted">—</span>
            )}
          </span>
          <span className="quote-note">
            {yielding ? (
              yieldFix ? (
                <>
                  {(Number(yieldFix.ratePerAnnum) * 100).toFixed(2)}% / yr ·{' '}
                  {yieldFix.dayCount} · attested by {yieldFix.attestors.length} of the
                  committee — the ledger accrues it continuously
                </>
              ) : (
                <>no accruing fix struck yet — the committee attests base, rate and day count</>
              )
            ) : (
              <>Published anchor · the cross is discovered from the book</>
            )}
          </span>
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
                  // VALUE COMES FROM THE SERVER NOW, because an accruing instrument is
                  // not worth its struck base. A money-market share is bought at 1.00
                  // and is worth more by lunchtime; valuing it at the base would show a
                  // static number on the one screen a holder actually looks at.
                  const rp = refPriceOf(h.instrumentId);
                  const val =
                    h.instrumentId === CASH
                      ? h.amount
                      : h.value != null
                        ? h.value
                        : rp != null
                          ? h.amount * rp
                          : null;
                  return (
                    <tr key={h.instrumentId}>
                      <td>
                        <span className={`pill ${h.instrumentId === CASH ? 'cash' : 'asset'}`}>
                          {h.instrumentId}
                        </span>
                        {h.accruing && (
                          <span className="accruing-tag" title="Earns through a rising price, not a rising balance — the units never change">
                            accruing
                          </span>
                        )}
                      </td>
                      <td className="num mono strong">{fmt(h.amount)}</td>
                      <td className="num mono muted">
                        {val != null ? fmt2(val) : '—'}
                        {h.accruing && h.mark != null && (
                          <div className="mark-now">@ {h.mark.toFixed(8)}</div>
                        )}
                      </td>
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
            {/* "Trade" alone read like a generic ticket. What this card actually lodges
                is a SEALED order into the open or closing cross — never a live order
                against a visible book — and saying so removes the question. */}
            <h2>Trade — Market-on-Close Orders</h2>
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
        {/* -------- Imbalance · the VENUE's view --------
            Unmatched interest is what forces the print to move, so the operator needs
            it to run an orderly close. A trader is told nothing about it: the book is
            sealed to the people in it, which is the same posture NYSE takes toward
            everyone who is not the DMM. */}
        {actingIsVenue && venueImbalance && (
          <section className="card lp-imbalance" aria-label="Venue imbalance view">
            <div className="card-head">
              <h2>Imbalance</h2>
              <span className="lp-tag">venue only</span>
            </div>
            <p className="hint">
              The unmatched side of <strong>{asset}</strong>&rsquo;s sealed book. Only the venue
              sees this — it signs every order, so this is arithmetic on what it already holds,
              not a disclosure. Traders see nothing about the residual.
            </p>
            <div className="cross-meta">
              <span className={`pill ${venueImbalance.side === 'Flat' ? 'cash' : 'asset'}`}>
                {venueImbalance.side === 'Flat'
                  ? 'balanced'
                  : `${fmt(venueImbalance.net)} ${asset} to ${venueImbalance.side.toLowerCase()}`}
              </span>
              <span className="cross-meta-price">
                {fmt(venueImbalance.buy)} buy · {fmt(venueImbalance.sell)} sell
              </span>
            </div>
            {venueImbalance.side !== 'Flat' && (
              <p className="hint subtle">
                Unmatched interest moves the print. A venue that wants a tighter close
                onboards a liquidity provider to absorb it — an obligation signed in
                advance, which the ledger models as a mandate.
              </p>
            )}
          </section>
        )}

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
            <div className="head-controls">
              {/* THE MARKET THIS CROSS IS FOR. It used to be driven only by the Trade
                  card's picker further up the page, which made the auction look like it
                  was hardwired to whichever instrument happened to load first. It runs
                  for ANY marked instrument — so the control belongs here, next to the
                  book it changes. Shared state with Trade, so the two stay in step. */}
              <select
                className="cross-instrument"
                value={asset}
                disabled={busy}
                aria-label="Market to cross"
                onChange={(e) => setAsset(e.target.value)}
              >
                {instruments
                  .filter((i) => i.kind !== 'Cash')
                  .map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.id}
                    </option>
                  ))}
              </select>
              <span className={`session-tag ${session.toLowerCase()}`}>{session}</span>
            </div>
          </div>
          <p className="hint">
            The venue&rsquo;s sealed call auction for <strong>{asset}</strong> — and it runs for any
            market on the desk, not just this one. Orders rest privately —
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
                        <td title={o.trader}>{partyLabel(o.trader)}</td>
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
                          {isMine(o.trader) ? (
                            <button
                              className="ghost small"
                              disabled={busy}
                              onClick={() => doWithdraw(o.contractId)}
                            >
                              Withdraw
                            </button>
                          ) : (
                            <span className="faint">—</span>
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

        {/* -------- The CONTINUOUS session · the ladder the close inherits --------
            A closing auction does not manufacture a price out of nothing; it
            inherits one from resting limit interest. This is where that interest
            rests, matched by price then time, settling at the MAKER's price. It is
            also the clearest demonstration of the privacy property: the book is
            dark pre-trade (even to the auditor), the tape is public post-trade.

            PARKED — see showContinuousBook above. Built and tested; not sold. */}
        {showContinuousBook && <ContinuousBookPanel
          parties={parties}
          instruments={instruments}
          acting={acting}
          asset={asset}
          onAsset={setAsset}
          onChanged={() => {
            void loadHoldings(acting);
            void loadReceipts(acting);
          }}
          flash={flash}
        />}

        {/* -------- Leverage · cash-settled perpetuals --------
            The one place exposure is taken WITHOUT holding the asset: post cash,
            get a view, settle in cash. It completes the arbitrage loop the fund
            layer starts — a share that drifts from NAV is only corrected if
            somebody can cheaply take the other side, and synthetic is cheaper
            than funding the whole basket.

            PARKED — see showPerpetuals above. Built and tested; not sold. */}
        {showPerpetuals && <PerpetualPanel
          parties={parties}
          instruments={instruments}
          acting={acting}
          asset={asset}
          onAsset={setAsset}
          onChanged={() => {
            void loadHoldings(acting);
            void loadReceipts(acting);
          }}
          flash={flash}
        />}

        {/* -------- Decentralised operator · committee-attested NAV -------- */}
        <CommitteePanel parties={parties} instruments={instruments} asset={asset} flash={flash} />

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
                      {/* WHEN IT SETTLED. `settledAt` is on the SettlementReceipt template
                          itself — signed as part of the record, not stamped by this browser
                          — so it is the ledger's own answer to "when". A receipt without a
                          time is not an audit trail; it is an assertion. */}
                      {r.settledAt && (
                        <time className="receipt-time mono" dateTime={r.settledAt} title={r.settledAt}>
                          {new Date(r.settledAt).toLocaleString(undefined, {
                            year: 'numeric', month: 'short', day: '2-digit',
                            hour: '2-digit', minute: '2-digit', second: '2-digit',
                          })}
                        </time>
                      )}
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
/**
 * Sum a party's holdings per instrument.
 *
 * A position is spread across many Holding contracts — reservations split them — so a
 * raw list shows one asset five times. Amounts add; the MARK does not, because every
 * slice of the same instrument carries the same one. Value is re-derived from the
 * summed amount rather than added up, so a null mark on one slice cannot silently
 * under-report the total.
 */
function aggregate(holdings: Holding[]): {
  instrumentId: string;
  amount: number;
  mark: number | null;
  value: number | null;
  accruing: boolean;
}[] {
  const m = new Map<string, { amount: number; mark: number | null; accruing: boolean }>();
  for (const h of holdings) {
    const prev = m.get(h.instrumentId);
    m.set(h.instrumentId, {
      amount: (prev?.amount ?? 0) + h.amount,
      mark: h.mark ?? prev?.mark ?? null,
      accruing: h.accruing || (prev?.accruing ?? false),
    });
  }
  return [...m.entries()]
    .map(([instrumentId, v]) => ({
      instrumentId,
      amount: v.amount,
      mark: v.mark,
      value: v.mark == null ? null : v.amount * v.mark,
      accruing: v.accruing,
    }))
    .sort((a, b) => a.instrumentId.localeCompare(b.instrumentId));
}

function shortCid(cid: string): string {
  return cid.length > 18 ? `${cid.slice(0, 10)}…${cid.slice(-6)}` : cid;
}
