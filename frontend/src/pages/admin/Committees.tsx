// Admin · Committees — the roster per instrument: seats, parties, users, last action.
import { desk, type Committee } from '../../desk';
import { fmtTs, LoadState, useAsync } from '../../components/ui';

export default function Committees() {
  const list = useAsync<Committee[]>(() => desk.committees(), []);
  return (
    <div className="page">
      <div className="page-head">
        <h1>Committees</h1>
        <p className="hint">Who attests each instrument. K of N is the threshold; a seat with no user and no key cannot sign.</p>
        <button type="button" className="ghost small" onClick={list.reload}>Refresh</button>
      </div>
      <LoadState loading={list.loading} error={list.error} onRetry={list.reload}
        empty={list.data && list.data.length === 0 ? 'No committees configured.' : null}>
        <div className="committee-grid">
          {(list.data ?? []).map((c) => (
            <div key={c.instrument} className="card">
              <div className="card-head">
                <h2>{c.instrument}</h2>
                <span className="tag">K {c.k} of N {c.n}</span>
              </div>
              <div className="table-wrap">
                <table className="blotter">
                  <thead><tr><th>Seat</th><th>Party</th><th>Users</th><th>Last action</th></tr></thead>
                  <tbody>
                    {c.seats.map((s) => (
                      <tr key={s.seat}>
                        <td className="mono">{s.seat}</td>
                        <td className="mono">{s.party}</td>
                        <td className="small">{s.users.length ? s.users.join(', ') : <span className="muted">none — automated only or unassigned</span>}</td>
                        <td className="small">{s.lastAction ? <>{s.lastAction.kind} <span className="mono muted">{fmtTs(s.lastAction.at)}</span></> : <span className="muted">never</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      </LoadState>
    </div>
  );
}
