// Who is signed in, and what CrossDesk says they are.
//
// Two facts, two sources. Firebase (or the sandbox header) says WHO; `/api/me` says what
// ROLE, PARTY and SEAT that person maps to — the backend owns the mapping (§3). The app
// never guesses a role from an email in production. It does degrade honestly: when the
// identity route is not there yet (404 / unreachable) the shell still renders, says so,
// and offers only what needs no role — never a blank screen.
import {
  createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode,
} from 'react';
import { ApiError, errorMessage } from '../api';
import { desk, MOCK, type Me } from '../desk';
import { AUTH_MODE, actAsUser, installTokenSource, sandboxUser, setActAsUser, setSandboxUser, type AuthMode } from './token';
import { findSandboxUser } from './sandboxUsers';

export type AuthStatus = 'loading' | 'signedOut' | 'ready';

export interface AuthState {
  mode: AuthMode;
  mock: boolean;
  status: AuthStatus;
  /** The identity as CrossDesk sees it. Null while loading or signed out. */
  me: Me | null;
  /** Why /api/me could not be read, when it could not. */
  meError: string | null;
  /** HTTP status behind `meError` (0 = unreachable, -1 = a fault in this app), or null. */
  meStatus: number | null;
  /** True when the identity route was unavailable and `me` is a placeholder. */
  degraded: boolean;
  /** Admin "View as": the real (admin) identity while `me` is the acted-as user. */
  adminMe: Me | null;
  /** The email being viewed as, or null. */
  actAs: string | null;
  /** Set when the backend did not echo `actingAs` — the header is not honoured yet. */
  actAsUnsupported: boolean;
  startActAs: (email: string) => Promise<void>;
  stopActAs: () => Promise<void>;
  signInEmail: (email: string, password: string) => Promise<void>;
  signInGoogle: () => Promise<void>;
  signInSandbox: (email: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshMe: () => Promise<void>;
}

const Ctx = createContext<AuthState | null>(null);

interface Identity { email: string; displayName: string; uid: string }

const placeholder = (id: Identity): Me => ({
  uid: id.uid, email: id.email, displayName: id.displayName || id.email,
  role: 'viewer', party: '', org: '',
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [identity, setIdentity] = useState<Identity | null>(null);
  const [me, setMe] = useState<Me | null>(null);
  const [meError, setMeError] = useState<string | null>(null);
  const [meStatus, setMeStatus] = useState<number | null>(null);
  const [degraded, setDegraded] = useState(false);
  const [adminMe, setAdminMe] = useState<Me | null>(null);
  const [actAs, setActAs] = useState<string | null>(() => actAsUser());
  const [actAsUnsupported, setActAsUnsupported] = useState(false);
  const firebaseSignOut = useRef<(() => Promise<void>) | null>(null);

  /** Resolve WHO into WHAT — the backend's answer wins; the fallbacks say they are fallbacks. */
  const loadMe = useCallback(async (id: Identity) => {
    try {
      // With "View as" armed, first learn WHO is really here (no header), then who they
      // are looking as. Only an admin may act as another user; anyone else is unarmed.
      const act = actAsUser();
      let real: Me | null = null;
      if (act) {
        setActAsUser(null);
        try { real = await desk.me(); } finally { setActAsUser(act); }
        if (real.role !== 'admin') {
          setActAsUser(null); setActAs(null);
          setMe(real); setAdminMe(null); setActAsUnsupported(false); setMeError(null); setDegraded(false);
          setStatus('ready');
          return;
        }
      }
      const m = await desk.me();
      if (act) {
        // Honoured when the backend says so (`actingAs`, string or `{ by }`) or when the
        // identity it returned IS the requested user.
        const a = m.actingAs;
        const honoured = (typeof a === 'string' ? a.toLowerCase() === act.toLowerCase() : !!a)
          || (m.email ?? '').toLowerCase() === act.toLowerCase();
        setAdminMe(real);
        setActAsUnsupported(!honoured);
        setMe(honoured ? m : real);
      } else {
        setAdminMe(null); setActAsUnsupported(false);
        setMe(m);
      }
      setMeError(null); setMeStatus(null); setDegraded(false);
    } catch (e) {
      // Only a transport/route failure means "unavailable". Anything else (a 401/403, a
      // 500, or a fault in this code) must not be dressed up as a missing route.
      const st = e instanceof ApiError ? e.status : -1;
      setMeStatus(st);
      const unavailable = st === 404 || st === 0 || st === 501 || st >= 502;
      if (AUTH_MODE === 'sandbox' && unavailable) {
        // Dev before the identity route lands: the local list IS users.yml's mirror.
        const s = findSandboxUser(id.email);
        setMe(s ? { ...s } : placeholder(id));
        setMeError(`identity route unavailable — role taken from the sandbox list (${errorMessage(e)})`);
        setDegraded(true);
      } else if (unavailable) {
        setMe(placeholder(id));
        setMeError(`CrossDesk has not published its identity route yet — ${errorMessage(e)}`);
        setDegraded(true);
      } else {
        // 401/403: signed in with the provider, but CrossDesk has no mapping (or rejects the token).
        setMe(placeholder(id));
        setMeError(errorMessage(e));
        setDegraded(false);
      }
    }
    setStatus('ready');
  }, []);

  // ---- boot -------------------------------------------------------------------
  useEffect(() => {
    let cancelled = false;
    if (AUTH_MODE === 'sandbox') {
      const email = sandboxUser();
      if (!email) { setStatus('signedOut'); return; }
      const id: Identity = { email, displayName: findSandboxUser(email)?.displayName || email, uid: `sandbox:${email}` };
      setIdentity(id);
      void loadMe(id);
      return;
    }
    // Firebase mode — load the runtime only here, so the sandbox bundle never ships it.
    void (async () => {
      const [{ firebaseAuth }, { onAuthStateChanged, signOut: fbSignOut }] = await Promise.all([
        import('./firebase'), import('firebase/auth'),
      ]);
      if (cancelled) return;
      const auth = firebaseAuth();
      firebaseSignOut.current = () => fbSignOut(auth);
      onAuthStateChanged(auth, (user) => {
        if (cancelled) return;
        if (!user) {
          installTokenSource(null);
          setIdentity(null); setMe(null); setMeError(null); setDegraded(false);
          setStatus('signedOut');
          return;
        }
        installTokenSource(() => user.getIdToken());
        const id: Identity = { email: user.email || '', displayName: user.displayName || user.email || '', uid: user.uid };
        setIdentity(id);
        setStatus('loading');
        void loadMe(id);
      });
    })();
    return () => { cancelled = true; };
  }, [loadMe]);

  // ---- actions ----------------------------------------------------------------
  const signInEmail = useCallback(async (email: string, password: string) => {
    const [{ firebaseAuth }, { signInWithEmailAndPassword }] = await Promise.all([import('./firebase'), import('firebase/auth')]);
    await signInWithEmailAndPassword(firebaseAuth(), email, password);
  }, []);

  const signInGoogle = useCallback(async () => {
    const [{ firebaseAuth, googleProvider }, { signInWithPopup }] = await Promise.all([import('./firebase'), import('firebase/auth')]);
    await signInWithPopup(firebaseAuth(), googleProvider());
  }, []);

  const signInSandbox = useCallback(async (email: string) => {
    setSandboxUser(email);
    const id: Identity = { email, displayName: findSandboxUser(email)?.displayName || email, uid: `sandbox:${email}` };
    setIdentity(id);
    setStatus('loading');
    await loadMe(id);
  }, [loadMe]);

  const signOut = useCallback(async () => {
    if (AUTH_MODE === 'sandbox') {
      setSandboxUser(null);
    } else if (firebaseSignOut.current) {
      await firebaseSignOut.current();
    }
    installTokenSource(null);
    setActAsUser(null); setActAs(null); setAdminMe(null); setActAsUnsupported(false);
    setIdentity(null); setMe(null); setMeError(null); setMeStatus(null); setDegraded(false);
    setStatus('signedOut');
  }, []);

  const refreshMe = useCallback(async () => {
    if (identity) await loadMe(identity);
  }, [identity, loadMe]);

  const startActAs = useCallback(async (email: string) => {
    if (!identity) return;
    setActAsUser(email); setActAs(email);
    setStatus('loading');
    await loadMe(identity);
  }, [identity, loadMe]);

  const stopActAs = useCallback(async () => {
    setActAsUser(null); setActAs(null); setActAsUnsupported(false); setAdminMe(null);
    if (identity) { setStatus('loading'); await loadMe(identity); }
  }, [identity, loadMe]);

  const value = useMemo<AuthState>(() => ({
    mode: AUTH_MODE, mock: MOCK, status, me, meError, meStatus, degraded,
    adminMe, actAs, actAsUnsupported, startActAs, stopActAs,
    signInEmail, signInGoogle, signInSandbox, signOut, refreshMe,
  }), [status, me, meError, meStatus, degraded, adminMe, actAs, actAsUnsupported, startActAs, stopActAs,
    signInEmail, signInGoogle, signInSandbox, signOut, refreshMe]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthState {
  const v = useContext(Ctx);
  if (!v) throw new Error('useAuth outside AuthProvider');
  return v;
}
