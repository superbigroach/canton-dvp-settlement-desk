// AP portal · Fund detail — the official NAV (gold), the indicative (not gold), the
// basket, what N shares means in units either way, the fee, the cutoff, and Create /
// Redeem behind a confirmation that repeats the numbers.
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../../api';
import { desk, type ApFund, type Receipt } from '../../desk';
import { ConfirmDialog, Countdown, fmtN, fmtQty, fmtTs, isOfficial, LoadState, NumberField, shortCid, Stat, TierTag, useAsync } from '../../components/ui';

export default function FundDetail() {
  const { id = '' } = useParams();
  const funds = useAsync<ApFund[]>(() => desk.apFunds(), []);
  const fund = funds.data?.find((f) => f.id === id) ?? null;

  const [shares, setShares] = useState('');
  const [side, setSide] = useState<'create' | 'redeem'>('create');
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [receipt, setReceipt] = useState<Receipt | null>(null);

  const n = Number(shares);
  const valid = fund !== null && Number.isFinite(n) && n > 0
    && (!fund.minShares || n >= fund.minShares) && (!fund.lot || Math.abs(n / fund.lot - Math.round(n / fund.lot)) < 1e-9);

  const preview = useMemo(() => {
    if (!fund || !valid) return null;
    const nav = fund.official?.nav ?? null;
    const units = fund.components.map((c) => ({ instrumentId: c.instrumentId, amount: c.unitsPerShare * n }));
    const bps = side === 'create' ? fund.fee.createBps : fund.fee.redeemBps;
    const notional = nav !== null ? nav * n : null;
    const fee = notional !== null ? Math.max(fund.fee.minimum ?? 0, (notional * bps) / 10_000) : null;
    return { nav, units, notional, fee, bps };
  }, [fund, valid, n, side]);

  const submit = async () => {
    if (!fund || !valid) return;
    setBusy(true); setError(null);
    try {
      const r = side === 'create' ? await desk.apCreate(fund.id, n) : await desk.apRedeem(fund.id, n);
      setReceipt(r);
      setConfirming(false);
      setShares('');
      funds.reload();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const cutoffPassed = fund?.cutoff.nextAt ? new Date(fund.cutoff.nextAt).getTime() < Date.now() : false;
  // Gold only when a committee attested the NAV. A seeded value still prices the order —
  // the backend settles at it — but it is not official and must not look it.
  const gold = isOfficial(fund?.official?.tier);
  const tierWord = fund?.official ? (fund.official.tierLabel ?? (fund.official.tier === 0 ? 'seed' : `tier ${fund.official.tier}`)) : '';
  const navLabel = gold || !fund?.official ? 'Official NAV' : `NAV (${tierWord} — not attested)`;
  const navCls = `mono${gold ? ' official' : ''}`;

  return (
    <div className="page">
      <div className="page-head">
        <h1><Link to="/ap" className="crumb">Funds</Link> / {fund?.name ?? id}</h1>
      </div>
      <LoadState loading={funds.loading} error={funds.error} onRetry={funds.reload}
        empty={funds.data && !fund ? `You are not an authorised participant of ${id}.` : null}>
        {fund && (
          <>
            <div className="stat-row">
              <Stat label={navLabel} gold={gold} value={fund.official ? fmtN(fund.official.nav) : '—'}
                sub={fund.official ? <><TierTag tier={fund.official.tier} k={fund.official.k} n={fund.official.n} label={fund.official.tierLabel} /> <span className="mono muted">{fmtTs(fund.official.asOf)}</span></> : 'no official NAV today'} />
              <Stat label="Indicative (not official)" value={fund.indicative !== undefined && fund.indicative !== null ? fmtN(fund.indicative) : '—'} sub="from live marks; nothing settles at it" />
              <Stat label="Shares outstanding" value={fund.sharesOutstanding !== undefined ? fmtQty(fund.sharesOutstanding) : '—'} />
              <Stat label="Cutoff" value={<>{fund.cutoff.time} <span className="muted small">{fund.cutoff.timezone}</span></>}
                sub={fund.cutoff.nextAt ? (cutoffPassed ? 'today\'s cutoff has passed' : <Countdown to={fund.cutoff.nextAt} />) : undefined} />
            </div>

            <div className="two-col">
              <div className="card">
                <h2>Basket per share</h2>
                <div className="table-wrap">
                  <table className="blotter">
                    <thead><tr><th>Component</th><th className="num">Units / share</th><th className="num">Mark</th></tr></thead>
                    <tbody>
                      {fund.components.map((c) => (
                        <tr key={c.instrumentId}>
                          <td><span className="pill asset">{c.instrumentId}</span></td>
                          <td className="num mono">{fmtQty(c.unitsPerShare)}</td>
                          <td className="num mono muted">{c.mark !== undefined && c.mark !== null ? fmtN(c.mark) : '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="hint subtle">
                  Fee {fund.fee.createBps} bps to create, {fund.fee.redeemBps} bps to redeem
                  {fund.fee.minimum ? `, minimum ${fmtN(fund.fee.minimum)} ${fund.fee.currency ?? fund.cash}` : ''}.
                  {fund.minShares ? ` Minimum ${fmtQty(fund.minShares)} shares` : ''}{fund.lot ? `, lots of ${fmtQty(fund.lot)}` : ''}.
                </p>
              </div>

              <form className="card" onSubmit={(e) => { e.preventDefault(); if (valid) setConfirming(true); }}>
                <h2>{side === 'create' ? 'Create' : 'Redeem'} in kind</h2>
                <div className="seg-toggle" role="tablist" aria-label="Create or redeem">
                  <button type="button" role="tab" aria-selected={side === 'create'} className={side === 'create' ? 'on' : ''} onClick={() => setSide('create')}>Create</button>
                  <button type="button" role="tab" aria-selected={side === 'redeem'} className={side === 'redeem' ? 'on' : ''} onClick={() => setSide('redeem')}>Redeem</button>
                </div>
                <NumberField id="shares" label="Shares" value={shares} onChange={setShares} min={0} step={fund.lot ? String(fund.lot) : 'any'} placeholder="0"
                  hint={shares !== '' && !valid ? `Enter a positive number${fund.minShares ? ` of at least ${fund.minShares}` : ''}${fund.lot ? ` in lots of ${fund.lot}` : ''}.` : undefined} />
                <div className="preview">
                  <div className="preview-title">{side === 'create' ? 'You deliver' : 'You receive'}</div>
                  {preview ? (
                    <ul className="preview-units mono">
                      {preview.units.map((u) => <li key={u.instrumentId}><span>{u.instrumentId}</span><span>{fmtQty(u.amount)}</span></li>)}
                    </ul>
                  ) : <div className="empty">Enter a share count to see the units.</div>}
                  <div className="preview-title">{side === 'create' ? 'You receive' : 'You deliver'}</div>
                  <div className="mono">{valid ? `${fmtQty(n)} shares of ${fund.id}` : '—'}</div>
                  <dl className="kv">
                    <dt>{gold ? 'At official NAV' : 'At NAV (not attested)'}</dt><dd className={navCls}>{preview?.nav !== null && preview?.nav !== undefined ? fmtN(preview.nav) : '—'}</dd>
                    <dt>Notional</dt><dd className="mono">{preview?.notional !== null && preview?.notional !== undefined ? `${fmtN(preview.notional)} ${fund.cash}` : '—'}</dd>
                    <dt>Fee ({preview?.bps ?? (side === 'create' ? fund.fee.createBps : fund.fee.redeemBps)} bps)</dt>
                    <dd className="mono">{preview?.fee !== null && preview?.fee !== undefined ? `${fmtN(preview.fee)} ${fund.fee.currency ?? fund.cash}` : '—'}</dd>
                  </dl>
                </div>
                {!fund.official && <p className="hint subtle bad-text">No official NAV today — orders cannot be priced.</p>}
                <button type="submit" className={`primary${side === 'redeem' ? ' sell' : ''}`} disabled={!valid || !fund.official || busy}>
                  Review {side === 'create' ? 'creation' : 'redemption'}
                </button>
                {error && <div className="banner error" role="alert"><span>{error}</span></div>}
              </form>
            </div>

            {receipt && (
              <div className="card receipt-card">
                <div className="card-head"><h2>Receipt</h2><span className={`tag status ${receipt.status}`}>{receipt.status}</span></div>
                <p className="mono">
                  {receipt.kind === 'create' ? 'Created' : 'Redeemed'} {fmtQty(receipt.shares)} {receipt.fundId} at NAV <span className={isOfficial(receipt.navTier) ? 'official' : ''}>{fmtN(receipt.nav)}</span>
                  {' '}— units {receipt.units.map((u) => `${fmtQty(u.amount)} ${u.instrumentId}`).join(', ')} — fee {fmtN(receipt.fee)} {receipt.feeCurrency ?? fund.cash}
                  {' '}· {fmtTs(receipt.ts)}{receipt.cid ? ` · cid ${shortCid(receipt.cid)}` : ''}
                </p>
                {receipt.note && <p className="hint subtle">{receipt.note}</p>}
                <Link to="/ap/receipts" className="link">All receipts</Link>
              </div>
            )}

            <ConfirmDialog open={confirming} title={`Confirm ${side === 'create' ? 'creation' : 'redemption'}`} confirmLabel={side === 'create' ? 'Create shares' : 'Redeem shares'}
              busy={busy} danger={side === 'redeem'} onConfirm={() => void submit()} onCancel={() => setConfirming(false)}>
              {preview && (
                <dl className="kv">
                  <dt>Shares</dt><dd className="mono">{fmtQty(n)} {fund.id}</dd>
                  <dt>{side === 'create' ? 'You deliver' : 'You receive'}</dt>
                  <dd className="mono">{preview.units.map((u) => `${fmtQty(u.amount)} ${u.instrumentId}`).join(' + ')}</dd>
                  <dt>{gold ? 'Official NAV' : 'NAV (not attested)'}</dt><dd className={navCls}>{preview.nav !== null ? fmtN(preview.nav) : '—'}</dd>
                  <dt>Fee</dt><dd className="mono">{preview.fee !== null ? `${fmtN(preview.fee)} ${fund.fee.currency ?? fund.cash}` : '—'}</dd>
                </dl>
              )}
              <p className="hint subtle">Settles atomically on the ledger as {side === 'create' ? 'units in, shares out' : 'shares in, units out'}. There is no partial fill.</p>
              {error && <div className="banner error" role="alert"><span>{error}</span></div>}
            </ConfirmDialog>
          </>
        )}
      </LoadState>
    </div>
  );
}
