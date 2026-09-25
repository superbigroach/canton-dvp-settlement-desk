#!/usr/bin/env node
/**
 * ETP Foundry (CrossDesk) reference signer service - docs/SIGNER_PROTOCOL.md §4, protocol v2.
 *
 *   node dist/index.js [--config signer.yml] [--check] [--once]
 *
 *   --check  load the config, reach CrossDesk, verify the seat and its conditions per instrument, exit
 *   --once   one poll pass, act on what is open, exit (for cron or a one-off run)
 */
import { loadConfig, secretsOf, type Config } from './config';
import { CrossDeskClient } from './client';
import { State } from './state';
import { createApp, type Runtime } from './server';
import { handleProposal, type HandlerDeps } from './handler';
import { fieldsFor, ruleFor, satisfiedAlternative } from './evaluate';
import { ProtocolCache, type InstrumentProtocol } from './protocol';
import * as log from './log';

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}
const flag = (name: string): boolean => process.argv.includes(name);

/**
 * Before acting: the credential is a signer for this seat; the protocol has the seat;
 * and for EVERY configured instrument, every condition the protocol lists for the seat
 * under that instrument's reserve model has a rule in this build and a satisfied source
 * set in the config. A problem here means the service will not run at all - a seat that
 * would halt on every proposal is better told so at start.
 */
async function preflight(config: Config, client: CrossDeskClient, cache: ProtocolCache): Promise<{ version: string; models: Record<string, string> }> {
  const base = await client.signerProtocol();
  if (!base.roles.some((r) => r.key === config.seat)) {
    throw new Error(`CrossDesk protocol ${base.version} has no '${config.seat}' role; it has ${base.roles.map((r) => r.key).join(', ')}`);
  }

  const me = await client.me();
  if (me.role !== 'signer' && me.role !== 'admin') {
    throw new Error(`the credential maps to role '${me.role}', not a signer`);
  }
  if (me.seat && me.seat !== config.seat) {
    throw new Error(`the credential holds the '${me.seat}' seat but signer.yml says '${config.seat}'`);
  }
  // NO SEAT IS NOT A WARNING, IT IS A WALL. `POST /api/proposals/{cid}/confirm` requires a
  // signer SEAT, so a credential without one (an admin key used directly, which is the easy
  // mistake on a host running AUTH_MODE=firebase) reaches every proposal, evaluates it, and is
  // then refused 403 "your user has no signer seat" - and the 4xx is recorded as terminal, so
  // the proposal is never retried after the credential is fixed. Fail at start instead.
  if (!me.seat) {
    throw new Error(
      `the credential maps to '${me.email ?? me.uid}' (role ${me.role}) with no signer seat, so every confirm `
      + `would be refused. Use a credential issued to the ${config.seat} seat, or - on a host whose roster `
      + `seats have no sign-in of their own - keep the admin key and set crossdesk.actAs to the ${config.seat} `
      + `user's e-mail (sent as X-Act-As).`,
    );
  }
  const notMine = config.instruments.filter((i) => me.instruments && !me.instruments.some((x) => x.toLowerCase() === i.toLowerCase()));
  if (notMine.length) log.warn('preflight', { note: `credential is not a signer for ${notMine.join(', ')}; those proposals will not be visible` });

  const problems: string[] = [];
  const notes: string[] = [];
  const models: Record<string, string> = {};
  const applied = new Set<string>();
  const perInstrument: Record<string, InstrumentProtocol> = {};
  for (const instrument of config.instruments) {
    const proto = await cache.lookup(instrument);
    perInstrument[instrument] = proto;
    models[instrument] = proto.model;
    for (const c of proto.role.conditions) {
      applied.add(c.name);
      const spec = ruleFor(config.seat, c.name);
      if (!spec) {
        problems.push(`${instrument}: protocol names '${c.name}' for ${config.seat}; this build has no rule for it (upgrade the signer)`);
        continue;
      }
      const configured = Object.keys(config.conditions[c.name] ?? {});
      if (config.seat === 'venue' && c.name === 'no-prints-attested' && !config.venue.attestNoPrints) {
        notes.push(`${instrument}: venue.attestNoPrints is off; an empty window will halt the seat instead of attesting 'no-prints-attested'`);
        continue;
      }
      if (!satisfiedAlternative(spec, configured)) {
        problems.push(`${instrument}: conditions.${c.name}: needs ${spec.needs.map((a) => a.join('+')).join(' or ')} (accepted fields: ${fieldsFor(config.seat, c.name).join(', ')}); configured: ${configured.join(', ') || 'nothing'}`);
      }
    }
  }
  for (const name of Object.keys(config.conditions)) {
    if (!applied.has(name)) {
      problems.push(`conditions.${name} is not a ${config.seat} condition for any configured instrument in protocol ${base.version}; applied: ${Array.from(applied).join(', ')}`);
    }
  }
  for (const n of notes) log.warn('preflight', { note: n });
  if (problems.length) {
    for (const p of problems) log.error('preflight', { problem: p });
    throw new Error(`${problems.length} configuration problem(s); the service will not confirm anything until they are fixed`);
  }
  log.info('preflight', {
    protocol: base.version,
    seat: config.seat,
    as: me.email ?? me.uid,
    party: me.party,
    authMode: client.authMode,
    actingAs: client.actingAs,
    instruments: Object.fromEntries(config.instruments.map((i) => [i, {
      reserveModel: perInstrument[i].model,
      conditions: perInstrument[i].role.conditions.map((c) => ({ name: c.name, evidenceDeclared: c.evidence !== undefined, fields: Object.keys(config.conditions[c.name] ?? {}) })),
    }])),
    venue: config.seat === 'venue' ? config.venue : undefined,
    tolerances: config.tolerances,
  });
  return { version: base.version, models };
}

