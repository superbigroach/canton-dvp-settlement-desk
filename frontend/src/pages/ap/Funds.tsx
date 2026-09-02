// AP portal · Funds — one row per fund I am an authorised participant of.
import { Link } from 'react-router-dom';
import { desk, type ApFund } from '../../desk';
import { fmtN, fmtQty, fmtTs, LoadState, TierTag, useAsync } from '../../components/ui';

export default function Funds() {
  const funds = useAsync<ApFund[]>(() => desk.apFunds(), []);
  return (
    <div className="page">
      <div className="page-head">
        <h1>Funds</h1>
        <p className="hint">Creation and redemption in kind, at the attested NAV. Official values in gold; indicative values are not.</p>
        <button type="button" className="ghost small" onClick={funds.reload}>Refresh</button>
      </div>
      <LoadState loading={funds.loading} error={funds.error} onRetry={funds.reload}
        empty={funds.data && funds.data.length === 0 ? 'You are not yet an authorised participant of any fund.' : null}>
        <div className="card table-wrap">
          <table className="blotter">
            <thead>
              <tr><th>Fund</th><th className="num">Official NAV</th><th>As of</th><th className="num">Indicative</th><th className="num">Shares out</th><th className="num">Fee (bps)</th><th>Cutoff</th><th></th></tr>
            </thead>
            <tbody>
              {(funds.data ?? []).map((f) => (
                <tr key={f.id}>
                  <td><Link to={`/ap/funds/${encodeURIComponent(f.id)}`} className="strong">{f.name}</Link> <span className="mono muted">{f.id}</span></td>
                  <td className="num mono official">{f.official ? fmtN(f.official.nav) : '—'}</td>
                  <td>{f.official ? <><span className="mono muted">{fmtTs(f.official.asOf)}</span> <TierTag tier={f.official.tier} k={f.official.k} n={f.official.n} /></> : <span className="muted">no fixing</span>}</td>
                  <td className="num mono muted">{f.indicative !== undefined && f.indicative !== null ? fmtN(f.indicative) : '—'}</td>
                  <td className="num mono">{f.sharesOutstanding !== undefined ? fmtQty(f.sharesOutstanding) : '—'}</td>
                  <td className="num mono">{f.fee.createBps} / {f.fee.redeemBps}</td>
                  <td className="mono">{f.cutoff.time} <span className="muted">{f.cutoff.timezone}</span></td>
                  <td><Link to={`/ap/funds/${encodeURIComponent(f.id)}`} className="link">Create / Redeem</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </LoadState>
    </div>
  );
}
