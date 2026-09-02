// Admin · Fallback status — per instrument: where today's fixing stands against its
// schedule, and the tier of the last published value. Built from the two routes that
// already exist (/api/fixing-schedule and /api/benchmarks); no new contract needed.
import { desk, type Benchmark, type FixingSchedule } from '../../desk';
import { fmtN, fmtTs, LoadState, TierTag, useAsync } from '../../components/ui';

export default function Fallback() {
  const sched = useAsync<FixingSchedule>(() => desk.fixingSchedule(), []);
  const bench = useAsync<Benchmark[]>(() => desk.benchmarks(), []);
  const rows = sched.data?.identifiers ?? [];
  const lastFor = (id: string) => bench.data?.find((b) => b.id === id)?.last ?? null;

  return (
    <div className="page">
      <div className="page-head">
        <h1>Fallback status</h1>
        <p className="hint">Tier 1 is the committee. Anything else is a fallback and is shown as such on every published value.</p>
        <button type="button" className="ghost small" onClick={() => { sched.reload(); bench.reload(); }}>Refresh</button>
      </div>
      {sched.data && sched.data.overdueCount > 0 && (
        <div className="banner warn" role="status"><span>{sched.data.overdueCount} fixing{sched.data.overdueCount === 1 ? ' is' : 's are'} overdue as of {fmtTs(sched.data.asOf)}.</span></div>
      )}
      {bench.error && <div className="banner warn" role="status"><span>Published values unavailable — {bench.error}</span></div>}
      <LoadState loading={sched.loading} error={sched.error} onRetry={sched.reload}
        empty={sched.data && rows.length === 0 ? 'No instruments scheduled.' : null}>
        <div className="card table-wrap">
          <table className="blotter">
            <thead><tr><th>Instrument</th><th>Strike</th><th>State</th><th className="num">Late (min)</th><th>Note</th><th className="num">Last published</th><th>Tier</th><th>Age</th></tr></thead>
            <tbody>
              {rows.map((r) => {
                const last = lastFor(r.instrumentId);
                const state = (r.state || '').toUpperCase();
                return (
                  <tr key={`${r.instrumentId}-${r.session}`}>
                    <td className="mono">{r.instrumentId} <span className="muted">{r.session}</span></td>
                    <td className="mono">{r.strikeAt} <span className="muted">{r.zone}</span></td>
                    <td><span className={`tag state ${state.toLowerCase()}`}>{state || '—'}</span></td>
                    <td className="num mono">{r.minutesLate > 0 ? r.minutesLate : '—'}</td>
                    <td className="small">{r.note || '—'}</td>
                    <td className={`num mono${last && last.tier === 1 ? ' official' : ''}`}>{last ? fmtN(last.price) : '—'}</td>
                    <td>{last ? <TierTag tier={last.tier} k={last.k} n={last.n} /> : <span className="muted">none</span>}</td>
                    <td className="mono muted">{last ? `${Math.round(last.ageSeconds / 60)} min` : '—'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </LoadState>
    </div>
  );
}
