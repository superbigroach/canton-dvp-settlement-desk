// /desk/login — email + password, Google, or (sandbox builds only) a users.yml identity.
import { useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { SANDBOX_USERS } from '../auth/sandboxUsers';

export default function Login() {
  const auth = useAuth();
  const loc = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState<'email' | 'google' | 'sandbox' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const from = (loc.state as { from?: string } | null)?.from;
  if (auth.status === 'ready' && auth.me) {
    return <Navigate to={from && from !== '/login' ? from : '/'} replace />;
  }

  const run = async (kind: 'email' | 'google' | 'sandbox', fn: () => Promise<void>) => {
    setBusy(kind); setError(null);
    try {
      await fn();
    } catch (e) {
      const { firebaseErrorMessage } = await import('../auth/firebase');
      setError(firebaseErrorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  const onEmail = (e: FormEvent) => {
    e.preventDefault();
    void run('email', () => auth.signInEmail(email.trim(), password));
  };

  return (
    <div className="login">
      <div className="login-card card">
        <a className="brand" href="/" title="crossdesk.app">
          <span className="logo" aria-hidden>◈</span>
          <div className="brand-text">
            <span className="brand-name">CROSSDESK</span>
            <span className="brand-sub">sign in to the desk</span>
          </div>
        </a>

        {auth.status === 'loading' && <div className="empty" role="status">Checking your session…</div>}

        {auth.mode === 'firebase' && (
          <>
            <form onSubmit={onEmail} className="login-form">
              <label className="field" htmlFor="login-email">
                <span>Email</span>
                <input id="login-email" type="email" autoComplete="username" required value={email}
                  onChange={(e) => setEmail(e.target.value)} />
              </label>
              <label className="field" htmlFor="login-password">
                <span>Password</span>
                <input id="login-password" type="password" autoComplete="current-password" required value={password}
                  onChange={(e) => setPassword(e.target.value)} />
              </label>
              <button type="submit" className="primary" disabled={busy !== null}>
                {busy === 'email' ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
            <div className="login-or"><span>or</span></div>
            <button type="button" className="ghost wide" disabled={busy !== null}
              onClick={() => void run('google', () => auth.signInGoogle())}>
              {busy === 'google' ? 'Opening Google…' : 'Continue with Google'}
            </button>
            <p className="hint subtle">
              Accounts are created by CrossDesk. Signing in with an address that has no role shows an empty desk, not an error.
            </p>
          </>
        )}

        {auth.mode === 'sandbox' && (
          <>
            <p className="hint">
              <strong>Sandbox identities.</strong> This build sends <code>X-Sandbox-User</code>; the backend honours it only
              when it runs with <code>AUTH_MODE=sandbox</code>. Pick a seat:
            </p>
            <div className="sandbox-list" role="list">
              {SANDBOX_USERS.map((u) => (
                <button key={u.email} type="button" role="listitem" className="sandbox-user" disabled={busy !== null}
                  onClick={() => void run('sandbox', () => auth.signInSandbox(u.email))}>
                  <span className="sandbox-name">{u.displayName}</span>
                  <span className="sandbox-note mono">{u.note}</span>
                  <span className="sandbox-email muted">{u.email}</span>
                </button>
              ))}
            </div>
            <form className="login-form" onSubmit={(e) => { e.preventDefault(); if (email.trim()) void run('sandbox', () => auth.signInSandbox(email.trim())); }}>
              <label className="field" htmlFor="sandbox-email">
                <span>Or any email in users.yml</span>
                <input id="sandbox-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="someone@example.com" />
              </label>
              <button type="submit" className="ghost wide" disabled={busy !== null || !email.trim()}>Use this address</button>
            </form>
          </>
        )}

        {error && <div className="banner error" role="alert"><span>{error}</span></div>}
      </div>
      <p className="login-foot muted">
        Not a regulated benchmark administrator · values are struck on a hosted sandbox · <a href="/">crossdesk.app</a>
      </p>
    </div>
  );
}
