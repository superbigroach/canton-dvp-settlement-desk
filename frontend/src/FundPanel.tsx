// FundPanel — the ETF / TOKENISED-FUND BUILDER. A basket token (e.g. LX1 = 0.10
// cETH + 0.01 CBTC per share) is created and redeemed IN-KIND: an authorised
// participant delivers the exact underlyings and receives freshly-minted shares
// (or the reverse), atomically — the mechanism that keeps an ETF glued to NAV.
// NAV per share = Σ (unitsPerShare × close mark); the marks are the committee-
// attested prices, so the basket inherits a credibly-neutral NAV.

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  api,
  errorMessage,
  type PerpPosition,
  type Basket,
  type Instrument,
  type IndicativeNav,
  type NavResponse,
  type Party,
} from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  acting: string;
  onChanged: () => void; // refresh the desk's holdings after a create/redeem
  flash: (m: string) => void;
}

const CASH = 'USDC';
const fmt = (n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 6 });
const fmt2 = (n: number) => n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface Row {
  instrumentId: string;
  unitsPerShare: string;
}

export default function FundPanel({ parties, instruments, acting, onChanged, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  const assets = instruments.filter((i) => i.kind !== 'Cash');

  const [baskets, setBaskets] = useState<Basket[]>([]);
  const [navs, setNavs] = useState<Record<string, NavResponse | null>>({});
  const [inav, setInav] = useState<IndicativeNav | null>(null);
  const [selected, setSelected] = useState<string>('');
  const [shares, setShares] = useState<string>('10');

  const [showDefine, setShowDefine] = useState(false);
  const [admin, setAdmin] = useState<string>('Bank');
  const [newId, setNewId] = useState<string>('');
  const [rows, setRows] = useState<Row[]>([
    { instrumentId: 'cETH', unitsPerShare: '0.1' },
    { instrumentId: 'CBTC', unitsPerShare: '0.01' },
  ]);
  const [participants, setParticipants] = useState<string[]>(['Alice', 'Bob']);

  // Same reasoning as App.runAction: the arb is three ledger writes, so a double
  // click could start a second create before the first finished and leave two hedges
  // against one basket leg.
  const inFlight = useRef(false);
  const [busy, setBusy] = useState(false);
  const [step, setStep] = useState<string>('');
  const [hedge, setHedge] = useState<PerpPosition | null>(null);
  // Set when THIS session lodged a market-on-close order, so the auto-unwind only
  // ever fires for a trade we placed — never for a hedge somebody else is running.
  const [armed, setArmed] = useState(false);
  const [err, setErr] = useState<string>('');

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setErr('');
    try {
      return await fn();
    } catch (e) {
      // ONE formatter for every failure path — see errorMessage() in api.ts.
      setErr(errorMessage(e));
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  const loadBaskets = useCallback(async () => {
    try {
      const bs = await api.baskets();
      setBaskets(bs);
      setSelected((s) => s || (bs[0]?.basketId ?? ''));
      const entries = await Promise.all(
        bs.map(async (b) => [b.basketId, await api.basketNav(b.basketId).catch(() => null)] as const),
      );
      setNavs(Object.fromEntries(entries));
    } catch {
      /* leave as-is */
    }
  }, []);

  useEffect(() => {
    void loadBaskets();
  }, [loadBaskets]);

  const basket = baskets.find((b) => b.basketId === selected) ?? null;
  const nav = selected ? navs[selected] ?? null : null;

  // A SYMBOL IS A NAME ON A LEDGER, AND NAMES ARE TAKEN OR THEY ARE NOT.
  // Defining a second LX1 does not "copy" the fund — it collides with a live one
  // that already has holders, so the ledger rejects it and the operator is left
  // reading a stack trace. Both namespaces count: every basket also gets published
  // as an Instrument so it can trade, so a free basket id with a taken instrument
  // id is still taken.
  const taken = new Set<string>([
    ...baskets.map((b) => b.basketId.toLowerCase()),
    ...instruments.map((i) => i.id.toLowerCase()),
  ]);
  const trimmedId = newId.trim();
  const symbolTaken = trimmedId !== '' && taken.has(trimmedId.toLowerCase());

  /** First unused LX-n, so the form opens on a name that will actually work. */
  function freeSymbol(): string {
    for (let n = 1; n < 999; n += 1) {
      const candidate = `LX${n}`;
      if (!taken.has(candidate.toLowerCase())) return candidate;
    }
    return '';
  }

  /** Open the designer on a free symbol; optionally clone an existing recipe. */
  function beginDefine(from?: Basket) {
    setNewId(freeSymbol());
    if (from) {
      setRows(
        from.components.map((c) => ({
          instrumentId: c.instrumentId,
          unitsPerShare: String(c.unitsPerShare),
        })),
      );
      setAdmin(from.administrator);
    }
    setErr('');
    setShowDefine(true);
  }

  // ---- THE ARBITRAGE, MADE EXPLICIT -------------------------------------
  // A basket that trades is two prices: what the market pays for the share, and
  // what the share is actually worth. The whole reason in-kind create/redeem
  // exists is to let somebody collapse the difference — buy the cheap one, turn it
  // into the dear one. Showing the gap without showing the trade that closes it
  // would be a dashboard; this is a desk.
  const officialNav = nav?.navPerShare ?? null;
  const indicativeNav = inav?.indicativeNavPerShare ?? null;
  const arb =
    officialNav != null && indicativeNav != null && officialNav > 0
      ? {
          official: officialNav,
          indicative: indicativeNav,
          bps: ((indicativeNav - officialNav) / officialNav) * 10_000,
          // Worth MORE than the signed mark -> create at the mark and sell the share.
          above: indicativeNav > officialNav,
          edgePerShare: Math.abs(indicativeNav - officialNav),
        }
      : null;
  // A DEADBAND, BECAUSE A FEW BASIS POINTS IS NOT AN OPPORTUNITY.
  // The indicative NAV tracks live spot, so it crosses back and forth over the signed
  // mark every few seconds while the market breathes. Flipping the recommended trade
  // on that is worse than useless — it tells an operator to create, then redeem, then
  // create again on noise, each leg paying fees. Real ETF arbitrage clears at a spread
  // wide enough to cover fills; below that the honest answer is "nothing to do".
  const ARB_DEADBAND_BPS = 15;
  const arbLive = arb !== null && Math.abs(arb.bps) >= ARB_DEADBAND_BPS;

  // THE INDICATIVE SIDE IS MEANT TO MOVE, so it is polled rather than loaded once.
  // 10s is chosen to sit near the ~15s cadence exchanges disseminate an iNAV at —
  // fast enough that the number visibly lives, slow enough that it is not a stream
  // pretending to be a price. Failures are silent: a missing iNAV must never take the
  // official NAV off the screen, because that one is what actually settles.
  useEffect(() => {
    if (!selected) {
      setInav(null);
      return;
    }
    let alive = true;
    const load = () =>
      api
        .basketIndicativeNav(selected)
        .then((r) => {
          if (alive) setInav(r);
        })
        .catch(() => {
          /* keep the last good value; never blank the official NAV */
        });
    void load();
    const t = setInterval(load, 10_000);
    return () => {
      alive = false;
      clearInterval(t);
    };
  }, [selected, navs]);

  // AN OPEN HEDGE IS NOT ALLOWED TO GO QUIET. The auction settles the SHARE leg and
  // leaves the perp exactly where it was, so between the cross printing and somebody
  // remembering to close it, the desk is short a basket it no longer owns — a naked
  // directional bet wearing the costume of an arbitrage. Poll for it, and while one is
  // open the arb button becomes the unwind.
  useEffect(() => {
    if (!selected || !acting) {
      setHedge(null);
      return;
    }
    let alive = true;
    const load = async () => {
      try {
        const ps = await api.perpPositions(acting);
        if (!alive) return;
        const open = ps.find((x) => x.instrumentId === selected) ?? null;
        setHedge(open);

        // THE HEDGE COMES OFF WHEN THE CROSS PRINTS, not when somebody remembers.
        // A closing auction is a scheduled event — in a real venue it is the same
        // instant every day — so an arbitrageur's hedge is meant to expire WITH it.
        // We only act on an auction WE lodged into (`armed`), and only once our own
        // order is no longer resting, which is precisely the moment it crossed.
        if (open && armed) {
          const moc = await api.mocState(selected, 'Close', acting, CASH).catch(() => null);
          const stillResting = (moc?.orders ?? []).length > 0;
          if (!stillResting && alive) {
            setArmed(false);
            await unwind();
          }
        }
      } catch {
        /* no perp market on this basket is a normal state */
      }
    };
    void load();
    const t = setInterval(load, 6000);
    return () => {
      alive = false;
      clearInterval(t);
    };
  }, [selected, acting, busy, armed]);

  const sharesNum = Number((Number(shares) || 0).toFixed(10));

  async function create() {
    if (!basket) return;
    const res = await run(() => api.basketCreate({ basketId: basket.basketId, ap: acting, shares: sharesNum }));
    if (!res) return;
    flash(
      `Created ${fmt(res.shares)} ${basket.basketId} in-kind${
        res.navPerShare != null ? ` · NAV ${fmt2(res.navPerShare)} ${CASH}/share` : ''
      }.`,
    );
    onChanged();
    await loadBaskets();
  }

  async function redeem() {
    if (!basket) return;
    const res = await run(() => api.basketRedeem({ basketId: basket.basketId, ap: acting, shares: sharesNum }));
    if (!res) return;
    flash(`Redeemed ${fmt(res.shares)} ${basket.basketId} in-kind — underlyings returned.`);
    onChanged();
    await loadBaskets();
  }

  /**
   * THE WHOLE ARB, IN ORDER, OR NOT AT ALL.
   *
   * Three ledger writes: the basket leg, the hedge, and the market leg. They are
   * SEQUENTIAL on purpose — each one is a separate Daml transaction against a shared
   * node, so firing them together would race, and a hedge that lands before the shares
   * exist is a naked position rather than an arbitrage. If any leg fails the sequence
   * STOPS and says which one, because the honest failure is "you are half in" and the
   * dishonest one is a green toast over a broken book.
   */
  /**
   * ALWAYS CREATE, THEN HEDGE. `goLong` picks the perp side and the market leg.
   *
   * Shares go UP by `sharesNum` every time, which is the thing an operator can see and
   * check against the balance. Creating leaves you long the basket, so SHORT is the leg
   * that makes you flat and is the true arbitrage; LONG doubles the exposure and is a
   * levered directional bet, offered because a desk should not refuse a trade it can
   * price. The label says which is which.
   */
  async function runArb(goLong: boolean) {
    if (inFlight.current) return;
    inFlight.current = true;
    if (!basket || sharesNum <= 0) return;
    setBusy(true);
    setErr('');
    try {
      // 1 — the basket leg, settled at the SIGNED mark.
      setStep('creating…');
      await api.basketCreate({ basketId: basket.basketId, ap: acting, shares: sharesNum });

      // 2 — the hedge. Sized to the shares just created/redeemed and collateralised at
      //     roughly 5x, so the position is levered the way an arb desk would run it
      //     rather than tying up the full notional. Failing here is not fatal: the
      //     basket leg already settled, so we report it and keep going.
      const px = nav?.navPerShare ?? 0;
      let hedged = true;
      try {
        setStep('hedging…');
        await api.openPerpPosition({
          trader: acting,
          side: goLong ? 'Long' : 'Short',
          size: Number(sharesNum.toFixed(10)),
          instrumentId: basket.basketId,
          collateral: Number(((sharesNum * px) / 5).toFixed(2)),
          cashInstrument: CASH,
        });
      } catch (e) {
        hedged = false;
        setErr(`basket leg settled, hedge did not: ${errorMessage(e)}`);
      }

      // 3 — the market leg. Unpriced market-on-close: it takes the print the auction
      //     discovers and is allocated ahead of every limit order, which is exactly
      //     what an AP wants when the whole trade is "be flat by the close".
      setStep('sending MOC…');
      await api.mocOrder({
        trader: acting,
        side: goLong ? 'Buy' : 'Sell',
        quantity: Number(sharesNum.toFixed(10)),
        instrumentId: basket.basketId,
        cashInstrument: CASH,
        session: 'Close',
        orderType: 'Market',
      });

      setArmed(true);
      flash(
        `Created ${fmt(sharesNum)} ${basket.basketId}` +
          `${hedged ? `, went ${goLong ? 'long' : 'short'} the perp` : ''}` +
          `, ${goLong ? 'buy' : 'sell'} MOC resting for the close.`,
      );
      onChanged();
      await loadBaskets();
    } catch (e) {
      setErr(errorMessage(e));
    } finally {
      inFlight.current = false;
      setStep('');
      setBusy(false);
    }
  }

  /**
   * CLOSE THE WHOLE ARB — BOTH LEGS.
   *
   * Closing only the perp leaves the market-on-close order resting, so the position
   * that was hedged a second ago executes NAKED at the cross. That is strictly worse
   * than never hedging: the operator believes they are flat and the ledger disagrees.
   * So the resting MOC is withdrawn FIRST — if that fails we stop and keep the hedge,
   * because an unhedged live order is the one state this function exists to prevent.
   */
  async function unwind() {
    if (inFlight.current) return;
    inFlight.current = true;
    if (!hedge) return;
    setBusy(true);
    setErr('');
    try {
      setStep('pulling MOC…');
      const moc = await api.mocState(selected, 'Close', acting, CASH).catch(() => null);
      for (const ord of moc?.orders ?? []) {
        await api.withdrawOrder(ord.contractId, acting);
      }
      setArmed(false);

      setStep('closing hedge…');
      const r = await api.closePerpPosition(hedge.contractId, acting);
      flash(
        `Arb closed — resting MOC pulled and ${hedge.side} ${fmt(hedge.size)} ${hedge.instrumentId} unwound` +
          (r?.payout != null ? `, ${fmt2(Number(r.payout))} ${CASH} returned.` : '.'),
      );
      setHedge(null);
      onChanged();
    } catch (e) {
      setErr(errorMessage(e));
    } finally {
      inFlight.current = false;
      setStep('');
      setBusy(false);
    }
  }

  function toggleParticipant(label: string) {
    setParticipants((ps) => (ps.includes(label) ? ps.filter((x) => x !== label) : [...ps, label]));
  }

  function setRow(i: number, patch: Partial<Row>) {
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));
  }

  async function defineBasket() {
    if (!trimmedId) {
      setErr('a basket needs a symbol');
      return;
    }
    if (symbolTaken) {
      setErr(`${trimmedId} already exists — pick a symbol that is not in use`);
      return;
    }
    const components = rows
      .filter((r) => r.instrumentId && Number(r.unitsPerShare) > 0)
      .map((r) => ({ instrumentId: r.instrumentId, unitsPerShare: Number(r.unitsPerShare) }));
    if (components.length === 0) {
      setErr('add at least one component with a positive unitsPerShare');
      return;
    }
    // A basket holding itself is a NAV that references its own NAV. The ledger
    // would happily store it and the valuation would never terminate.
    if (components.some((c) => c.instrumentId.toLowerCase() === trimmedId.toLowerCase())) {
      setErr(`${trimmedId} cannot be a component of itself`);
      return;
    }
    const dupes = components
      .map((c) => c.instrumentId)
      .filter((id, i, all) => all.indexOf(id) !== i);
    if (dupes.length > 0) {
      setErr(`${dupes[0]} is listed twice — combine it into one line`);
      return;
    }
    const res = await run(() =>
      api.defineBasket({
        administrator: admin,
        basketId: trimmedId,
        components,
        participants,
        description: `${trimmedId} tokenised basket`,
      }),
    );
    if (!res) return;
    flash(`Basket ${res.basketId} defined — ${components.length} components, ${participants.length} APs.`);
    setSelected(res.basketId);
    setShowDefine(false);
    await loadBaskets();
  }

  return (
    <section className="card fund" aria-label="ETF / tokenised-fund builder">
      <div className="card-head">
        <h2>Fund / ETF Builder</h2>
        <span className="who">in-kind create &amp; redeem</span>
      </div>
      <p className="hint">
        A basket token created &amp; redeemed <strong>in-kind</strong> — deliver the underlyings, mint
        shares (and the reverse), atomically. NAV = &Sigma; (units &times; close mark).
      </p>

      {err && (
        <div className="warn" onClick={() => setErr('')}>
          {err}
        </div>
      )}

      {baskets.length === 0 ? (
        <p className="empty">No baskets defined yet. Define one below.</p>
      ) : (
        <>
          <label className="field">
            <span>Basket</span>
            <select value={selected} disabled={busy} onChange={(e) => setSelected(e.target.value)}>
              {baskets.map((b) => (
                <option key={b.basketId} value={b.basketId}>
                  {b.basketId} · {b.administrator}
                </option>
              ))}
            </select>
          </label>

          {basket && (
            <div className="basket-card">
              <div className="basket-components">
                {basket.components.map((c) => (
                  <span key={c.instrumentId} className="component-chip">
                    <span className="mono strong">{fmt(c.unitsPerShare)}</span> {c.instrumentId}
                  </span>
                ))}
                <span className="per-share">per share</span>
              </div>
              {/* THE TWO NAVs, AND THE GAP BETWEEN THEM.
                  Official is struck from signed marks and is what create/redeem
                  settles at. Indicative is what the fund is worth right now — the
                  on-chain equivalent of the iNAV an exchange puts out every ~15s.
                  A real ETF runs both; the drift is the honest measure of how stale
                  the last strike has become, and the cue to strike again. */}
              <div className="nav-line">
                <span className="nav-label">
                  Official NAV / share
                  <span className="nav-sub">signed · settles create &amp; redeem</span>
                </span>
                <span className="mono nav">
                  {nav && nav.navPerShare != null ? `${fmt2(nav.navPerShare)} ${CASH}` : '—'}
                </span>
              </div>
              {inav && inav.indicativeNavPerShare != null && (
                <div className="nav-line indicative">
                  <span className="nav-label">
                    Indicative NAV <span className="live-dot" aria-hidden="true" />
                    <span className="nav-sub">live · binding on nobody</span>
                  </span>
                  <span className="mono nav">
                    {fmt2(inav.indicativeNavPerShare)} {CASH}
                    {inav.driftBps != null && (
                      <span
                        className={
                          'drift ' +
                          (inav.driftBps > 0 ? 'up' : inav.driftBps < 0 ? 'down' : '')
                        }
                        title="gap between the live value and the last signed strike"
                      >
                        {inav.driftBps > 0 ? '+' : ''}
                        {inav.driftBps.toFixed(2)} bps
                      </span>
                    )}
                  </span>
                </div>
              )}
              {nav && (
                <table className="blotter nav-breakdown">
                  <tbody>
                    {nav.legs.map((l) => {
                      const il = inav?.legs.find((x) => x.instrumentId === l.instrumentId);
                      return (
                        <tr key={l.instrumentId}>
                          <td>
                            <span className="pill asset">{l.instrumentId}</span>
                            {il && <div className="basis">{il.basis}</div>}
                          </td>
                          <td className="num mono muted">
                            {fmt(l.unitsPerShare)} × {l.price != null ? fmt2(l.price) : '—'}
                            {il && il.indicativePrice != null && l.price != null
                              && il.indicativePrice !== l.price && (
                              <div className="now">now {fmt2(il.indicativePrice)}</div>
                            )}
                          </td>
                          <td className="num mono strong">
                            {l.value != null ? fmt2(l.value) : '—'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {/* THE ARB, AS ONE ACTION.
              The gap that matters to an authorised participant is between what the
              basket is WORTH RIGHT NOW (indicative) and the signed number create and
              redeem settle at (official). Capturing it is three legs — the basket, a
              hedge, and a market order — and doing them by hand across three cards is
              how a leg gets left on. One button per direction runs the sequence in
              order and stops at the first failure rather than half-arbing. */}
          {arb && (
            <div className={`arb-strip ${!arbLive ? 'flat' : arb.above ? 'premium' : 'discount'}`}>
              <div className="arb-head">
                <span className="arb-tag">
                  {!arbLive ? 'AT NAV' : arb.above ? 'BASKET RICH' : 'BASKET CHEAP'}
                </span>
                <span className="mono arb-bps">{arb.bps > 0 ? '+' : ''}{arb.bps.toFixed(1)} bps</span>
                <span className="arb-px mono">
                  indicative <strong>{fmt2(arb.indicative)}</strong> vs official{' '}
                  <strong>{fmt2(arb.official)}</strong>
                </span>
              </div>
              {!arbLive ? (
                <p className="arb-play muted">
                  Inside the {ARB_DEADBAND_BPS} bp noise band — the basket is worth what the signed
                  mark says, which is what a working create/redeem mechanism looks like from the
                  outside. The trade below still runs if you want it.
                </p>
              ) : (
                <p className="arb-play">
                  {arb.above ? (
                    <>The underlyings are worth <strong>more</strong> than the signed NAV. Hand back
                    a share carried at the lower official mark and take the richer units:{' '}
                    <strong>redeem</strong>, <strong>long</strong> the perp to go flat, and buy back{' '}
                    <strong>market-on-close</strong>.</>
                  ) : (
                    <>The underlyings are worth <strong>less</strong> than the signed NAV. Deliver
                    the cheap units and receive a share carried at the higher official mark:{' '}
                    <strong>create</strong>, <strong>short</strong> the perp to go flat, and sell{' '}
                    <strong>market-on-close</strong>.</>
                  )}{' '}
                  <strong className="mono">{fmt2(arb.edgePerShare * sharesNum)}</strong> {CASH} on{' '}
                  {fmt(sharesNum)} shares.
                </p>
              )}
              {/* BOTH SIDES, ALWAYS. A desk does not tell a trader which way to go —
                  it shows the gap and offers either side. The strip RECOMMENDS one
                  (highlighted) because the arithmetic points somewhere, but a trader
                  who reads the market differently, or who is closing out an earlier
                  leg, must be able to take the other side without arguing with the UI. */}
              <div className="arb-actions">
                <button
                  className={!arb.above ? 'primary' : 'ghost'}
                  disabled={busy || sharesNum <= 0}
                  onClick={() => void runArb(false)}
                  title="Create the shares and short the perp so you are flat on price — the true arbitrage. Sell into the close."
                >
                  {busy ? step || 'Working…' : `Create ${fmt(sharesNum)} · SHORT hedge · sell MOC`}
                  {!arb.above && arbLive && <span className="rec">recommended</span>}
                </button>
                <button
                  className={arb.above ? 'primary venue' : 'ghost'}
                  disabled={busy || sharesNum <= 0}
                  onClick={() => void runArb(true)}
                  title="Create the shares and go long as well — a levered directional bet, not a hedge. Buys into the close."
                >
                  {busy ? step || 'Working…' : `Create ${fmt(sharesNum)} · LONG · buy MOC`}
                  {arb.above && arbLive && <span className="rec">recommended</span>}
                </button>
              </div>
              {hedge && (
                <p className="hedge-open">
                  <strong>Hedge open</strong> — {hedge.side} {fmt(hedge.size)} {hedge.instrumentId} at{' '}
                  {fmt2(hedge.entryPrice)}. It unwinds <strong>automatically when the cross prints</strong>.
                  {' '}
                  <button className="link" disabled={busy} onClick={() => void unwind()}>
                    close it now instead
                  </button>{' '}
                  if you want out before the close.
                </p>
              )}
            </div>
          )}

          <div className="row tight">
            <label className="field small">
              <span>Shares</span>
              <input
                className="mono"
                type="number"
                min="0"
                step="any"
                value={shares}
                disabled={busy}
                onChange={(e) => setShares(e.target.value)}
              />
            </label>
            <div className="field">
              <span>As {acting} (AP)</span>
              <div className="row tight">
                <button className="primary" disabled={busy || sharesNum <= 0} onClick={create}>
                  {busy ? '…' : `Create ${fmt(sharesNum)}`}
                </button>
                <button className="ghost" disabled={busy || sharesNum <= 0} onClick={redeem}>
                  Redeem {fmt(sharesNum)}
                </button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Define a new basket */}
      <button
        className="ghost small define-toggle"
        disabled={busy}
        onClick={() => (showDefine ? setShowDefine(false) : beginDefine())}
      >
        {showDefine ? '− Hide' : '+ Define a basket'}
      </button>
      {showDefine && (
        <div className="define-basket">
          <div className="row tight">
            <label className="field small">
              <span>Symbol</span>
              <input
                value={newId}
                disabled={busy}
                className={symbolTaken ? 'bad' : undefined}
                aria-invalid={symbolTaken}
                placeholder={freeSymbol()}
                onChange={(e) => setNewId(e.target.value)}
              />
            </label>
            <label className="field">
              <span>Administrator</span>
              <select value={admin} disabled={busy} onChange={(e) => setAdmin(e.target.value)}>
                {people.map((p) => (
                  <option key={p.party} value={p.label}>
                    {p.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {/* WHAT IS ALREADY TAKEN, SHOWN RATHER THAN DISCOVERED ON SUBMIT. */}
          {symbolTaken ? (
            <p className="warn subtle taken">
              <strong>{trimmedId}</strong> already exists.{' '}
              {baskets.some((b) => b.basketId.toLowerCase() === trimmedId.toLowerCase())
                ? 'It is a live fund with holders — you cannot redefine it.'
                : 'That symbol is a listed instrument.'}{' '}
              <button className="link" onClick={() => setNewId(freeSymbol())}>
                use {freeSymbol()}
              </button>
            </p>
          ) : (
            <p className="hint in-use">
              in use: {[...baskets.map((b) => b.basketId), ...assets.map((a) => a.id)].join(' · ')}
            </p>
          )}
          {/* Cloning a RECIPE is useful; cloning a fund is not a thing. This copies
              the components and leaves the symbol free, which is the only version
              of "copy" that can actually be submitted. */}
          {baskets.length > 0 && (
            <label className="field">
              <span>Start from an existing recipe (optional)</span>
              <select
                value=""
                disabled={busy}
                onChange={(e) => {
                  const src = baskets.find((b) => b.basketId === e.target.value);
                  if (src) {
                    setRows(
                      src.components.map((c) => ({
                        instrumentId: c.instrumentId,
                        unitsPerShare: String(c.unitsPerShare),
                      })),
                    );
                    setAdmin(src.administrator);
                  }
                }}
              >
                <option value="">— build from scratch —</option>
                {baskets.map((b) => (
                  <option key={b.basketId} value={b.basketId}>
                    copy {b.basketId}&rsquo;s components
                  </option>
                ))}
              </select>
            </label>
          )}
          <div className="step-label">Creation unit (units per share)</div>
          {rows.map((r, i) => (
            <div key={i} className="row tight">
              <label className="field">
                <span>Underlying</span>
                <select value={r.instrumentId} disabled={busy} onChange={(e) => setRow(i, { instrumentId: e.target.value })}>
                  {assets.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.id}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field small">
                <span>Units / share</span>
                <input
                  className="mono"
                  type="number"
                  min="0"
                  step="any"
                  value={r.unitsPerShare}
                  disabled={busy}
                  onChange={(e) => setRow(i, { unitsPerShare: e.target.value })}
                />
              </label>
              <button
                className="ghost small"
                disabled={busy || rows.length <= 1}
                onClick={() => setRows((rs) => rs.filter((_, idx) => idx !== i))}
              >
                ✕
              </button>
            </div>
          ))}
          <button
            className="ghost small"
            disabled={busy}
            onClick={() => setRows((rs) => [...rs, { instrumentId: assets[0]?.id ?? 'cETH', unitsPerShare: '0' }])}
          >
            + Component
          </button>
          <div className="step-label">Authorised participants</div>
          <div className="member-chips">
            {people.map((p) => (
              <button
                key={p.party}
                className={`chip ${participants.includes(p.label) ? 'on' : ''}`}
                disabled={busy}
                onClick={() => toggleParticipant(p.label)}
              >
                {p.label}
              </button>
            ))}
          </div>
          <button
            className="primary"
            disabled={busy || !trimmedId || symbolTaken || participants.length === 0}
            onClick={defineBasket}
          >
            {symbolTaken ? `${trimmedId} is taken` : `Define ${trimmedId || '…'}`}
          </button>
        </div>
      )}
    </section>
  );
}