async function pollOnce(deps: HandlerDeps, rt: Runtime): Promise<void> {
  const h = rt.health.poll;
  try {
    const open = await deps.client.openProposals();
    h.lastAt = new Date().toISOString();
    h.lastOk = true;
    h.lastError = null;
    log.debug('poll', { open: open.length });
    for (const p of open) {
      await handleProposal(deps, p);
    }
  } catch (e) {
    // Includes a 2xx with a non-JSON body: the client refuses to hand that back as a
    // list, so it lands here and /health goes red.
    h.lastAt = new Date().toISOString();
    h.lastOk = false;
    h.lastError = e instanceof Error ? e.message : String(e);
    log.error('poll', { error: h.lastError });
  }
}

async function main(): Promise<void> {
  const file = arg('--config') ?? process.env.SIGNER_CONFIG ?? './signer.yml';
  const config = loadConfig(file);
  log.registerSecrets(secretsOf(config));
  const client = new CrossDeskClient({ ...config.crossdesk });
  const cache = new ProtocolCache(client, config.seat, config.crossdesk.protocolCacheSeconds * 1000);
  const pre = await preflight(config, client, cache);
  if (flag('--check')) {
    log.info('check', { ok: true, protocol: pre.version, reserveModels: pre.models });
    return;
  }
  const state = new State(config.state.file);
  const deps: HandlerDeps = { config, client, state, protocol: cache.lookup };
  const rt: Runtime = {
    deps,
    health: {
      status: 'ok',
      seat: config.seat,
      instruments: config.instruments,
      authMode: client.authMode,
      webhook: { path: config.server.webhookPath, secretConfigured: Boolean(config.crossdesk.webhookSecret), received: 0, rejected: 0 },
      poll: { enabled: config.crossdesk.poll.enabled, intervalSeconds: config.crossdesk.poll.intervalSeconds, lastAt: null, lastOk: null, lastError: null },
      acted: state.size(),
      protocolVersion: pre.version,
      reserveModels: pre.models,
      startedAt: new Date().toISOString(),
    },
  };

  if (flag('--once')) {
    await pollOnce(deps, rt);
    return;
  }

  const app = createApp(config, client, rt);
  const server = app.listen(config.server.port, config.server.host, () => {
    log.info('listening', { host: config.server.host, port: config.server.port, webhook: config.server.webhookPath, health: '/health', state: state.file });
  });

  let timer: NodeJS.Timeout | undefined;
  if (config.crossdesk.poll.enabled) {
    await pollOnce(deps, rt);
    timer = setInterval(() => void pollOnce(deps, rt), config.crossdesk.poll.intervalSeconds * 1000);
  } else {
    log.info('poll', { enabled: false, note: 'webhook only; set crossdesk.poll.enabled: true if CrossDesk cannot reach this host' });
  }

  const stop = (sig: string) => {
    log.info('stopping', { signal: sig });
    if (timer) clearInterval(timer);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(0), 3000).unref();
  };
  process.on('SIGINT', () => stop('SIGINT'));
  process.on('SIGTERM', () => stop('SIGTERM'));
}

main().catch((e) => {
  log.error('fatal', { error: e instanceof Error ? e.message : String(e) });
  process.exit(1);
});
