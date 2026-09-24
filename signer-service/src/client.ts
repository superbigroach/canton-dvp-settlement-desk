/** The CrossDesk API - docs/PRODUCT-PLAN.md §5, the signer's slice of it. */

export interface ProtocolCondition {
  name: string;
  passesWhen?: string;
  /** The evidence schema: { required, verifiedBy, rule?, fields: [{ name, type, description }] }. */
  evidence?: unknown;
}

export interface ProtocolRole {
  key: string;
  title?: string;
  uniquelyKnows?: string;
  conditions: ProtocolCondition[];
  requiresObservedRange?: boolean;
}

/** `GET /api/signer-protocol[?instrument=]` - the roles as they apply to that instrument's reserve model. */
export interface SignerProtocol {
  version: string;
  roles: ProtocolRole[];
}

export interface Me {
  uid: string;
  email?: string;
  role: string;
  party?: string;
  seat?: string;
  instruments?: string[];
}

/** One row of GET /api/proposals - only the fields this service reads. */
export interface Proposal {
  cid: string;
  rootCid?: string;
  instrument: string;
  price: number | string;
  referencePrice?: number | string | null;
  wrapperFactor?: number | string | null;
  status?: string;
  deadline?: string;
  /** The seat's conditions as the ROW lists them - the strict profile. The per-instrument protocol lookup is authoritative. */
  conditions?: string[];
  requiresObservedRange?: boolean;
  my?: { seat?: string; action?: string; canConfirm?: boolean };
  mine?: { action?: string; at?: string; checks?: string[]; evidence?: unknown; condition?: string; reason?: string } | null;
}

export interface HttpResult<T = unknown> {
  status: number;
  ok: boolean;
  body: T;
  /** A 2xx whose body was not JSON. Never `ok`: a login page or a proxy error with a 200 is a failure, not data. */
  nonJson?: boolean;
}

export class ApiError extends Error {
  constructor(public readonly status: number, public readonly body: unknown, message: string) {
    super(message);
  }
}

export interface ClientOptions {
  baseUrl: string;
  apiKey?: string;
  sandboxUser?: string;
  timeoutMs?: number;
  fetchImpl?: typeof fetch;
}

function snippet(text: unknown, n = 80): string {
  return String(text ?? '').replace(/\s+/g, ' ').slice(0, n);
}

export class CrossDeskClient {
  private readonly base: string;
  private readonly timeoutMs: number;
  private readonly fetchImpl: typeof fetch;
  readonly authMode: 'apikey' | 'sandbox-header';

  constructor(private readonly opts: ClientOptions) {
    this.base = opts.baseUrl.replace(/\/+$/, '');
    this.timeoutMs = opts.timeoutMs ?? 15000;
    this.fetchImpl = opts.fetchImpl ?? fetch;
    this.authMode = opts.apiKey ? 'apikey' : 'sandbox-header';
  }

  /** API key wins when both are set: the key is the production path, the header is the sandbox's. */
  authHeaders(): Record<string, string> {
    if (this.opts.apiKey) return { authorization: `Bearer ${this.opts.apiKey}` };
    if (this.opts.sandboxUser) return { 'x-sandbox-user': this.opts.sandboxUser };
    return {};
  }

  private async request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<HttpResult<T>> {
    const headers: Record<string, string> = { accept: 'application/json', ...this.authHeaders() };
    if (body !== undefined) headers['content-type'] = 'application/json';
    const res = await this.fetchImpl(this.base + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(this.timeoutMs),
    });
    const text = await res.text();
    let parsed: unknown = text;
    let isJson = false;
    try {
      parsed = text.trim() ? JSON.parse(text) : null;
      isJson = text.trim() !== '';
    } catch {
      /* keep the text; the caller decides what a non-JSON body means */
    }
    if (res.ok && !isJson) {
      // A 2xx that is not JSON is not a success. It is a captive portal, a proxy page, an
      // empty reply from the wrong host - and iterating a string as a proposal list, or
      // recording a confirm as done, would be acting on nothing (audit 2026-09-22, #15).
      return { status: res.status, ok: false, body: parsed as T, nonJson: true };
    }
    return { status: res.status, ok: res.ok, body: parsed as T };
  }

  private async must<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
    const r = await this.request<T>(method, path, body);
    if (r.nonJson) {
      throw new ApiError(r.status, r.body, `${method} ${path} -> HTTP ${r.status} but the body is not JSON: '${snippet(r.body)}'`);
    }
    if (!r.ok) {
      const b = r.body as unknown;
      const msg = typeof b === 'object' && b !== null && 'message' in b
        ? String((b as { message: unknown }).message)
        : snippet(b, 200);
      throw new ApiError(r.status, r.body, `${method} ${path} -> HTTP ${r.status}: ${msg}`);
    }
    return r.body;
  }

  /**
   * The protocol, with the ISSUER seat as the instrument's reserve model defines it when an
   * instrument is given; the strict (attested) profile otherwise.
   */
  async signerProtocol(instrument?: string): Promise<SignerProtocol> {
    const path = instrument ? `/api/signer-protocol?instrument=${encodeURIComponent(instrument)}` : '/api/signer-protocol';
    const p = await this.must<SignerProtocol>('GET', path);
    if (!p || typeof p !== 'object' || typeof p.version !== 'string' || !Array.isArray(p.roles)) {
      throw new ApiError(200, p, `GET ${path} -> a JSON body that is not a signer protocol ({version, roles[]})`);
    }
    return p;
  }

  me(): Promise<Me> {
    return this.must<Me>('GET', '/api/me');
  }

  private async proposalList(path: string): Promise<Proposal[]> {
    const list = await this.must<unknown>('GET', path);
    if (!Array.isArray(list)) {
      throw new ApiError(200, list, `GET ${path} -> a JSON body that is not a list of proposals: '${snippet(JSON.stringify(list))}'`);
    }
    return list as Proposal[];
  }

  openProposals(): Promise<Proposal[]> {
    return this.proposalList('/api/proposals?status=open&mine=true');
  }

  allProposals(): Promise<Proposal[]> {
    return this.proposalList('/api/proposals?status=all&mine=true');
  }

  async proposal(cid: string): Promise<Proposal> {
    const p = await this.must<Proposal>('GET', `/api/proposals/${encodeURIComponent(cid)}`);
    if (!p || typeof p !== 'object' || typeof p.cid !== 'string') {
      throw new ApiError(200, p, `GET /api/proposals/${cid} -> a JSON body that is not a proposal`);
    }
    return p;
  }

  /**
   * Confirm-with-checks. `evidence` is keyed by condition - { "<condition>": { field: value } } -
   * which the backend verifies for every seat but the venue; for the venue the traded range
   * is also present at the top level as {low, high}, the shape the ledger enforces, or is
   * absent when the venue attests `no-prints-attested`.
   */
  confirm(cid: string, checks: string[], evidence: Record<string, unknown>): Promise<HttpResult> {
    return this.request('POST', `/api/proposals/${encodeURIComponent(cid)}/confirm`, { checks, evidence });
  }

  refuse(cid: string, condition: string, reason: string): Promise<HttpResult> {
    return this.request('POST', `/api/proposals/${encodeURIComponent(cid)}/refuse`, { condition, reason });
  }
}
