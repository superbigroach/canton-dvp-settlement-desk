// Events — the audit trail (fixing_events), filterable, downloadable as CSV.
// Shared by Admin (writes allowed elsewhere) and Auditor (read-only mirror routes).
import { useState } from 'react';
import { errorMessage } from '../../api';
import { desk, type FixingEvent } from '../../desk';
import { fmtN, fmtTs, LoadState, shortCid, useAsync } from '../../components/ui';

export function EventsView({ base }: { base: 'admin' | 'audit' }) {
  const [instrument, setInstrument] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [applied, setApplied] = useState({ instrument: '', from: '', to: '' });
  const [csvError, setCsvError] = useState<string | null>(null);
  const [csvBusy, setCsvBusy] = useState(false);
  const list = useAsync<FixingEvent[]>(() => desk.events(applied, base), [applied.instrument, applied.from, applied.to, base]);

  const download = async () => {
    setCsvBusy(true); setCsvError(null);
    try { await desk.eventsCsv(applied, base); } catch (e) { setCsvError(errorMessage(e)); } finally { setCsvBusy(false); }
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>Events</h1>
        <p className="hint">Every step of every fixing: proposal, notification, attestation, refusal, restrike, finalisation, fallback. This is the audit export.</p>
      </div>
      <form className="card filters" onSubmit={(e) => { e.preventDefault(); setApplied({ instrument: instrument.trim(), from, to }); }}>
        <div className="row tight">
          <label className="field small" htmlFor="ev-instr"><span>Instrument</span>
            <input id="ev-instr" className="mono" value={instrument} onChange={(e) => setInstrument(e.target.value)} placeholder="all" /></label>
          <label className="field small" htmlFor="ev-from"><span>From</span>
            <input id="ev-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
          <label className="field small" htmlFor="ev-to"><span>To</span>
            <input id="ev-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} /></label>
          <div className="field small actions">
            <span>&nbsp;</span>
            <div className="proposal-actions">
              <button type="submit" className="ghost">Apply</button>
              <button type="button" className="ghost" disabled={csvBusy} onClick={() => void download()}>{csvBusy ? 'Preparing…' : 'Download CSV'}</button>
            </div>
          </div>
        </div>
        {csvError && <div className="banner error" role="alert"><span>{csvError}</span></div>}
      </form>
      <LoadState loading={list.loading} error={list.error} onRetry={list.reload}
        empty={list.data && list.data.length === 0 ? 'No events in this range.' : null}>
        <div className="card table-wrap">
          <table className="blotter">
            <thead><tr><th>When</th><th>Instrument</th><th>Kind</th><th>Actor</th><th className="num">Price</th><th>Reason</th><th>Proposal</th><th>cid</th></tr></thead>
            <tbody>
              {(list.data ?? []).map((e, i) => (
                <tr key={e.id ?? i}>
                  <td className="mono when">{fmtTs(e.ts)}</td>
                  <td className="mono">{e.instrument}</td>
                  <td><span className={`tag ev ${e.kind.replace(/\W+/g, '-')}`}>{e.kind}</span>{e.tier !== undefined && e.tier !== 1 && <span className="tag fallback"> tier {e.tier}</span>}</td>
                  <td>{e.actor ?? '—'}</td>
                  <td className="num mono">{e.price !== undefined ? fmtN(e.price) : '—'}</td>
                  <td className="small wrap">{e.reason ?? '—'}</td>
                  <td className="mono muted" title={e.proposalCid}>{shortCid(e.proposalCid)}</td>
                  <td className="mono muted" title={e.cid}>{shortCid(e.cid)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </LoadState>
    </div>
  );
}

export default function Events() {
  return <EventsView base="admin" />;
}
