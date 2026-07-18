// FundPanel — the ETF / TOKENISED-FUND BUILDER. A basket token (e.g. LX1 = 0.10
// cETH + 0.01 CBTC per share) is created and redeemed IN-KIND: an authorised
// participant delivers the exact underlyings and receives freshly-minted shares
// (or the reverse), atomically — the mechanism that keeps an ETF glued to NAV.
// NAV per share = Σ (unitsPerShare × close mark); the marks are the committee-
// attested prices, so the basket inherits a credibly-neutral NAV.

import { useCallback, useEffect, useState } from 'react';
import {
  api,
  ApiError,
  type Basket,
  type Instrument,
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
      setErr(e instanceof ApiError ? e.message : e instanceof Error ? e.message : String(e));
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
              <div className="nav-line">
                <span className="nav-label">NAV / share</span>
                <span className="mono nav">
                  {nav && nav.navPerShare != null ? `${fmt2(nav.navPerShare)} ${CASH}` : '—'}
                </span>
              </div>
              {nav && (
                <table className="blotter nav-breakdown">
                  <tbody>
                    {nav.legs.map((l) => (
                      <tr key={l.instrumentId}>
                        <td>
                          <span className="pill asset">{l.instrumentId}</span>
                        </td>
                        <td className="num mono muted">
                          {fmt(l.unitsPerShare)} × {l.price != null ? fmt2(l.price) : '—'}
                        </td>
                        <td className="num mono strong">{l.value != null ? fmt2(l.value) : '—'}</td>
                      </tr>
                    ))}
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
