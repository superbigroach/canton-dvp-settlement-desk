import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The desk's Spring Boot backend runs on :8080. Both the dev server and the
// production `preview` server proxy /api → :8080 so the browser only ever talks
// to one origin (no CORS, no hardcoded host in the client).
const proxy = {
  '/api': {
    // The backend runs inside WSL when the ledger does (WSL's network boundary breaks
    // the larger gRPC streams if the backend sits on the Windows side). Point this at the
    // WSL IP via VITE_API_TARGET; defaults to localhost for an all-Windows run.
    // (`@types/node` is not a dependency of this app — Vite runs its own config
    // through esbuild in Node, so the global exists at run time. Declared locally
    // rather than pulling in the whole Node type surface for one env read.)
    target: (globalThis as { process?: { env?: Record<string, string | undefined> } })
      .process?.env?.VITE_API_TARGET || 'http://localhost:8080',
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  preview: { port: 5173, proxy },
});
