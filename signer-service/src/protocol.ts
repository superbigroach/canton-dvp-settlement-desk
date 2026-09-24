/**
 * Per-instrument protocol lookup: `GET /api/signer-protocol?instrument=<id>`.
 *
 * WHY PER INSTRUMENT. In v2 each instrument declares a reserve model (attested,
 * onchain-verifiable, custodial) and the ISSUER seat's condition list depends on it: cBTC
 * asks for an attestor quorum, cETH cannot (its lock is on-chain) and must not be asked
 * for a number it would have to invent. The proposal row's `conditions` still lists the
 * strict profile (audit 2026-09-22, medium #5), so the row is never used to decide what to
 * evaluate - this lookup is.
 *
 * The wire carries no model NAME, only the resulting role; the model is inferred here for
 * the log and the health page, from the issuer role's condition list and wording.
 */
import type { CrossDeskClient, ProtocolRole, SignerProtocol } from './client';
import type { Seat } from './config';

export type ReserveModel = 'attested' | 'onchain-verifiable' | 'custodial' | 'unknown';

export interface InstrumentProtocol {
  version: string;
  instrument: string;
  /** The reserve model this instrument's issuer profile corresponds to. */
  model: ReserveModel;
  /** This seat's role as it applies to the instrument. */
  role: ProtocolRole;
  /** Every role, for diagnostics. */
  roles: ProtocolRole[];
}

export type ProtocolLookup = (instrument: string) => Promise<InstrumentProtocol>;

export function inferReserveModel(protocol: SignerProtocol): ReserveModel {
  const issuer = protocol.roles.find((r) => r.key === 'issuer');
  if (!issuer) return 'unknown';
  const names = issuer.conditions.map((c) => c.name);
  if (names.includes('attestor-quorum')) return 'attested';
  const rc = issuer.conditions.find((c) => c.name === 'reserves-current');
  const text = `${rc?.passesWhen ?? ''} ${describe(rc?.evidence)}`.toLowerCase();
  if (/on-chain|onchain|lock/.test(text)) return 'onchain-verifiable';
  if (/custod/.test(text)) return 'custodial';
  return 'unknown';
}

function describe(evidence: unknown): string {
  if (!evidence || typeof evidence !== 'object') return '';
  const fields = (evidence as { fields?: unknown }).fields;
  if (!Array.isArray(fields)) return '';
  return fields.map((f) => String((f as { description?: unknown })?.description ?? '')).join(' ');
}

export function protocolFor(seat: Seat, instrument: string, protocol: SignerProtocol): InstrumentProtocol {
  const role = protocol.roles.find((r) => r.key === seat);
  if (!role) throw new Error(`CrossDesk protocol ${protocol.version} has no '${seat}' role for instrument ${instrument}`);
  return { version: protocol.version, instrument, model: inferReserveModel(protocol), role, roles: protocol.roles };
}

/** Caches one answer per instrument for `ttlMs`; a failed fetch is not cached, so the next evaluation retries. */
export class ProtocolCache {
  private readonly entries = new Map<string, { at: number; value: InstrumentProtocol }>();

  constructor(
    private readonly client: CrossDeskClient,
    private readonly seat: Seat,
    private readonly ttlMs: number,
    private readonly now: () => number = () => Date.now(),
  ) {}

  readonly lookup: ProtocolLookup = async (instrument: string) => {
    const key = instrument.toUpperCase();
    const hit = this.entries.get(key);
    if (hit && this.now() - hit.at < this.ttlMs) return hit.value;
    const protocol = await this.client.signerProtocol(instrument);
    const value = protocolFor(this.seat, instrument, protocol);
    this.entries.set(key, { at: this.now(), value });
    return value;
  };

  /** What is cached, for /health. */
  models(): Record<string, ReserveModel> {
    const out: Record<string, ReserveModel> = {};
    for (const [k, v] of this.entries) out[k] = v.value.model;
    return out;
  }
}

/** A lookup that always answers with the same role - for tests and for --check dry runs. */
export function staticLookup(seat: Seat, role: ProtocolRole, version = 'SIGNER_PROTOCOL v2', model: ReserveModel = 'unknown'): ProtocolLookup {
  return async (instrument: string) => ({ version, instrument, model, role, roles: [role] });
}

/** A role built from bare condition names - the shape the backend serves, minus the schemas. */
export function roleOf(seat: Seat, conditions: string[], requiresObservedRange = seat === 'venue'): ProtocolRole {
  return { key: seat, conditions: conditions.map((name) => ({ name })), requiresObservedRange };
}
