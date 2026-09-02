import express, { type Express, type Request, type Response } from 'express';
import type { Config } from './config';
import type { CrossDeskClient } from './client';
import { ACTIONABLE_TYPES, SIGNATURE_HEADER, parsePayload, verifySignature } from './webhook';
import { handleProposal, type HandlerDeps } from './handler';
import * as log from './log';

export interface Health {
  status: 'ok' | 'degraded';
  seat: string;
  instruments: string[];
  authMode: string;
  webhook: { path: string; secretConfigured: boolean; received: number; rejected: number };
  poll: { enabled: boolean; intervalSeconds: number; lastAt: string | null; lastOk: boolean | null; lastError: string | null };
  acted: number;
  protocolVersion: string | null;
  startedAt: string;
}

export interface Runtime {
  health: Health;
  deps: HandlerDeps;
}

export function createApp(config: Config, client: CrossDeskClient, rt: Runtime): Express {
  const app = express();
  app.disable('x-powered-by');

  app.get('/health', (_req: Request, res: Response) => {
    const h = rt.health;
    h.acted = rt.deps.state.size();
    h.status = h.poll.enabled && h.poll.lastOk === false ? 'degraded' : 'ok';
    res.status(h.status === 'ok' ? 200 : 503).json(h);
  });

  // Raw bytes: the signature is over the exact body CrossDesk sent, not a re-serialisation.
  app.post(config.server.webhookPath, express.raw({ type: '*/*', limit: '256kb' }), async (req: Request, res: Response) => {
    const body: Buffer = Buffer.isBuffer(req.body) ? req.body : Buffer.alloc(0);
    const header = req.header(SIGNATURE_HEADER);
    rt.health.webhook.received++;
    if (!config.crossdesk.webhookSecret) {
      rt.health.webhook.rejected++;
      log.warn('webhook.rejected', { why: 'no webhook secret configured (CROSSDESK_WEBHOOK_SECRET); refusing unsigned delivery' });
      res.status(503).json({ error: 'webhook secret not configured' });
      return;
    }
    if (!verifySignature(config.crossdesk.webhookSecret, body, header)) {
      rt.health.webhook.rejected++;
      log.warn('webhook.rejected', { why: 'bad signature', from: req.ip, bytes: body.length });
      res.status(401).json({ error: 'bad signature' });
      return;
    }
    let payload;
    try {
      payload = parsePayload(body);
    } catch (e) {
      rt.health.webhook.rejected++;
      log.warn('webhook.rejected', { why: `malformed payload: ${e instanceof Error ? e.message : String(e)}` });
      res.status(400).json({ error: 'malformed payload' });
      return;
    }
    log.info('webhook.received', { type: payload.type, instrument: payload.instrument, proposalCid: payload.proposalCid, price: payload.price, deadline: payload.deadline });
    // Acknowledge first; CrossDesk retries on non-2xx and the evaluation may take seconds.
    res.status(202).json({ accepted: true });
    if (!ACTIONABLE_TYPES.has(payload.type)) return;
    try {
      // The webhook is a trigger; the proposal view (root cid, my seat, what I already did) is the truth.
      const p = await client.proposal(payload.proposalCid);
      await handleProposal(rt.deps, p);
    } catch (e) {
      log.error('webhook.followup', { proposalCid: payload.proposalCid, error: e instanceof Error ? e.message : String(e), note: 'the poller will pick it up if polling is enabled' });
    }
  });

  return app;
}
