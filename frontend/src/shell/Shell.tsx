// The signed-in frame: brand, who you are (org · party · seat), the sections your role
// has, sign out. Nothing here asserts a ledger or a role the backend did not give.
import { Navigate, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import type { Role } from '../desk';
import { ViewAsBanner, ViewAsSelect } from './ViewAs';

export interface Section { path: string; label: string; roles: Role[] }

export const SECTIONS: Section[] = [
  { path: '/sign', label: 'Sign', roles: ['signer'] },
  { path: '/ap', label: 'Funds', roles: ['ap'] },
  { path: '/fund', label: 'Fund admin', roles: ['fund_admin'] },
  { path: '/admin', label: 'Admin', roles: ['admin'] },
  { path: '/ops', label: 'Operator desk', roles: ['admin'] },
  { path: '/audit', label: 'Audit', roles: ['auditor', 'admin'] },
];

export const sectionsFor = (role: Role | undefined, degraded: boolean): Section[] => {
  const mine = SECTIONS.filter((s) => role && s.roles.includes(role));
  // The identity route is missing: the only thing that provably works is today's desk.
  if (degraded && mine.length === 0) return SECTIONS.filter((s) => s.path === '/ops');
  return mine;
};

const SUBNAV: Record<string, { to: string; label: string; end?: boolean }[]> = {
  '/sign': [
    { to: '/sign', label: 'Open proposals', end: true },
    { to: '/sign/history', label: 'History' },
    { to: '/sign/settings', label: 'Settings' },
  ],
  '/ap': [
    { to: '/ap', label: 'Funds', end: true },
    { to: '/ap/receipts', label: 'Receipts' },
  ],
  '/fund': [{ to: '/fund', label: 'Dashboard', end: true }],
  '/admin': [
    { to: '/admin', label: 'Schedule', end: true },
    { to: '/admin/committees', label: 'Committees' },
    { to: '/admin/users', label: 'Users & roles' },
    { to: '/admin/events', label: 'Events' },
    { to: '/admin/fallback', label: 'Fallback status' },
  ],
  '/audit': [
    { to: '/audit', label: 'Events', end: true },
    { to: '/audit/series', label: 'Series' },
  ],
  '/ops': [],
};

export default function Shell() {
  const auth = useAuth();
  const loc = useLocation();

  if (auth.status === 'loading') {
    return <div className="shell-loading" role="status">Signing you in…</div>;
  }
  if (auth.status === 'signedOut' || !auth.me) {
    return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  }

  const me = auth.me;
  const realAdmin = auth.adminMe?.role === 'admin' || (!auth.actAs && me.role === 'admin');
  const sections = sectionsFor(me.role, auth.degraded);
  const current = SECTIONS.find((s) => loc.pathname === s.path || loc.pathname.startsWith(s.path + '/'));
  // While viewing as someone else the nav shows THEIR sections, but the admin's own
  // stay reachable (the banner's "Back to admin" link) — the acted-as user cannot open them.
  const allowed = !current || sections.some((s) => s.path === current.path)
    || (realAdmin && current.roles.includes('admin'));
  const isOps = current?.path === '/ops';

  return (
    <div className="shell">
      <header className="shell-top">
        <a className="brand" href="/" title="crossdesk.app">
          <span className="logo" aria-hidden>◈</span>
          <div className="brand-text">
            <span className="brand-name">CROSSDESK</span>
            <span className="brand-sub">the desk</span>
          </div>
        </a>
        <div className="shell-who">
          <span className="who-line">
            <strong>{me.displayName || me.email}</strong>
            <span className="muted"> · {me.email}</span>
          </span>
          <span className="who-line mono">
            {me.org && <span className="tag">{me.org}</span>}
            {me.party && <span className="tag">party {me.party}</span>}
            <span className="tag role">{me.role}{me.seat ? ` · ${me.seat} seat` : ''}</span>
            {auth.mock && <span className="tag mock" title="VITE_API_MOCK=1 — invented numbers">mock data</span>}
            {auth.mode === 'sandbox' && <span className="tag mock" title="X-Sandbox-User header">sandbox auth</span>}
          </span>
        </div>
        <div className="shell-top-actions">
          {realAdmin && <ViewAsSelect />}
          <button type="button" className="ghost small" onClick={() => void auth.signOut()}>Sign out</button>
        </div>
      </header>

      <ViewAsBanner />

      {auth.meError && (
        <div className={`banner ${auth.degraded ? 'warn' : 'error'}`} role="status">
          <span>{auth.degraded ? 'Limited mode — ' : ''}{auth.meError}</span>
          <button type="button" className="ghost small" onClick={() => void auth.refreshMe()}>Retry</button>
        </div>
      )}

      <div className="shell-body">
        <nav className="shell-nav" aria-label="Sections">
          {sections.length === 0 && <div className="nav-empty">No sections for role <code>{me.role}</code>.</div>}
          {sections.map((s) => (
            <div key={s.path} className="nav-group">
              <NavLink to={s.path} end={s.path !== current?.path} className={({ isActive }) => `nav-link${isActive || current?.path === s.path ? ' on' : ''}`}>
                {s.label}
              </NavLink>
              {current?.path === s.path && SUBNAV[s.path]?.length > 0 && (
                <div className="nav-sub">
                  {SUBNAV[s.path].map((l) => (
                    <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => `nav-sub-link${isActive ? ' on' : ''}`}>
                      {l.label}
                    </NavLink>
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>
        <main className={`shell-main${isOps ? ' ops' : ''}`}>
          {allowed ? <Outlet /> : (
            <div className="card">
              <h2>Not your section</h2>
              <p className="hint">Your role <code>{me.role}</code> does not include <code>{current?.label}</code>.
                {sections.length > 0 && <> Go to <NavLink to={sections[0].path}>{sections[0].label}</NavLink>.</>}
              </p>
            </div>
          )}
        </main>
      </div>
      <footer className="foot">
        CrossDesk is not a regulated benchmark administrator. Values shown are struck on a hosted sandbox unless the
        page says otherwise; tier and age are shown on every published value.
      </footer>
    </div>
  );
}

/** `/desk` → the role's first section, or an explanation when there is none. */
export function Home() {
  const auth = useAuth();
  if (auth.status === 'loading') return <div className="shell-loading" role="status">Signing you in…</div>;
  if (auth.status === 'signedOut' || !auth.me) return <Navigate to="/login" replace />;
  const sections = sectionsFor(auth.me.role, auth.degraded);
  if (sections.length > 0) return <Navigate to={sections[0].path} replace />;
  return (
    <div className="card">
      <h2>No role yet</h2>
      <p className="hint">
        You are signed in as <strong>{auth.me.email}</strong>, but CrossDesk has not mapped that address to a role,
        party and seat. Ask CrossDesk to add you (Admin → Users &amp; roles), then sign in again.
      </p>
      {auth.meError && <p className="error">{auth.meError}</p>}
      <button type="button" className="ghost" onClick={() => void auth.refreshMe()}>Check again</button>
    </div>
  );
}
