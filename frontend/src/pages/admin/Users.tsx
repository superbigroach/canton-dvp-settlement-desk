// Admin · Users & roles — the email → role, party, seat mapping (§3).
import { useState, type FormEvent } from 'react';
import { errorMessage } from '../../api';
import { desk, type Role, type Seat, type UserInput, type UserRow } from '../../desk';
import { LoadState, useAsync } from '../../components/ui';

const ROLES: Role[] = ['admin', 'signer', 'ap', 'fund_admin', 'auditor', 'viewer'];
const SEATS: Seat[] = ['issuer', 'lender', 'venue'];
const empty = (): UserInput => ({ email: '', role: 'viewer', party: '', org: '', displayName: '', seat: undefined, instruments: [] });

export default function Users() {
  const list = useAsync<UserRow[]>(() => desk.users(), []);
  const [editing, setEditing] = useState<string | 'new' | null>(null);
  const [form, setForm] = useState<UserInput>(empty());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const startNew = () => { setForm(empty()); setEditing('new'); setError(null); };
  const startEdit = (u: UserRow) => {
    const { uid: _uid, ...rest } = u;
    setForm({ ...rest, instruments: rest.instruments ?? [] });
    setEditing(u.uid); setError(null);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true); setError(null);
    const body: UserInput = {
      ...form, email: form.email.trim(), party: form.party.trim(), org: form.org.trim(), displayName: form.displayName.trim(),
      seat: form.role === 'signer' ? form.seat : undefined,
      instruments: (form.instruments ?? []).map((s) => s.trim()).filter(Boolean),
    };
    try {
      if (editing === 'new') {
        const r = await desk.addUser(body);
        list.setData((prev) => [...(prev ?? []), r]);
      } else if (editing) {
        const r = await desk.updateUser(editing, body);
        list.setData((prev) => (prev ? prev.map((u) => (u.uid === editing ? r : u)) : prev));
      }
      setEditing(null);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>Users &amp; roles</h1>
        <p className="hint">Who maps to which role, party and seat. In production the party lives on the institution's own participant; the mapping is the same.</p>
        <button type="button" className="ghost small" onClick={startNew}>Add user</button>
      </div>

      {editing && (
        <form className="card" onSubmit={(e) => void submit(e)}>
          <h2>{editing === 'new' ? 'New user' : 'Edit user'}</h2>
          <div className="row">
            <label className="field" htmlFor="u-email"><span>Email</span>
              <input id="u-email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
            <label className="field" htmlFor="u-name"><span>Display name</span>
              <input id="u-name" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></label>
            <label className="field" htmlFor="u-org"><span>Organisation</span>
              <input id="u-org" value={form.org} onChange={(e) => setForm({ ...form, org: e.target.value })} /></label>
          </div>
          <div className="row">
            <label className="field" htmlFor="u-role"><span>Role</span>
              <select id="u-role" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select></label>
            <label className="field" htmlFor="u-party"><span>Party</span>
              <input id="u-party" className="mono" required value={form.party} onChange={(e) => setForm({ ...form, party: e.target.value })} placeholder="Issuer" /></label>
            {form.role === 'signer' && (
              <label className="field" htmlFor="u-seat"><span>Seat</span>
                <select id="u-seat" value={form.seat ?? ''} required onChange={(e) => setForm({ ...form, seat: (e.target.value || undefined) as Seat | undefined })}>
                  <option value="">choose…</option>
                  {SEATS.map((s) => <option key={s} value={s}>{s}</option>)}
                </select></label>
            )}
            {(form.role === 'signer' || form.role === 'fund_admin' || form.role === 'ap') && (
              <label className="field" htmlFor="u-instr"><span>Instruments (comma-separated)</span>
                <input id="u-instr" className="mono" value={(form.instruments ?? []).join(', ')}
                  onChange={(e) => setForm({ ...form, instruments: e.target.value.split(',') })} placeholder="CBTC, cETH, LX1" /></label>
            )}
          </div>
          <div className="proposal-actions">
            <button type="submit" className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
            <button type="button" className="ghost" disabled={busy} onClick={() => setEditing(null)}>Cancel</button>
          </div>
          {error && <div className="banner error" role="alert"><span>{error}</span></div>}
        </form>
      )}

      <LoadState loading={list.loading} error={list.error} onRetry={list.reload}
        empty={list.data && list.data.length === 0 ? 'No users mapped yet.' : null}>
        <div className="card table-wrap">
          <table className="blotter">
            <thead><tr><th>Email</th><th>Name</th><th>Org</th><th>Role</th><th>Seat</th><th>Party</th><th>Instruments</th><th></th></tr></thead>
            <tbody>
              {(list.data ?? []).map((u) => (
                <tr key={u.uid}>
                  <td className="mono small">{u.email}</td>
                  <td>{u.displayName}</td>
                  <td>{u.org}</td>
                  <td><span className="tag role">{u.role}</span></td>
                  <td className="mono">{u.seat ?? '—'}</td>
                  <td className="mono">{u.party}</td>
                  <td className="mono small">{u.instruments?.join(', ') || '—'}</td>
                  <td><button type="button" className="link" onClick={() => startEdit(u)}>edit</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </LoadState>
    </div>
  );
}
