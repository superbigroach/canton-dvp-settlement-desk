// Firebase Hosting serves a static /index.html for "/" BEFORE it consults any rewrite,
// so a `** -> /landing.html` rule can never win the root. The only way to put the
// marketing page at "/" is to make it the actual index.html — which means relocating
// the SPA's entry point to /desk/index.html.
//
// Vite emits absolute asset URLs (/assets/...), so moving the entry HTML does not
// break it. Run automatically after every build; safe to run twice.
import { mkdirSync, renameSync, existsSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const dist = 'dist';
const spa = join(dist, 'index.html');
const landing = join(dist, 'landing.html');
const deskDir = join(dist, 'desk');

if (!existsSync(landing)) {
  console.error('arrange-dist: public/landing.html missing — nothing to arrange');
  process.exit(1);
}
mkdirSync(deskDir, { recursive: true });
const deskIndex = join(deskDir, 'index.html');
if (existsSync(deskIndex)) rmSync(deskIndex);
renameSync(spa, deskIndex);        // SPA  -> /desk/index.html
renameSync(landing, spa);          // page -> /index.html
console.log('arrange-dist: landing -> /, app -> /desk/');
