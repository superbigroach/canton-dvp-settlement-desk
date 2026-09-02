// Signer portal · History — what I signed and refused, with on-ledger cids.
import { desk, type Proposal } from '../../desk';
import { fmtN, fmtTs, LoadState, shortCid, useAsync } from '../../components/ui';

export default function History() {
  const list = useAsync<Proposal[]>(() => desk.proposals('all'), []);
  const rows = (list.data ?? []).filter((p) => p.mine || p.status !== 'open')
    .sort((a, b) => (b.mine?.at ?? b.proposedAt).localeCompare(a.mine?.at ?? a.proposedAt));
  return (
    <div className="page">
      <div className="page-head">
        <h1>History</h1>
        <p className="hint">Your signatures and refusals, newest first, with the contract id each action produced.</p>
        <button type="button" className="ghost small" onClick={list.reload}>Refresh</button>
      </div>
      <LoadState loading={list.loading} error={list.error} onRetry={list.reload}
        empty={list.data && rows.length === 0 ? 'No signatures yet.' : null}>
        <div className="card table-wrap">
          <table className="blotter">
            <thead>
              <tr><th>When</th><th>Instrument</th><th className="num">Proposed</th><th>My action</th><th>Verified / reason</th><th>Outcome</th><th>cid</th></tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.cid}>
                  <td className="mono">{fmtTs(p.mine?.at ?? p.proposedAt)}</td>
                  <td>{p.instrument}{p.session ? ` · ${p.session}` : ''}</td>
                  <td className="num mono">{fmtN(p.price)}</td>
                  <td>{p.mine ? <span className={`tag status ${p.mine.action}`}>{p.mine.action}</span> : <span className="muted">—</span>}</td>
                  <td className="mono small">{p.mine?.checks?.join(', ') || p.mine?.reason || '—'}</td>
                  <td><span className={`tag status ${p.status}`}>{p.status}</span></td>
                  <td className="mono muted" title={p.mine?.cid ?? p.cid}>{shortCid(p.mine?.cid ?? p.cid)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </LoadState>
    </div>
  );
}
