/**
 * The decision loop for one proposal: resolve sources -> evaluate every condition of the
 * seat -> confirm with evidence, or refuse naming the failed condition -> record.
 *
 * Three outcomes, deliberately distinct:
 *   confirm  every condition passed; evidence attached; recorded (never repeated)
 *   refuse   a condition FAILED on real numbers; the refusal names it; recorded
 *   halt     a condition could not be evaluated (source down, field missing, unconfigured);
 *            nothing is sent; NOT recorded, so the next poll retries after the operator fixes it
 */
import type { Config, Seat } from './config';
import type { CrossDeskClient, Proposal, ProtocolRole } from './client';
import { ApiError } from './client';
import { MissingEvidence, ruleFor, satisfiedAlternative, tidy, type RuleResult } from './evaluate';
import { SourceResolver, type Fetcher, type Runner } from './sources';
import type { State } from './state';
import * as log from './log';

export interface ConditionOutcome {
  pass: boolean;
  values: Record<string, unknown>;
  evidence: Record<string, unknown>;
  reason: string;
}

export interface Decision {
  decision: 'confirm' | 'refuse' | 'halt' | 'skip';
  key: string;
  cid: string;
  instrument: string;
  price: number;
  conditions: Record<string, ConditionOutcome | { error: string }>;
  checks: string[];
  evidence: Record<string, unknown>;
  failed?: { condition: string; reason: string };
  halt?: string;
  http?: { status: number; body?: unknown };
}

export interface HandlerDeps {
  config: Config;
  client: CrossDeskClient;
  state: State;
  role: ProtocolRole | undefined;
  fetcher?: Fetcher;
  runner?: Runner;
  now?: () => Date;
}

export function proposalKey(p: Proposal): string {
  return p.rootCid || p.cid;
}

function priceOf(p: Proposal): number {
  const n = typeof p.price === 'number' ? p.price : Number(p.price);
  if (!Number.isFinite(n)) throw new Error(`proposal ${p.cid} has no numeric price: ${JSON.stringify(p.price)}`);
  return n;
}

/** Evidence fields the backend's protocol declares for a condition, if it declares any. */
function declaredEvidenceFields(role: ProtocolRole | undefined, condition: string): string[] {
  const c = role?.conditions.find((x) => x.name === condition);
  const ev = c?.evidence;
  if (!ev) return [];
  const names = (list: unknown[]): string[] =>
    list.map((e) => (typeof e === 'string' ? e : String((e as { name?: unknown })?.name ?? ''))).filter(Boolean);
  if (Array.isArray(ev)) return names(ev);
  if (typeof ev === 'object') {
    const o = ev as Record<string, unknown>;
    if (o.required === false) return [];
    if (Array.isArray(o.fields)) return names(o.fields as unknown[]);
    if (Array.isArray(o.required)) return (o.required as unknown[]).map(String);
    if (o.properties && typeof o.properties === 'object') return Object.keys(o.properties as object);
    return Object.keys(o).filter((k) => !['type', 'description', 'title'].includes(k));
  }
  return [];
}

