import { createHmac, timingSafeEqual } from 'node:crypto';

export const SIGNATURE_HEADER = 'x-crossdesk-signature';

/** `sha256=` + hex HMAC-SHA256 over the exact body bytes - backend WebhookSigner.java. */
export function signature(secret: string, body: Buffer | string): string {
  return 'sha256=' + createHmac('sha256', secret).update(body).digest('hex');
}

/** Constant-time compare of the header against the recomputed signature. */
export function verifySignature(secret: string | undefined, body: Buffer, header: string | undefined): boolean {
  if (!secret || !header) return false;
  const expected = Buffer.from(signature(secret, body), 'utf8');
  const given = Buffer.from(header.trim().toLowerCase(), 'utf8');
  if (expected.length !== given.length) return false;
  return timingSafeEqual(expected, given);
}

export interface WebhookPayload {
  type: 'proposal.created' | 'proposal.restruck' | 'fixing.finalized' | 'fixing.missed' | string;
  instrument: string;
  proposalCid: string;
  price?: number | string;
  referencePrice?: number | string;
  wrapperFactor?: number | string;
  conditions?: string[];
  deadline?: string | null;
}

export const ACTIONABLE_TYPES = new Set(['proposal.created', 'proposal.restruck']);

export function parsePayload(body: Buffer): WebhookPayload {
  const parsed = JSON.parse(body.toString('utf8')) as Partial<WebhookPayload>;
  if (!parsed || typeof parsed !== 'object') throw new Error('payload is not an object');
  if (typeof parsed.type !== 'string') throw new Error('payload.type missing');
  if (typeof parsed.proposalCid !== 'string' || !parsed.proposalCid) throw new Error('payload.proposalCid missing');
  if (typeof parsed.instrument !== 'string' || !parsed.instrument) throw new Error('payload.instrument missing');
  return parsed as WebhookPayload;
}
