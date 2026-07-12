import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The desk's Spring Boot backend runs on :8080. Both the dev server and the
// production `preview` server proxy /api → :8080 so the browser only ever talks
// to one origin (no CORS, no hardcoded host in the client).
const proxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy },
  preview: { port: 5173, proxy },
});
