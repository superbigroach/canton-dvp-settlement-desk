/**
 * One JSON object per line on stdout. Nothing else is ever printed there.
 *
 * Every line passes through `redact` before it is written: configured secrets (the API
 * key, the webhook secret, every source bearer / header value) are replaced by `***`, and
 * anything shaped like `token=...`, `Authorization: Bearer ...` or `scheme://user:pass@`
 * is masked whether or not it was registered. A source's stderr or an error text can
 * echo a command line; the command line can carry an inline credential; this is the last
 * line of defence for it (audit 2026-09-22, medium #15).
 */
export type Level = 'debug' | 'info' | 'warn' | 'error';

const ORDER: Record<Level, number> = { debug: 10, info: 20, warn: 30, error: 40 };
let threshold: Level = (process.env.LOG_LEVEL as Level) || 'info';

const secrets: string[] = [];

export function setLevel(level: Level): void {
  threshold = level;
}

/** Values that must never reach a log line. Short values are ignored (masking "1" would destroy every line). */
export function registerSecrets(values: string[]): void {
  for (const v of values) {
    if (typeof v === 'string' && v.length >= 8 && !secrets.includes(v)) secrets.push(v);
  }
}

/** Test hook. */
export function clearSecrets(): void {
  secrets.length = 0;
}

const MASK = '***';
// `key=value`, `key: value`, `"key":"value"` for the usual credential names. The value
// class excludes quotes so a JSON line stays valid JSON after masking.
const KV = /((?:bearer|token|secret|password|passwd|pwd|api[-_]?key|access[-_]?key|private[-_]?key|authorization|x-api-key)["']?\s*[:=]\s*["']?(?:bearer\s+)?)([^\s"'&,;}\]]+)/gi;
// scheme://user:password@host
const USERINFO = /(\b[a-z][a-z0-9+.-]*:\/\/)([^\s/@"']+):([^\s/@"']+)@/gi;

export function redact(text: string): string {
  let out = text;
  for (const s of secrets) {
    out = out.split(s).join(MASK);
    // The same secret as it appears inside a JSON string literal.
    const escaped = JSON.stringify(s).slice(1, -1);
    if (escaped !== s) out = out.split(escaped).join(MASK);
  }
  out = out.replace(KV, (m, head: string, value: string) => (value === MASK ? m : head + MASK));
  out = out.replace(USERINFO, (_m, scheme: string, user: string) => `${scheme}${user}:${MASK}@`);
  return out;
}

export function log(level: Level, event: string, fields: Record<string, unknown> = {}): void {
  if (ORDER[level] < ORDER[threshold]) return;
  const line = { ts: new Date().toISOString(), level, event, ...fields };
  process.stdout.write(redact(JSON.stringify(line)) + '\n');
}

export const info = (event: string, f?: Record<string, unknown>) => log('info', event, f);
export const warn = (event: string, f?: Record<string, unknown>) => log('warn', event, f);
export const error = (event: string, f?: Record<string, unknown>) => log('error', event, f);
export const debug = (event: string, f?: Record<string, unknown>) => log('debug', event, f);
