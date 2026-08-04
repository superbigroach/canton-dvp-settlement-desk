// FundPanel — the ETF / TOKENISED-FUND BUILDER. A basket token (e.g. LX1 = 0.10
// cETH + 0.01 CBTC per share) is created and redeemed IN-KIND: an authorised
// participant delivers the exact underlyings and receives freshly-minted shares
// (or the reverse), atomically — the mechanism that keeps an ETF glued to NAV.
// NAV per share = Σ (unitsPerShare × close mark); the marks are the committee-
// attested prices, so the basket inherits a credibly-neutral NAV.

import { useCallback, useEffect, useState } from 'react';
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
  const [newId, setNewId] = useState<string>('LX1');
  const [rows, setRows] = useState<Row[]>([
    { instrumentId: 'cETH', unitsPerShare: '0.1' },
    { instrumentId: 'CBTC', unitsPerShare: '0.01' },
  ]);
  const [participants, setParticipants] = useState<string[]>(['Alice', 'Bob']);

  const [busy, setBusy] = useState(false);
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
  const sharesNum = Number(shares) || 0;

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

  function toggleParticipant(label: string) {
    setParticipants((ps) => (ps.includes(label) ? ps.filter((x) => x !== label) : [...ps, label]));
  }

  function setRow(i: number, patch: Partial<Row>) {
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));
  }

  async function defineBasket() {
    const components = rows
      .filter((r) => r.instrumentId && Number(r.unitsPerShare) > 0)
      .map((r) => ({ instrumentId: r.instrumentId, unitsPerShare: Number(r.unitsPerShare) }));
    if (components.length === 0) {
      setErr('add at least one component with a positive unitsPerShare');
      return;
    }
    const res = await run(() =>
      api.defineBasket({
        administrator: admin,
        basketId: newId,
        components,
        participants,
        description: `${newId} tokenised basket`,
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
      <button className="ghost small define-toggle" disabled={busy} onClick={() => setShowDefine((s) => !s)}>
        {showDefine ? '− Hide' : '+ Define a basket'}
      </button>
      {showDefine && (
        <div className="define-basket">
          <div className="row tight">
            <label className="field small">
              <span>Symbol</span>
              <input value={newId} disabled={busy} onChange={(e) => setNewId(e.target.value)} />
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
          <button className="primary" disabled={busy || !newId || participants.length === 0} onClick={defineBasket}>
            Define {newId}
          </button>
        </div>
      )}
    </section>
  );
}
