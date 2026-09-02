// The ONE place the browser decides how it identifies itself to /api/**.
//
// Two modes, chosen at build time by VITE_AUTH_MODE:
//   firebase (default) — the signed-in Firebase user's ID token, as `Authorization: Bearer`.
//   sandbox            — `X-Sandbox-User: <email>`, which the backend honours only when it
//                        itself runs with AUTH_MODE=sandbox (docs/PRODUCT-PLAN.md §3).
//
// This module has no React in it on purpose: `api.ts` (the operator desk's client) and
// `desk/client.ts` (the product client) both import it, and neither should know which
// identity provider is behind the header.

export type AuthMode = 'firebase' | 'sandbox';

const MOCK = import.meta.env.VITE_API_MOCK === '1';

export const AUTH_MODE: AuthMode =
  import.meta.env.VITE_AUTH_MODE === 'sandbox' || (MOCK && !import.meta.env.VITE_AUTH_MODE)
    ? 'sandbox'
    : 'firebase';

const SANDBOX_KEY = 'crossdesk.sandboxUser';

export function sandboxUser(): string | null {
  try {
    return window.localStorage.getItem(SANDBOX_KEY);
  } catch {
    return null;
  }
}

export function setSandboxUser(email: string | null): void {
  try {
    if (email) window.localStorage.setItem(SANDBOX_KEY, email);
    else window.localStorage.removeItem(SANDBOX_KEY);
  } catch {
    /* private mode — the session simply does not persist */
  }
}

// ---- Admin "View as" ---------------------------------------------------------
// An admin can look at (and act on) the desk as another mapped user. The choice is
// per-tab (sessionStorage) and travels as `X-Act-As: <email>`; the backend decides
// whether to honour it and echoes `actingAs` on /api/me when it does.
const ACT_AS_KEY = 'crossdesk.actAs';

export function actAsUser(): string | null {
  try {
    return window.sessionStorage.getItem(ACT_AS_KEY);
  } catch {
    return null;
  }
}

export function setActAsUser(email: string | null): void {
  try {
    if (email) window.sessionStorage.setItem(ACT_AS_KEY, email);
    else window.sessionStorage.removeItem(ACT_AS_KEY);
  } catch {
    /* no sessionStorage — the switch simply does not survive a reload */
  }
}

/** Installed by the AuthProvider once Firebase is up; null until then. */
let tokenSource: (() => Promise<string | null>) | null = null;

export function installTokenSource(fn: (() => Promise<string | null>) | null): void {
  tokenSource = fn;
}

/** Headers that identify the caller, or none when nobody is signed in. */
export async function authHeaders(): Promise<Record<string, string>> {
  const h = await identityHeaders();
  const act = actAsUser();
  if (act && Object.keys(h).length) h['X-Act-As'] = act;
  return h;
}

async function identityHeaders(): Promise<Record<string, string>> {
  if (AUTH_MODE === 'sandbox') {
    const u = sandboxUser();
    return u ? { 'X-Sandbox-User': u } : {};
  }
  if (!tokenSource) return {};
  try {
    const t = await tokenSource();
    return t ? { Authorization: `Bearer ${t}` } : {};
  } catch {
    return {};
  }
}
