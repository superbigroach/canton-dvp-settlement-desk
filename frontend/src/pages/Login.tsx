// /desk/login — Google only, or (sandbox builds only) a users.yml identity.
import { useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { SANDBOX_USERS } from '../auth/sandboxUsers';
import { sectionsFor } from '../shell/Shell';

export default function Login() {
  const auth = useAuth();
  const loc = useLocation();
  // Sign-in is Google-only; the e-mail is still held because the sandbox path selects a
  // roster user by address, and that is the only place it is used now.
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState<'email' | 'google' | 'sandbox' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const from = (loc.state as { from?: string } | null)?.from;
  if (auth.status === 'ready' && auth.me) {
    // Go back to where the session expired — but only if THIS role has that section. Signing
    // out on /admin and back in as a signer used to land the signer on "NOT YOUR SECTION".
    const mine = sectionsFor(auth.me.role, auth.degraded);
    const back = from && from !== '/login' && mine.some((s) => from === s.path || from.startsWith(`${s.path}/`));
    return <Navigate to={back ? from : '/'} replace />;
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

  return (
    <div className="login">
      <div className="login-card card">
        <a className="brand" href="/" title="etpfoundry.com">
          <span className="logo" aria-hidden>◈</span>
          <div className="brand-text">
            <span className="brand-name">ETP FOUNDRY</span>
            <span className="brand-sub">sign in to the desk</span>
          </div>
        </a>

        {auth.status === 'loading' && <div className="empty" role="status">Checking your session…</div>}

        {auth.mode === 'firebase' && (
          <>
            {/* GOOGLE ONLY. A seat is issued to a named person, never self-registered, so the
                address is what matters and the password is just one more secret for a
                counterparty's engineer to mislay or leak. Federated sign-in also means we never
                hold a credential we could lose. The e-mail and password form is deliberately gone
                rather than hidden; if it is ever needed for a seat whose firm forbids Google, add
                it back behind a flag rather than leaving an unused field on the page. */}
            <button type="button" className="primary wide" disabled={busy !== null}
              onClick={() => void run('google', () => auth.signInGoogle())}>
              {busy === 'google' ? 'Opening Google…' : 'Continue with Google'}
            </button>
            <p className="hint subtle">
              Accounts are created by ETP Foundry. Signing in with an address that has no role shows an empty desk, not an error.
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
                <div key={u.email} role="listitem">
                  <button type="button" className="sandbox-user" disabled={busy !== null}
                    onClick={() => void run('sandbox', () => auth.signInSandbox(u.email))}>
                    <span className="sandbox-name">{u.displayName}</span>
                    <span className="sandbox-note mono">{u.note}</span>
                    <span className="sandbox-email muted">{u.email}</span>
                  </button>
                </div>
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
        Not a regulated benchmark administrator · values are struck on our own Canton DevNet validator · <a href="/">etpfoundry.com</a>
      </p>
    </div>
  );
}
