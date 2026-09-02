/** One JSON object per line on stdout. Nothing else is ever printed there. */
export type Level = 'debug' | 'info' | 'warn' | 'error';

const ORDER: Record<Level, number> = { debug: 10, info: 20, warn: 30, error: 40 };
let threshold: Level = (process.env.LOG_LEVEL as Level) || 'info';

export function setLevel(level: Level): void {
  threshold = level;
}

export function log(level: Level, event: string, fields: Record<string, unknown> = {}): void {
  if (ORDER[level] < ORDER[threshold]) return;
  const line = { ts: new Date().toISOString(), level, event, ...fields };
  process.stdout.write(JSON.stringify(line) + '\n');
}

export const info = (event: string, f?: Record<string, unknown>) => log('info', event, f);
export const warn = (event: string, f?: Record<string, unknown>) => log('warn', event, f);
export const error = (event: string, f?: Record<string, unknown>) => log('error', event, f);
export const debug = (event: string, f?: Record<string, unknown>) => log('debug', event, f);