/** Evaluate every condition without sending anything. Pure apart from the sources. */
export async function evaluateProposal(deps: HandlerDeps, p: Proposal): Promise<Decision> {
  const { config } = deps;
  const seat: Seat = config.seat;
  const price = priceOf(p);
  const key = proposalKey(p);
  const now = deps.now ? deps.now() : new Date();
  const conditions = (p.conditions && p.conditions.length > 0)
    ? p.conditions
    : (deps.role?.conditions.map((c) => c.name) ?? Object.keys(config.conditions));

  const resolver = new SourceResolver({ instrument: p.instrument, price, cid: p.cid, seat }, deps.fetcher, deps.runner);
  const out: Decision = { decision: 'halt', key, cid: p.cid, instrument: p.instrument, price, conditions: {}, checks: [], evidence: {} };

  for (const name of conditions) {
    const spec = ruleFor(seat, name);
    if (!spec) {
      out.conditions[name] = { error: `no rule for condition '${name}' on the ${seat} seat` };
      out.halt = out.halt ?? `unknown condition '${name}'`;
      continue;
    }
    const sources = config.conditions[name];
    if (!sources) {
      out.conditions[name] = { error: `no data source configured for '${name}'` };
      out.halt = out.halt ?? `no data source configured for '${name}'`;
      continue;
    }
    const configured = Object.keys(sources);
    const alt = satisfiedAlternative(spec, configured);
    if (!alt) {
      const need = spec.needs.map((a) => a.join('+')).join(' or ');
      out.conditions[name] = { error: `'${name}' needs ${need}; configured: ${configured.join(', ') || 'nothing'}` };
      out.halt = out.halt ?? `'${name}' is missing evidence fields (${need})`;
      continue;
    }
    // Resolve every configured field (the rule reads what it needs; extras become evidence too).
    const fields: Record<string, unknown> = {};
    try {
      for (const [fname, src] of Object.entries(sources)) {
        fields[fname] = await resolver.resolve(src);
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      out.conditions[name] = { error: `source failed: ${msg}` };
      out.halt = out.halt ?? `'${name}': source failed: ${msg}`;
      continue;
    }
    let r: RuleResult;
    try {
      r = spec.rule(fields, { seat, instrument: p.instrument, price, cid: p.cid, now, tolerances: config.tolerances });
    } catch (e) {
      const msg = e instanceof MissingEvidence ? e.message : (e instanceof Error ? e.message : String(e));
      out.conditions[name] = { error: msg };
      out.halt = out.halt ?? msg;
      continue;
    }
    // The backend may declare evidence fields we must carry; if a declared field is neither
    // produced by the rule nor configured as a source, that is a halt - never confirm without it.
    const declared = declaredEvidenceFields(deps.role, name);
    const missing = declared.filter((f) => !(f in r.evidence) && !(f in fields));
    if (r.pass && missing.length > 0) {
      out.conditions[name] = { error: `protocol declares evidence ${missing.join(', ')} for '${name}' and no source provides it` };
      out.halt = out.halt ?? `'${name}': missing declared evidence ${missing.join(', ')}`;
      continue;
    }
    for (const f of declared) if (!(f in r.evidence) && f in fields) r.evidence[f] = fields[f];
    r.evidence = tidy(r.evidence);
    r.values = tidy(r.values);
    out.conditions[name] = { pass: r.pass, values: r.values, evidence: r.evidence, reason: r.reason };
    if (r.pass) {
      out.checks.push(name);
      // The backend contract: evidence is keyed by condition name, { "<condition>": { field: value } }.
      // The venue's traded range is ALSO hoisted to the top level - that is the {low, high} the
      // ledger enforces, and the shape the venue path reads.
      out.evidence[name] = r.evidence;
      if (name === 'traded-range') {
        out.evidence.low = r.evidence.low;
        out.evidence.high = r.evidence.high;
      }
    } else if (!out.failed) {
      out.failed = { condition: name, reason: r.reason };
    }
  }

  if (out.failed) {
    out.decision = 'refuse';
    // The reason names every failed condition, first one leads.
    const others = Object.entries(out.conditions)
      .filter(([n, c]) => 'pass' in c && !c.pass && n !== out.failed!.condition)
      .map(([n, c]) => `${n}: ${(c as ConditionOutcome).reason}`);
    if (others.length) out.failed.reason += `; also failed ${others.join('; ')}`;
  } else if (out.halt) {
    out.decision = 'halt';
  } else if (out.checks.length === conditions.length && out.checks.length > 0) {
    out.decision = 'confirm';
  } else {
    out.decision = 'halt';
    out.halt = 'no conditions evaluated';
  }
  return out;
}

const inFlight = new Set<string>();

/** Evaluate, act, record. Safe to call from the poller and the webhook for the same proposal. */
export async function handleProposal(deps: HandlerDeps, p: Proposal): Promise<Decision | null> {
  const { config, client, state } = deps;
  const key = proposalKey(p);
  const base = { proposalCid: p.cid, rootCid: p.rootCid, instrument: p.instrument, seat: config.seat, price: p.price };

  if (!config.instruments.some((i) => i.toLowerCase() === String(p.instrument).toLowerCase())) {
    log.debug('proposal.skip', { ...base, why: 'instrument not configured' });
    return null;
  }
  const prior = state.get(key);
  if (prior) {
    log.debug('proposal.skip', { ...base, why: `already acted: ${prior.decision} at ${prior.at}` });
    return null;
  }
  const mineAction = p.mine?.action;
  if (mineAction === 'confirmed' || mineAction === 'refused') {
    state.record(key, { cid: p.cid, instrument: p.instrument, decision: `already-${mineAction}`, at: new Date().toISOString(), detail: 'found on CrossDesk before this service acted' });
    log.info('proposal.skip', { ...base, why: `CrossDesk already shows mine.action=${mineAction}` });
    return null;
  }
  if (p.my && p.my.canConfirm === false) {
    log.warn('proposal.skip', { ...base, why: 'CrossDesk says this seat cannot confirm (not a member of the committee, or already signed)' });
    return null;
  }
  if (inFlight.has(key)) {
    log.debug('proposal.skip', { ...base, why: 'evaluation already in flight' });
    return null;
  }
  inFlight.add(key);
  try {
    const d = await evaluateProposal(deps, p);
    if (d.decision === 'halt') {
      log.warn('decision', { ...base, decision: 'halt', halt: d.halt, conditions: d.conditions, note: 'nothing sent; will retry next poll' });
      return d;
    }
    if (d.decision === 'confirm') {
      const r = await client.confirm(p.cid, d.checks, d.evidence);
      d.http = { status: r.status, body: r.body };
      const ok = r.ok;
      if (ok || (r.status >= 400 && r.status < 500)) {
        state.record(key, { cid: p.cid, instrument: p.instrument, decision: ok ? 'confirm' : 'rejected', at: new Date().toISOString(), httpStatus: r.status, detail: ok ? undefined : summarize(r.body) });
      }
      log[ok ? 'info' : 'error']('decision', { ...base, decision: 'confirm', checks: d.checks, evidence: d.evidence, conditions: d.conditions, http: { status: r.status, body: ok ? r.body : summarize(r.body) } });
      return d;
    }
    // refuse
    const f = d.failed!;
    const r = await client.refuse(p.cid, f.condition, f.reason);
    d.http = { status: r.status, body: r.body };
    if (r.ok || (r.status >= 400 && r.status < 500)) {
      state.record(key, { cid: p.cid, instrument: p.instrument, decision: r.ok ? 'refuse' : 'rejected', at: new Date().toISOString(), httpStatus: r.status, detail: r.ok ? `${f.condition}: ${f.reason}` : summarize(r.body) });
    }
    log[r.ok ? 'info' : 'error']('decision', { ...base, decision: 'refuse', condition: f.condition, reason: f.reason, conditions: d.conditions, http: { status: r.status, body: r.ok ? r.body : summarize(r.body) } });
    return d;
  } catch (e) {
    const msg = e instanceof ApiError ? `${e.message}` : (e instanceof Error ? e.message : String(e));
    log.error('decision', { ...base, decision: 'error', error: msg, note: 'nothing recorded; will retry next poll' });
    return null;
  } finally {
    inFlight.delete(key);
  }
}

function summarize(body: unknown): string {
  if (body && typeof body === 'object') {
    const o = body as Record<string, unknown>;
    const m = o.message ?? o.error ?? o.detail;
    if (m) return String(m).slice(0, 300);
    return JSON.stringify(body).slice(0, 300);
  }
  return String(body ?? '').slice(0, 300);
}
