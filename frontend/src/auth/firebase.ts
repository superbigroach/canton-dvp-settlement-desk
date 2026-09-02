// Firebase — AUTH ONLY. No Firestore, no analytics; the ledger is the database.
// The web config is public by design (it identifies the project, it does not authorise
// anything) and is the one printed by `firebase apps:sdkconfig web --project crossdesk-devnet-app`.
import { initializeApp, type FirebaseApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  browserLocalPersistence,
  setPersistence,
  type Auth,
} from 'firebase/auth';

const env = import.meta.env;

export const firebaseConfig = {
  apiKey: env.VITE_FIREBASE_API_KEY || 'AIzaSyDDG4S9RHg_nW6KkMOxDftZmkE5e0yjM4Y',
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN || 'crossdesk-devnet-app.firebaseapp.com',
  projectId: env.VITE_FIREBASE_PROJECT_ID || 'crossdesk-devnet-app',
  appId: env.VITE_FIREBASE_APP_ID || '1:367745852528:web:86cbd82cfd1f744c2700b5',
};

let app: FirebaseApp | null = null;
let auth: Auth | null = null;

/** Lazily initialised so the sandbox build never loads the Firebase runtime at all. */
export function firebaseAuth(): Auth {
  if (!auth) {
    app = initializeApp(firebaseConfig);
    auth = getAuth(app);
    void setPersistence(auth, browserLocalPersistence).catch(() => undefined);
  }
  return auth;
}

export const googleProvider = () => {
  const p = new GoogleAuthProvider();
  p.setCustomParameters({ prompt: 'select_account' });
  return p;
};

/** Firebase's error codes, turned into sentences a signer can act on. */
export function firebaseErrorMessage(e: unknown): string {
  const code = (e as { code?: string })?.code ?? '';
  switch (code) {
    case 'auth/operation-not-allowed':
      return 'That sign-in method is not enabled on this project yet. Ask CrossDesk to enable it in the Firebase console (Authentication → Sign-in method).';
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
    case 'auth/user-not-found':
      return 'Email or password not recognised.';
    case 'auth/invalid-email':
      return 'That is not a valid email address.';
    case 'auth/too-many-requests':
      return 'Too many attempts — wait a minute and try again.';
    case 'auth/popup-closed-by-user':
    case 'auth/cancelled-popup-request':
      return 'Sign-in window closed before finishing.';
    case 'auth/popup-blocked':
      return 'The browser blocked the sign-in window. Allow pop-ups for this site and try again.';
    case 'auth/unauthorized-domain':
      return `This domain (${window.location.hostname}) is not authorised for sign-in on the Firebase project.`;
    case 'auth/network-request-failed':
      return 'No network — could not reach the identity service.';
    default: {
      const m = (e as { message?: string })?.message;
      return m ? m.replace(/^Firebase:\s*/, '').replace(/\s*\(auth\/[^)]+\)\.?$/, '') : 'sign-in failed';
    }
  }
}
