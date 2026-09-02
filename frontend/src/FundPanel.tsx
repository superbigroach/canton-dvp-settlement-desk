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
  // Every basket is also published as a `Fund` instrument so its shares can trade
  // through the cross. Called after anything that can add or re-mark one, so the
  // desk reloads its instrument list and the fund shows up in the Asset picker.
  onInstrumentsChanged?: () => void;
  flash: (m: string) => void;
}

const CASH = 'USDC';
const fmt = (n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 6 });
const fmt2 = (n: number) => n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface Row {
  instrumentId: string;
  unitsPerShare: string;
}

export default function FundPanel({
  parties,
  instruments,
  acting,
  onChanged,
  onInstrumentsChanged,
  flash,
}: Props) {
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

  // Same reasoning as App.runAction: the arb is two ledger writes, so a double click
  // could start a second basket leg before the first finished and leave two
  // market-on-close orders resting against one create or redeem.
  const inFlight = useRef(false);
  const [busy, setBusy] = useState(false);
  const [step, setStep] = useState<string>('');
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

  const sharesNum = Number((Number(shares) || 0).toFixed(10));

  // WHAT THE ACTING PARTY ACTUALLY HOLDS gates every button below. A button that lets
  // you press Redeem with no shares only teaches you the backend's error text.
  const [held, setHeld] = useState<Record<string, number>>({});
  const reloadHeld = useCallback(async () => {
    try {
      const hs = await api.holdings(acting);
      const m: Record<string, number> = {};
      for (const h of hs) m[h.instrumentId] = (m[h.instrumentId] ?? 0) + Number(h.amount);
      setHeld(m);
    } catch {
      /* keep the last known holdings */
    }
  }, [acting]);
  useEffect(() => {
    void reloadHeld();
  }, [reloadHeld]);

  const sharesHeld = basket ? (held[basket.basketId] ?? 0) : 0;
  const cashHeld = held[CASH] ?? 0;
  const shortUnits = basket
    ? basket.components
        .map((c) => ({
          id: c.instrumentId,
          need: Number(c.unitsPerShare) * sharesNum,
          have: held[c.instrumentId] ?? 0,
        }))
        .filter((c) => c.have + 1e-9 < c.need)
    : [];
  const canCreate = sharesNum > 0 && !!basket && shortUnits.length === 0;
  const canRedeem = sharesNum > 0 && !!basket && sharesHeld + 1e-9 >= sharesNum;
  // An unpriced buy reserves the collar ceiling (anchor + 10%) per share until the print.
  const buyReserve = officialNav != null ? officialNav * 1.1 * sharesNum : 0;
  const canRedeemBuy = canRedeem && cashHeld + 1e-9 >= buyReserve;
  const createWhy = shortUnits.length
    ? `needs ${shortUnits.map((c) => `${fmt(c.need)} ${c.id} (you hold ${fmt(c.have)})`).join(', ')}`
    : '';
  const redeemWhy =
    basket && sharesHeld + 1e-9 < sharesNum
      ? `needs ${fmt(sharesNum)} ${basket.basketId} (you hold ${fmt(sharesHeld)})`
      : '';
  const buyWhy =
    canRedeem && cashHeld + 1e-9 < buyReserve
      ? `buy MOC reserves ${fmt2(buyReserve)} ${CASH} (you hold ${fmt2(cashHeld)})`
      : '';

  // A create or redeem changes holdings AND may re-mark the fund instrument, so
  // both the desk's holdings and its instrument list are refreshed.
  function changed() {
    onChanged();
    onInstrumentsChanged?.();
    void reloadHeld();
  }

  async function create() {
    if (!basket) return;
    const res = await run(() => api.basketCreate({ basketId: basket.basketId, ap: acting, shares: sharesNum }));
    if (!res) return;
    flash(
      `Created ${fmt(res.shares)} ${basket.basketId} in-kind${
        res.navPerShare != null ? ` · NAV ${fmt2(res.navPerShare)} ${CASH}/share` : ''
      }.`,
    );
    changed();
    await loadBaskets();
  }

  async function redeem() {
    if (!basket) return;
    const res = await run(() => api.basketRedeem({ basketId: basket.basketId, ap: acting, shares: sharesNum }));
    if (!res) return;
    flash(`Redeemed ${fmt(res.shares)} ${basket.basketId} in-kind — underlyings returned.`);
    changed();
    await loadBaskets();
  }

  /**
   * THE WHOLE ARB, IN ORDER, OR NOT AT ALL.
   *
   * Two ledger writes: the basket leg at the signed NAV, then the opposite side in
   * SHARES as an unpriced market-on-close order on the fund itself. They are
   * SEQUENTIAL on purpose — each one is a separate Daml transaction against a shared
   * node, so firing them together would race, and a sell that lands before the shares
   * exist has nothing to deliver. If either leg fails the sequence STOPS and says which
   * one, because the honest failure is "you are half in" and the dishonest one is a
   * green toast over a broken book.
   *
   * `redeem` picks the direction. Basket rich (units worth more than the signed NAV):
   * redeem at the mark, buy the shares back at the close. Basket cheap: create at the
   * mark, sell the shares at the close. Either way the position is flat once the Venue
   * runs the cross — no synthetic leg, nothing left open to unwind.
   */
  async function runArb(redeem: boolean) {
    if (inFlight.current || !basket || sharesNum <= 0) return;
    inFlight.current = true;
    setBusy(true);
    setErr('');
    try {
      // 1 — the basket leg, settled at the SIGNED mark. Redeem hands shares back for the
      //     basket; create delivers the basket for shares. Both are in kind, atomic.
      setStep(redeem ? 'redeeming…' : 'creating…');
      if (redeem) await api.basketRedeem({ basketId: basket.basketId, ap: acting, shares: sharesNum });
      else await api.basketCreate({ basketId: basket.basketId, ap: acting, shares: sharesNum });

      // 2 — the market leg, the opposite side, in SHARES, at the close. Unpriced
      //     market-on-close on the fund instrument itself: it takes the print the
      //     venue's cross discovers, which is anchored to the official NAV. Redeemed?
      //     buy the shares back at the close. Created? sell them at the close. Flat
      //     by the close — nothing synthetic, nothing left open.
      setStep('sending MOC…');
      await api.mocOrder({
        trader: acting,
        side: redeem ? 'Buy' : 'Sell',
        quantity: Number(sharesNum.toFixed(10)),
        instrumentId: basket.basketId,
        cashInstrument: CASH,
        session: 'Close',
        orderType: 'Market',
      });

      flash(
        `${redeem ? 'Redeemed' : 'Created'} ${fmt(sharesNum)} ${basket.basketId} at the signed NAV, ` +
          `${redeem ? 'buy' : 'sell'} MOC resting — it prints when the Venue runs the close.`,
      );
      changed();
      await loadBaskets();
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
    // The new basket is now a listed `Fund` instrument — tell the desk so it shows
    // up in the Asset picker without a page refresh.
    onInstrumentsChanged?.();
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
              redeem settle at (official). Capturing it is two legs — the basket leg
              at the signed NAV, and a market-on-close order on the shares the other
              way — and doing them by hand across two cards is how a leg gets left
              off. One button per direction runs the sequence in order and stops at
              the first failure rather than half-arbing. */}
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
                  outside. Either side below still runs if you want it; the close prints when the Venue runs the cross.
                </p>
              ) : (
                <p className="arb-play">
                  {arb.above ? (
                    <>The underlyings are worth <strong>more</strong> than the signed NAV. Hand shares back
                    at the lower official mark, take the richer units, and buy the shares back at the
                    close: <strong>redeem</strong>, then <strong>buy market-on-close</strong>.</>
                  ) : (
                    <>The underlyings are worth <strong>less</strong> than the signed NAV. Deliver the cheap
                    units, receive shares at the higher official mark, and sell them at the close:{' '}
                    <strong>create</strong>, then <strong>sell market-on-close</strong>.</>
                  )}{' '}
                  <strong className="mono">{fmt2(arb.edgePerShare * sharesNum)}</strong> {CASH} on{' '}
                  {fmt(sharesNum)} shares.
                </p>
              )}
              {/* BOTH SIDES, ALWAYS, IN FIXED COLOURS. Green creates and sells at the close;
                  red redeems and buys at the close. Nothing is "recommended" — the strip
                  above says which way the gap points, and the trader decides. A side you
                  cannot fund is disabled, and the reason is printed under it. */}
              <div className="arb-actions">
                <button
                  className="primary venue"
                  disabled={busy || !canCreate}
                  onClick={() => void runArb(false)}
                  title="Deliver the basket, receive shares at the signed NAV, sell them market-on-close. Prints when the Venue runs the close."
                >
                  {busy ? step || 'Working…' : `Create ${fmt(sharesNum)} · sell MOC`}
                  {!busy && createWhy && <span className="rec">{createWhy}</span>}
                </button>
                <button
                  className="primary sell"
                  disabled={busy || !canRedeemBuy}
                  onClick={() => void runArb(true)}
                  title="Hand shares back at the signed NAV, receive the basket, buy the shares back market-on-close. Prints when the Venue runs the close."
                >
                  {busy ? step || 'Working…' : `Redeem ${fmt(sharesNum)} · buy MOC`}
                  {!busy && (redeemWhy || buyWhy) && <span className="rec">{redeemWhy || buyWhy}</span>}
                </button>
              </div>
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
                <button className="primary" disabled={busy || !canCreate} onClick={create} title={createWhy || 'Deliver the basket, receive shares — in kind, atomic'}>
                  {busy ? '…' : `Create ${fmt(sharesNum)}`}
                </button>
                <button className="ghost" disabled={busy || !canRedeem} onClick={redeem} title={redeemWhy || 'Hand shares back, receive the basket — in kind, atomic'}>
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
