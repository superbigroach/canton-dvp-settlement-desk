// Admin "View as": look at (and act on) the desk as any mapped user. The choice rides
// on every request as X-Act-As; the backend decides whether to honour it.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { desk, type Role, type UserRow } from '../desk';
import { useAsync } from '../components/ui';

const ROLE_ORDER: Role[] = ['admin', 'signer', 'ap', 'fund_admin', 'auditor', 'viewer'];

export function ViewAsSelect() {
  const auth = useAuth();
  const users = useAsync<UserRow[]>(() => desk.usersAsSelf(), []);
  const [busy, setBusy] = useState(false);
  const groups = ROLE_ORDER
    .map((role) => ({ role, rows: (users.data ?? []).filter((u) => u.role === role) }))
    .filter((g) => g.rows.length > 0);

  const onChange = async (email: string) => {
    setBusy(true);
    try {
      if (email) await auth.startActAs(email);
      else await auth.stopActAs();
    } finally {
      setBusy(false);
    }
  };

  return (
    <label className="view-as" htmlFor="view-as">
      <span>View as</span>
      <select id="view-as" value={auth.actAs ?? ''} disabled={busy || users.loading} onChange={(e) => void onChange(e.target.value)}
        title={users.error ? `Users could not be loaded — ${users.error}` : 'Look at the desk as another mapped user'}>
        <option value="">myself</option>
        {groups.map((g) => (
          <optgroup key={g.role} label={g.role}>
            {g.rows.map((u) => (
              <option key={u.uid} value={u.email}>
                {u.email} · {u.role} · {u.seat ?? u.party ?? '—'}
              </option>
            ))}
          </optgroup>
        ))}
        {users.error && <option value="" disabled>users unavailable</option>}
      </select>
    </label>
  );
}

export function ViewAsBanner() {
  const auth = useAuth();
  if (!auth.actAs) return null;
  const me = auth.me;
  const seat = me?.seat ? `${me.seat} seat` : me?.party ? `party ${me.party}` : me?.role ?? '';
  return (
    <div className="banner warn view-as-banner" role="status">
      <span>
        {auth.actAsUnsupported
          ? <>View as <strong>{auth.actAs}</strong> — not supported by this backend yet (<code>/api/me</code> did not echo <code>actingAs</code>). You are still yourself.</>
          : <>Viewing as <strong>{auth.actAs}</strong>{seat ? ` (${seat})` : ''} — actions run as this user.</>}
      </span>
      <span className="view-as-actions">
        <Link to="/admin" className="link">Back to admin</Link>
        <button type="button" className="ghost small" onClick={() => void auth.stopActAs()}>Exit</button>
      </span>
    </div>
  );
}
