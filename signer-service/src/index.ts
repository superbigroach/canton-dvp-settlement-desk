#!/usr/bin/env node
/**
 * CrossDesk reference signer service - docs/SIGNER_PROTOCOL.md §4.
 *
 *   node dist/index.js [--config signer.yml] [--check] [--once]
 *
 *   --check  load the config, reach CrossDesk, verify the seat and its conditions, exit
 *   --once   one poll pass, act on what is open, exit (for cron or a one-off run)
 */
import { loadConfig, type Config } from './config';
import { CrossDeskClient, type ProtocolRole } from './client';
import { State } from './state';
import { createApp, type Runtime } from './server';
import { handleProposal, type HandlerDeps } from './handler';
import { fieldsFor, ruleFor, satisfiedAlternative } from './evaluate';
import * as log from './log';

function arg(name: string): string | undefined {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : undefined;
}
const flag = (name: string): boolean => process.argv.includes(name);

async function preflight(config: Config, client: CrossDeskClient): Promise<ProtocolRole | undefined> {
  const protocol = await client.signerProtocol();
  const role = protocol.roles.find((r) => r.key === config.seat);
  if (!role) throw new Error(`CrossDesk protocol ${protocol.version} has no '${config.seat}' role`);

  const me = await client.me();
  if (me.role !== 'signer' && me.role !== 'admin') {
    throw new Error(`the credential maps to role '${me.role}', not a signer`);
  }
  if (me.seat && me.seat !== config.seat) {
    throw new Error(`the credential holds the '${me.seat}' seat but signer.yml says '${config.seat}'`);
  }
  const notMine = config.instruments.filter((i) => me.instruments && !me.instruments.some((x) => x.toLowerCase() === i.toLowerCase()));
  if (notMine.length) log.warn('preflight', { note: `credential is not a signer for ${notMine.join(', ')}; those proposals will not be visible` });

  // Every condition the protocol names for this seat needs a rule and a satisfied source set.
  const problems: string[] = [];
  for (const c of role.conditions) {
    const spec = ruleFor(config.seat, c.name);
    if (!spec) {
      problems.push(`protocol names '${c.name}' for ${config.seat}; this build has no rule for it (upgrade the signer)`);
      continue;
    }
    const configured = Object.keys(config.conditions[c.name] ?? {});
    if (!satisfiedAlternative(spec, configured)) {
      problems.push(`conditions.${c.name}: needs ${spec.needs.map((a) => a.join('+')).join(' or ')} (accepted fields: ${fieldsFor(config.seat, c.name).join(', ')}); configured: ${configured.join(', ') || 'nothing'}`);
    }
  }
  for (const name of Object.keys(config.conditions)) {
    if (!role.conditions.some((c) => c.name === name)) {
      problems.push(`conditions.${name} is not a ${config.seat} condition in protocol ${protocol.version}; expected ${role.conditions.map((c) => c.name).join(', ')}`);
    }
  }
  if (problems.length) {
    for (const p of problems) log.error('preflight', { problem: p });
    throw new Error(`${problems.length} configuration problem(s); the service will not confirm anything until they are fixed`);
  }
  log.info('preflight', {
    protocol: protocol.version,
    seat: config.seat,
    as: me.email ?? me.uid,
    party: me.party,
    authMode: client.authMode,
    instruments: config.instruments,
    conditions: role.conditions.map((c) => ({ name: c.name, evidenceDeclared: c.evidence !== undefined, fields: Object.keys(config.conditions[c.name] ?? {}) })),
    tolerances: config.tolerances,
  });
  return role;
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
    h.lastAt = new Date().toISOString();
    h.lastOk = false;
    h.lastError = e instanceof Error ? e.message : String(e);
    log.error('poll', { error: h.lastError });
  }
}

async function main(): Promise<void> {
  const file = arg('--config') ?? process.env.SIGNER_CONFIG ?? './signer.yml';
  const config = loadConfig(file);
  const client = new CrossDeskClient({ ...config.crossdesk });
  const role = await preflight(config, client);
  if (flag('--check')) {
    log.info('check', { ok: true });
    return;
  }
  const state = new State(config.state.file);
  const deps: HandlerDeps = { config, client, state, role };
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
      protocolVersion: null,
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
