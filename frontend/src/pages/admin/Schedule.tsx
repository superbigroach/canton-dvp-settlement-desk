// Admin · Schedule — per-instrument strike time, window, tiers; and Strike now.
import { useEffect, useState } from 'react';
import { errorMessage } from '../../api';
import { desk, type ScheduleRow, type StrikeResult } from '../../desk';
import { ConfirmDialog, fmtN, LoadState, shortCid, useAsync } from '../../components/ui';

const blank = (): ScheduleRow => ({ instrument: '', session: 'Close', strikeAt: '16:00', timezone: 'Europe/London', windowMinutes: 30, enabled: true, tiers: { t2: false, t3: true, t4: true } });

export default function Schedule() {
  const loaded = useAsync<ScheduleRow[]>(() => desk.schedule(), []);
  const [rows, setRows] = useState<ScheduleRow[] | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [strike, setStrike] = useState<string | null>(null);
  const [striking, setStriking] = useState(false);
  const [struck, setStruck] = useState<{ id: string; r: StrikeResult } | null>(null);
  const [strikeError, setStrikeError] = useState<string | null>(null);

  useEffect(() => { if (loaded.data && rows === null) setRows(loaded.data); }, [loaded.data, rows]);

  const update = (i: number, patch: Partial<ScheduleRow>) =>
    setRows((r) => (r ? r.map((row, j) => (j === i ? { ...row, ...patch } : row)) : r));

  const save = async () => {
    if (!rows) return;
    setSaving(true); setError(null); setSaved(null);
    try {
      const r = await desk.saveSchedule(rows.filter((x) => x.instrument.trim()));
      setRows(r);
      setSaved(`Saved at ${new Date().toLocaleTimeString()}`);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const doStrike = async () => {
    if (!strike) return;
    setStriking(true); setStrikeError(null);
    try {
      const r = await desk.strikeNow(strike);
      setStruck({ id: strike, r });
      setStrike(null);
    } catch (e) {
      setStrikeError(errorMessage(e));
    } finally {
      setStriking(false);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>Schedule</h1>
        <p className="hint">When each instrument is proposed, how long the committee has, and which fallback tiers may run if K is not reached.</p>
      </div>
      <LoadState loading={loaded.loading && !rows} error={loaded.error} onRetry={loaded.reload}>
        {rows && (
          <form className="card" onSubmit={(e) => { e.preventDefault(); void save(); }}>
            <div className="table-wrap">
              <table className="blotter editable">
                <thead>
                  <tr><th>Instrument</th><th>Session</th><th>Strike at</th><th>Timezone</th><th className="num">Window (min)</th><th>Tier 2</th><th>Tier 3</th><th>Tier 4</th><th>Enabled</th><th></th></tr>
                </thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={i}>
                      <td><input aria-label="Instrument" className="mono" value={r.instrument} onChange={(e) => update(i, { instrument: e.target.value })} /></td>
                      <td>
                        <select aria-label="Session" value={r.session ?? 'Close'} onChange={(e) => update(i, { session: e.target.value })}>
                          <option>Close</option><option>Open</option>
                        </select>
                      </td>
                      <td><input aria-label="Strike time" type="time" className="mono" value={r.strikeAt} onChange={(e) => update(i, { strikeAt: e.target.value })} /></td>
                      <td><input aria-label="Timezone" value={r.timezone} onChange={(e) => update(i, { timezone: e.target.value })} /></td>
                      <td><input aria-label="Window minutes" type="number" min={1} className="mono num" value={r.windowMinutes} onChange={(e) => update(i, { windowMinutes: Number(e.target.value) })} /></td>
                      <td><input aria-label="Tier 2 alternate seats" type="checkbox" checked={r.tiers.t2} onChange={(e) => update(i, { tiers: { ...r.tiers, t2: e.target.checked } })} /></td>
                      <td><input aria-label="Tier 3 benchmark times factor" type="checkbox" checked={r.tiers.t3} onChange={(e) => update(i, { tiers: { ...r.tiers, t3: e.target.checked } })} /></td>
                      <td><input aria-label="Tier 4 prior fixing" type="checkbox" checked={r.tiers.t4} onChange={(e) => update(i, { tiers: { ...r.tiers, t4: e.target.checked } })} /></td>
                      <td><input aria-label="Enabled" type="checkbox" checked={r.enabled} onChange={(e) => update(i, { enabled: e.target.checked })} /></td>
                      <td className="actions-cell">
                        <button type="button" className="ghost small" disabled={!r.instrument.trim()} onClick={() => { setStrikeError(null); setStrike(r.instrument); }}>Strike now</button>
                        <button type="button" className="link" onClick={() => setRows((x) => (x ? x.filter((_, j) => j !== i) : x))}>remove</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="hint subtle">Tier 1 is the committee. Tier 2 alternate seats, tier 3 benchmark × last factor (automatic), tier 4 prior fixing flagged, tier 5 missed — always recorded as an event.</p>
            <div className="proposal-actions">
              <button type="submit" className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save schedule'}</button>
              <button type="button" className="ghost" onClick={() => setRows((r) => [...(r ?? []), blank()])}>Add instrument</button>
              {saved && <span className="good mono small">{saved}</span>}
            </div>
            {error && <div className="banner error" role="alert"><span>{error}</span></div>}
          </form>
        )}
      </LoadState>
      {struck && (
        <div className="banner ok" role="status">
          <span>
            Proposed {struck.id}{struck.r.price !== undefined ? ` at ${fmtN(struck.r.price)}` : ''}
            {struck.r.proposalCid ? ` · proposal ${shortCid(struck.r.proposalCid)}` : ''}{struck.r.note ? ` — ${struck.r.note}` : '. Seats have been notified.'}
          </span>
        </div>
      )}
      <ConfirmDialog open={strike !== null} title={`Strike ${strike ?? ''} now?`} confirmLabel="Propose now" busy={striking}
        onConfirm={() => void doStrike()} onCancel={() => setStrike(null)}>
        <p>Runs the propose step immediately: computes the price, creates the on-ledger proposal, notifies every seat. The window starts now.</p>
        {strikeError && <div className="banner error" role="alert"><span>{strikeError}</span></div>}
      </ConfirmDialog>
    </div>
  );
}
