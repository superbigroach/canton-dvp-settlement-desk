/** The CrossDesk API - docs/PRODUCT-PLAN.md §5, the signer's slice of it. */

export interface ProtocolCondition {
  name: string;
  passesWhen?: string;
  /** Added by the backend alongside this service: the evidence fields the confirm must carry. */
  evidence?: unknown;
}

export interface ProtocolRole {
  key: string;
  title?: string;
  conditions: ProtocolCondition[];
  requiresObservedRange?: boolean;
}

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
  conditions?: string[];
  requiresObservedRange?: boolean;
  my?: { seat?: string; action?: string; canConfirm?: boolean };
  mine?: { action?: string; at?: string; checks?: string[]; evidence?: unknown; condition?: string; reason?: string } | null;
}

export interface HttpResult<T = unknown> {
  status: number;
  ok: boolean;
  body: T;
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
    try {
      parsed = text ? JSON.parse(text) : null;
    } catch {
      /* keep the text */
    }
    return { status: res.status, ok: res.ok, body: parsed as T };
  }

  private async must<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
    const r = await this.request<T>(method, path, body);
    if (!r.ok) {
      const b = r.body as unknown;
      const msg = typeof b === 'object' && b !== null && 'message' in b
        ? String((b as { message: unknown }).message)
        : String(b).slice(0, 200);
      throw new ApiError(r.status, r.body, `${method} ${path} -> HTTP ${r.status}: ${msg}`);
    }
    return r.body;
  }

  signerProtocol(): Promise<SignerProtocol> {
    return this.must<SignerProtocol>('GET', '/api/signer-protocol');
  }

  me(): Promise<Me> {
    return this.must<Me>('GET', '/api/me');
  }

  openProposals(): Promise<Proposal[]> {
    return this.must<Proposal[]>('GET', '/api/proposals?status=open&mine=true');
  }

  allProposals(): Promise<Proposal[]> {
    return this.must<Proposal[]>('GET', '/api/proposals?status=all&mine=true');
  }

  proposal(cid: string): Promise<Proposal> {
    return this.must<Proposal>('GET', `/api/proposals/${encodeURIComponent(cid)}`);
  }

  /**
   * Confirm-with-checks. `evidence` is keyed by condition - { "<condition>": { field: value } } -
   * which the backend verifies for the issuer and lender seats; for the venue the traded
   * range is also present at the top level as {low, high}, the shape the ledger enforces.
   */
  confirm(cid: string, checks: string[], evidence: Record<string, unknown>): Promise<HttpResult> {
    return this.request('POST', `/api/proposals/${encodeURIComponent(cid)}/confirm`, { checks, evidence });
  }

  refuse(cid: string, condition: string, reason: string): Promise<HttpResult> {
    return this.request('POST', `/api/proposals/${encodeURIComponent(cid)}/refuse`, { condition, reason });
  }
}
