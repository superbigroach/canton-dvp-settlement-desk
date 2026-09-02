// Auditor · Series — every published benchmark value, with tier, K of N, signers, cid.
import { useState } from 'react';
import { errorMessage } from '../../api';
import { desk, type Benchmark, type SeriesRow } from '../../desk';
import LineChart from '../../components/LineChart';
import { fmtN, fmtTs, LoadState, shortCid, TierTag, useAsync } from '../../components/ui';

export default function AuditSeries() {
  const bench = useAsync<Benchmark[]>(() => desk.benchmarks(), []);
  const [id, setId] = useState<string>('');
  const chosen = id || bench.data?.[0]?.id || '';
  const series = useAsync<SeriesRow[]>(() => (chosen ? desk.series(chosen, { limit: 250 }) : Promise.resolve([])), [chosen]);
  const [csvError, setCsvError] = useState<string | null>(null);
  const points = [...(series.data ?? [])].reverse().map((r) => ({ x: r.date, y: r.price, tier: r.tier, tierLabel: r.tierLabel, restated: r.restated }));

  return (
    <div className="page">
      <div className="page-head">
        <h1>Series</h1>
        <p className="hint">The published history. A restated value stays in the series with its restatement flagged; nothing is deleted.</p>
      </div>
      <LoadState loading={bench.loading} error={bench.error} onRetry={bench.reload}
        empty={bench.data && bench.data.length === 0 ? 'No benchmarks published.' : null}>
        <div className="card">
          <div className="row tight">
            <label className="field small" htmlFor="series-id"><span>Benchmark</span>
              <select id="series-id" value={chosen} onChange={(e) => setId(e.target.value)}>
                {(bench.data ?? []).map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select></label>
            <div className="field small actions"><span>&nbsp;</span>
              <button type="button" className="ghost" disabled={!chosen} onClick={() => { setCsvError(null); desk.seriesCsv(chosen).catch((e) => setCsvError(errorMessage(e))); }}>Download CSV</button>
            </div>
          </div>
          {csvError && <div className="banner error" role="alert"><span>{csvError}</span></div>}
          <LoadState loading={series.loading} error={series.error} onRetry={series.reload}
            empty={series.data && series.data.length === 0 ? 'No values published for this benchmark yet.' : null}>
            <LineChart points={points} ariaLabel={`${chosen} series, ${points.length} values`} />
            <div className="table-wrap">
              <table className="blotter">
                <thead><tr><th>Date</th><th>As of</th><th className="num">Value</th><th className="num">Reference</th><th className="num">Factor</th><th>Attestation</th><th>Signers</th><th>cid</th><th></th></tr></thead>
                <tbody>
                  {(series.data ?? []).map((r, i) => (
                    <tr key={`${r.fixingCid ?? r.asOf}-${i}`}>
                      <td className="mono">{r.date}</td>
                      <td className="mono muted when">{fmtTs(r.asOf)}</td>
                      <td className={`num mono${r.tier === 1 ? ' official' : ''}`}>{fmtN(r.price)}</td>
                      <td className="num mono muted">{r.referencePrice !== undefined ? fmtN(r.referencePrice) : '—'}</td>
                      <td className="num mono muted">{r.wrapperFactor !== undefined ? r.wrapperFactor : '—'}</td>
                      <td><TierTag tier={r.tier} k={r.k} n={r.n} label={r.tierLabel} /></td>
                      <td className="small">{r.signers.join(', ') || '—'}</td>
                      <td className="mono muted" title={r.fixingCid}>{shortCid(r.fixingCid)}</td>
                      <td>{r.restated && <span className="tag">restated</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </LoadState>
        </div>
      </LoadState>
    </div>
  );
}
