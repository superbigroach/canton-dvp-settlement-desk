import fs from 'node:fs';
import path from 'node:path';
import YAML from 'yaml';

export type Seat = 'issuer' | 'lender' | 'venue';
export const SEATS: Seat[] = ['issuer', 'lender', 'venue'];

export interface SourceSpec {
  kind: 'static' | 'http' | 'command';
  /** static */
  value?: unknown;
  /** http */
  url?: string;
  method?: 'GET' | 'POST';
  bearer?: string;
  headers?: Record<string, string>;
  /** command */
  command?: string;
  /** http + command: how to read stdout / the body. Default json. */
  parse?: 'json' | 'text';
  /** http + command: JSON pointer into the parsed document. */
  pointer?: string;
  timeoutMs?: number;
}

/** condition name -> evidence field -> where it comes from. */
export type ConditionSources = Record<string, Record<string, SourceSpec>>;

export interface Config {
  crossdesk: {
    baseUrl: string;
    apiKey?: string;
    sandboxUser?: string;
    webhookSecret?: string;
    poll: { enabled: boolean; intervalSeconds: number };
    timeoutMs: number;
  };
  server: { port: number; host: string; webhookPath: string };
  state: { file: string };
  seat: Seat;
  instruments: string[];
  tolerances: Record<string, number>;
  conditions: ConditionSources;
}

export const DEFAULT_TOLERANCES: Record<string, number> = {
  // issuer
  reservesMaxAgeHours: 24,
  maxQueueDepth: 0,
  // lender
  markToleranceBps: 25,
  liquidationToleranceBps: 100,
  bookAcceptanceMaxAgeMinutes: 60,
  // venue
  maxSpreadBps: 50,
  minVolume: 0,
};

/**
 * ${VAR} and ${VAR:-default} in any string value are replaced from the environment.
 * An unset variable without a default becomes '' - validation then decides if that matters.
 */
export function substituteEnv(value: unknown, env: NodeJS.ProcessEnv = process.env): unknown {
  if (typeof value === 'string') {
    return value.replace(/\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\}/g, (_m, name: string, dflt?: string) => {
      const v = env[name];
      if (v !== undefined && v !== '') return v;
      return dflt ?? '';
    });
  }
  if (Array.isArray(value)) return value.map((v) => substituteEnv(v, env));
  if (value !== null && typeof value === 'object') {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) out[k] = substituteEnv(v, env);
    return out;
  }
  return value;
}

function str(v: unknown): string | undefined {
  if (v === undefined || v === null) return undefined;
  const s = String(v).trim();
  return s === '' ? undefined : s;
}

function num(v: unknown, fallback: number): number {
  if (v === undefined || v === null || v === '') return fallback;
  const n = Number(v);
  if (!Number.isFinite(n)) throw new Error(`expected a number, got '${String(v)}'`);
  return n;
}

function bool(v: unknown, fallback: boolean): boolean {
  if (v === undefined || v === null || v === '') return fallback;
  if (typeof v === 'boolean') return v;
  return !['false', '0', 'no', 'off'].includes(String(v).toLowerCase());
}

