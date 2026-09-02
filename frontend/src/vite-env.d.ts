/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_MOCK?: string;
  readonly VITE_AUTH_MODE?: string;
  readonly VITE_FIREBASE_API_KEY?: string;
  readonly VITE_FIREBASE_AUTH_DOMAIN?: string;
  readonly VITE_FIREBASE_PROJECT_ID?: string;
  readonly VITE_FIREBASE_APP_ID?: string;
}
