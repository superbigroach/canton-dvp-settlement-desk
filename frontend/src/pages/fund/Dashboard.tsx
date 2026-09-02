// Fund admin · the fund as its administrator sees it: NAV series (chart + table),
// shares outstanding, the creation/redemption log, fee accruals, who references it.
import { useParams } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { desk, type FundDashboard as Dash } from '../../desk';
import LineChart from '../../components/LineChart';
import { fmtN, fmtQty, fmtTs, LoadState, shortCid, Stat, TierTag, useAsync } from '../../components/ui';
import { ReceiptTable } from '../ap/Receipts';

const DEFAULT_FUND = 'LX1';

export default function FundDashboard() {
  const { id } = useParams();
  const { me } = useAuth();
  const fundId = id || me?.instruments?.[0] || DEFAULT_FUND;
  const d = useAsync<Dash>(() => desk.fundDashboard(fundId), [fundId]);
  const data = d.data;
  const last = data?.series[0];
  const points = data ? [...data.series].reverse().map((r) => ({ x: r.date, y: r.price, tier: r.tier, tierLabel: r.tierLabel, restated: r.restated })) : [];
  const fallback = data ? data.series.filter((r) => r.tier >= 2).length : 0;
  const seeded = data ? data.series.filter((r) => r.tier === 0).length : 0;

  return (
    <div className="page">
      <div className="page-head">
        <h1>{data?.name ?? fundId}</h1>
        <p className="hint">Official NAV series in gold. Fallback tiers and restatements are drawn hollow and labelled.</p>
        <button type="button" className="ghost small" onClick={d.reload}>Refresh</button>
      </div>
      <LoadState loading={d.loading} error={d.error} onRetry={d.reload}>
        {data && (
          <>
            <div className="stat-row">
              <Stat label={last && last.tier === 1 ? 'Last official NAV' : 'Last NAV'} gold={last?.tier === 1} value={last ? fmtN(last.price) : '—'}
                sub={last ? <><TierTag tier={last.tier} k={last.k} n={last.n} label={last.tierLabel} /> <span className="mono muted">{fmtTs(last.asOf)}</span></> : 'no fixing yet'} />
              <Stat label="Shares outstanding" value={fmtQty(data.sharesOutstanding)} />
              <Stat label="Fees accrued" value={`${fmtN(data.fees.accrued)} ${data.fees.currency}`} />
              <Stat label="Fixings" value={data.series.length}
                sub={`${fallback} by fallback${seeded ? ` · ${seeded} seeded` : ''} · ${data.series.filter((r) => r.restated).length} restated`} />
            </div>
            <div className="card">
              <h2>NAV series</h2>
              <LineChart points={points} ariaLabel={`${data.name} NAV per share, ${points.length} fixings`} />
              <div className="table-wrap">
                <table className="blotter">
                  <thead><tr><th>Date</th><th className="num">NAV</th><th>Attestation</th><th>Signers</th><th>Fixing cid</th><th></th></tr></thead>
                  <tbody>
                    {data.series.slice(0, 30).map((r, i) => (
                      <tr key={`${r.fixingCid ?? r.asOf}-${i}`}>
                        <td className="mono">{r.date}</td>
                        <td className={`num mono${r.tier === 1 ? ' official' : ''}`}>{fmtN(r.price)}</td>
                        <td><TierTag tier={r.tier} k={r.k} n={r.n} label={r.tierLabel} /></td>
                        <td className="small">{r.signers.join(', ') || '—'}</td>
                        <td className="mono muted" title={r.fixingCid}>{shortCid(r.fixingCid)}</td>
                        <td>{r.restated && <span className="tag">restated</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
            <div className="two-col">
              <div className="card">
                <h2>Fees</h2>
                {data.fees.rows.length === 0 ? <div className="empty">No fee accruals yet.</div> : (
                  <div className="table-wrap">
                    <table className="blotter">
                      <thead><tr><th>Date</th><th>Kind</th><th className="num">Amount</th><th>Ref</th></tr></thead>
                      <tbody>
                        {data.fees.rows.map((f, i) => (
                          <tr key={i}>
                            <td className="mono">{f.date}</td><td>{f.kind}</td>
                            <td className="num mono">{fmtN(f.amount)} {data.fees.currency}</td>
                            <td className="mono muted">{f.ref ?? '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
              <div className="card">
                <h2>Basket &amp; licensees</h2>
                {data.components && (
                  <ul className="plain mono">
                    {data.components.map((c) => <li key={c.instrumentId}>{fmtQty(c.unitsPerShare)} {c.instrumentId} per share</li>)}
                  </ul>
                )}
                {data.licensees && data.licensees.length > 0 ? (
                  <ul className="plain">{data.licensees.map((l, i) => <li key={i}>{l.name} <span className="muted">· {l.kind}</span></li>)}</ul>
                ) : <div className="empty">No licensees recorded.</div>}
              </div>
            </div>
            <h2 className="section-h">Creations and redemptions</h2>
            {data.log.length === 0 ? <div className="empty">No creations or redemptions yet.</div> : <ReceiptTable rows={data.log} showParty />}
          </>
        )}
      </LoadState>
    </div>
  );
}