export function parseConfig(raw: unknown, env: NodeJS.ProcessEnv = process.env): Config {
  const doc = substituteEnv(raw, env) as Record<string, any>;
  if (!doc || typeof doc !== 'object') throw new Error('signer.yml is empty');

  const cd = doc.crossdesk ?? {};
  const baseUrl = (str(cd.baseUrl) ?? str(env.CROSSDESK_BASE_URL) ?? '').replace(/\/+$/, '');
  if (!baseUrl) throw new Error('crossdesk.baseUrl is required');
  const apiKey = str(cd.apiKey) ?? str(env.CROSSDESK_API_KEY);
  const sandboxUser = str(cd.sandboxUser) ?? str(env.CROSSDESK_SANDBOX_USER);
  if (!apiKey && !sandboxUser) {
    throw new Error(
      'one of crossdesk.apiKey (CROSSDESK_API_KEY) or crossdesk.sandboxUser (CROSSDESK_SANDBOX_USER) is required',
    );
  }
  const webhookSecret = str(cd.webhookSecret) ?? str(env.CROSSDESK_WEBHOOK_SECRET);
  const poll = cd.poll ?? {};

  const seat = str(doc.seat) as Seat | undefined;
  if (!seat || !SEATS.includes(seat)) throw new Error(`seat must be one of ${SEATS.join(' | ')}`);

  const instruments: string[] = Array.isArray(doc.instruments)
    ? doc.instruments.map((i: unknown) => String(i).trim()).filter(Boolean)
    : [];
  if (instruments.length === 0) throw new Error('instruments must list at least one instrument id');

  const conditions: ConditionSources = {};
  const rawConds = doc.conditions ?? {};
  if (typeof rawConds !== 'object') throw new Error('conditions must be a map');
  for (const [cname, fields] of Object.entries(rawConds as Record<string, unknown>)) {
    if (!fields || typeof fields !== 'object') {
      throw new Error(`conditions.${cname} must map evidence fields to sources`);
    }
    const out: Record<string, SourceSpec> = {};
    for (const [fname, spec] of Object.entries(fields as Record<string, unknown>)) {
      out[fname] = parseSource(`conditions.${cname}.${fname}`, spec);
    }
    conditions[cname] = out;
  }

  const tolerances: Record<string, number> = { ...DEFAULT_TOLERANCES };
  for (const [k, v] of Object.entries((doc.tolerances ?? {}) as Record<string, unknown>)) {
    tolerances[k] = num(v, NaN);
    if (!Number.isFinite(tolerances[k])) throw new Error(`tolerances.${k} must be a number`);
  }

  const server = doc.server ?? {};
  const state = doc.state ?? {};
  return {
    crossdesk: {
      baseUrl,
      apiKey,
      sandboxUser,
      webhookSecret,
      poll: {
        enabled: bool(poll.enabled, true),
        intervalSeconds: num(poll.intervalSeconds, 30),
      },
      timeoutMs: num(cd.timeoutMs, 15000),
    },
    server: {
      port: num(server.port ?? env.PORT, 8787),
      host: str(server.host) ?? '0.0.0.0',
      webhookPath: str(server.webhookPath) ?? '/webhook',
    },
    state: { file: str(state.file) ?? str(env.SIGNER_STATE) ?? './signer-state.json' },
    seat,
    instruments,
    tolerances,
    conditions,
  };
}

function parseSource(where: string, spec: unknown): SourceSpec {
  // A bare scalar (or list) is shorthand for a static value.
  if (spec === null || typeof spec !== 'object' || Array.isArray(spec)) {
    return { kind: 'static', value: spec };
  }
  const s = spec as Record<string, unknown>;
  const kind = str(s.kind);
  if (kind === 'static' || (kind === undefined && 'value' in s)) {
    return { kind: 'static', value: s.value };
  }
  if (kind === 'http') {
    const url = str(s.url);
    if (!url) throw new Error(`${where}: http source needs url`);
    const headers: Record<string, string> = {};
    for (const [k, v] of Object.entries((s.headers ?? {}) as Record<string, unknown>)) headers[k] = String(v);
    return {
      kind: 'http',
      url,
      method: str(s.method)?.toUpperCase() === 'POST' ? 'POST' : 'GET',
      bearer: str(s.bearer),
      headers,
      parse: str(s.parse) === 'text' ? 'text' : 'json',
      pointer: str(s.pointer),
      timeoutMs: num(s.timeoutMs, 10000),
    };
  }
  if (kind === 'command') {
    const command = str(s.command);
    if (!command) throw new Error(`${where}: command source needs command`);
    return {
      kind: 'command',
      command,
      parse: str(s.parse) === 'text' ? 'text' : 'json',
      pointer: str(s.pointer),
      timeoutMs: num(s.timeoutMs, 10000),
    };
  }
  throw new Error(`${where}: source kind must be static | http | command (got '${kind ?? 'none'}')`);
}

export function loadConfig(file: string, env: NodeJS.ProcessEnv = process.env): Config {
  const abs = path.resolve(file);
  if (!fs.existsSync(abs)) throw new Error(`config file not found: ${abs}`);
  const raw = YAML.parse(fs.readFileSync(abs, 'utf8'));
  return parseConfig(raw, env);
}
