import { exec } from 'node:child_process';
import { resolvePointer } from './jsonpointer';
import type { SourceSpec } from './config';

/** What a source may reference in its url / command / headers. */
export interface SourceContext {
  instrument: string;
  price: number;
  cid: string;
  seat: string;
}

const SAFE_CID = /^[A-Za-z0-9:_.-]+$/;
const SAFE_INSTRUMENT = /^[A-Za-z0-9_.-]+$/;

/** {instrument}, {price}, {cid}, {seat} - the only values interpolated, each validated first. */
export function interpolate(template: string, ctx: SourceContext): string {
  if (!SAFE_INSTRUMENT.test(ctx.instrument)) throw new Error(`refusing to interpolate instrument '${ctx.instrument}'`);
  if (!SAFE_CID.test(ctx.cid)) throw new Error(`refusing to interpolate cid '${ctx.cid}'`);
  if (!Number.isFinite(ctx.price)) throw new Error('refusing to interpolate a non-numeric price');
  return template
    .replace(/\{instrument\}/g, ctx.instrument)
    .replace(/\{price\}/g, String(ctx.price))
    .replace(/\{cid\}/g, ctx.cid)
    .replace(/\{seat\}/g, ctx.seat);
}

export interface Fetcher {
  (
    url: string,
    init: { method: string; headers: Record<string, string>; signal: AbortSignal },
  ): Promise<{ ok: boolean; status: number; text(): Promise<string> }>;
}

export interface Runner {
  (command: string, timeoutMs: number): Promise<string>;
}

export const defaultFetcher: Fetcher = (url, init) => fetch(url, init);

export const defaultRunner: Runner = (command, timeoutMs) =>
  new Promise((resolve, reject) => {
    exec(command, { timeout: timeoutMs, maxBuffer: 1024 * 1024, windowsHide: true }, (err, stdout, stderr) => {
      if (err) {
        const why = String(stderr || err.message).trim();
        reject(new Error(`command failed (${err.code ?? 'signal ' + err.signal}): ${why}`));
        return;
      }
      resolve(String(stdout));
    });
  });

function parseBody(text: string, parse: 'json' | 'text' | undefined): unknown {
  if (parse === 'text') {
    const t = text.trim();
    const n = Number(t);
    return t !== '' && Number.isFinite(n) ? n : t;
  }
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`stdout/body is not JSON: ${text.slice(0, 120).replace(/\s+/g, ' ')}`);
  }
}

/**
 * Resolves every source once per evaluation: two fields pointing at the same URL or
 * command share one fetch, so a venue can read low and high from one book snapshot.
 */
export class SourceResolver {
  private cache = new Map<string, Promise<unknown>>();

  constructor(
    private readonly ctx: SourceContext,
    private readonly fetcher: Fetcher = defaultFetcher,
    private readonly runner: Runner = defaultRunner,
  ) {}

  async resolve(spec: SourceSpec): Promise<unknown> {
    switch (spec.kind) {
      case 'static':
        return spec.value;
      case 'http': {
        const doc = await this.memo('http', spec, () => this.http(spec));
        return resolvePointer(doc, spec.pointer);
      }
      case 'command': {
        const doc = await this.memo('command', spec, () => this.command(spec));
        return resolvePointer(doc, spec.pointer);
      }
    }
  }

  private memo(kind: string, spec: SourceSpec, run: () => Promise<unknown>): Promise<unknown> {
    const key = JSON.stringify([kind, spec.url, spec.method, spec.headers, spec.bearer, spec.command, spec.parse]);
    let p = this.cache.get(key);
    if (!p) {
      p = run();
      this.cache.set(key, p);
    }
    return p;
  }

  private async http(spec: SourceSpec): Promise<unknown> {
    const url = interpolate(spec.url!, this.ctx);
    const headers: Record<string, string> = { accept: 'application/json' };
    for (const [k, v] of Object.entries(spec.headers ?? {})) headers[k] = interpolate(v, this.ctx);
    if (spec.bearer) headers.authorization = `Bearer ${spec.bearer}`;
    const res = await this.fetcher(url, {
      method: spec.method ?? 'GET',
      headers,
      signal: AbortSignal.timeout(spec.timeoutMs ?? 10000),
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`${spec.method ?? 'GET'} ${redact(url)} -> HTTP ${res.status}: ${text.slice(0, 160)}`);
    return parseBody(text, spec.parse);
  }

  private async command(spec: SourceSpec): Promise<unknown> {
    const cmd = interpolate(spec.command!, this.ctx);
    const out = await this.runner(cmd, spec.timeoutMs ?? 10000);
    return parseBody(out, spec.parse);
  }
}

function redact(url: string): string {
  try {
    const u = new URL(url);
    for (const k of Array.from(u.searchParams.keys())) {
      if (/key|token|secret|sig/i.test(k)) u.searchParams.set(k, '***');
    }
    return u.toString();
  } catch {
    return url;
  }
}
