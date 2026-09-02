// The product API client (§5). Same discipline as api.ts: reject ONLY with an ApiError
// that carries a sentence, and never invent a body the server did not send.
import { ApiError, errorMessage } from '../api';
import { authHeaders } from '../auth/token';
import type {
  ApFund, Benchmark, Committee, ConfirmBody, FixingEvent, FixingSchedule, FundDashboard, Me,
  Methodology, Proposal, ProposalEvent, Receipt, RefuseBody, ScheduleRow, SeriesRow,
  SignerSettings, StrikeResult, UserInput, UserRow,
} from './types';
import type { SignerProtocolResponse } from '../api';

function parseJson(text: string): unknown {
  if (!text) return null;
  try { return JSON.parse(text) as unknown; } catch { return null; }
}

const asText = (v: unknown) => (typeof v === 'string' ? v.trim() : '');

function statusSentence(status: number, path: string): string {
  if (status === 0) return `no response from CrossDesk (${path})`;
  if (status === 401) return 'not signed in, or the session has expired — sign in again';
  if (status === 403) return 'your role is not allowed to do that';
  if (status === 404) return `this route is not available on the backend yet (${path})`;
  if (status >= 502 && status <= 504) return 'CrossDesk or its Canton participant is not responding — check GET /api/diag';
  return `CrossDesk returned HTTP ${status} for ${path}`;
}

async function call<T>(path: string, init: RequestInit = {}): Promise<T> {
  const auth = await authHeaders();
  // A caller may override a header to '' to SUPPRESS it (e.g. X-Act-As for a call that
  // must run as the real admin). Empty headers are dropped, never sent empty.
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...auth, ...((init.headers as Record<string, string>) || {}) };
  for (const k of Object.keys(headers)) if (headers[k] === '') delete headers[k];
  let res: Response;
  try {
    res = await fetch(`/api${path}`, { ...init, headers });
  } catch (e) {
    throw new ApiError(`cannot reach CrossDesk at /api${path} — ${errorMessage(e)}`, 0);
  }
  const text = await res.text();
  const body = parseJson(text);
  if (!res.ok) {
    const b = body && typeof body === 'object' ? (body as Record<string, unknown>) : null;
    const plain = !text.includes('<') ? asText(text).slice(0, 300) : '';
    throw new ApiError(
      (b && (asText(b.message) || asText(b.error))) || plain || statusSentence(res.status, path),
      res.status,
      { code: b ? asText(b.code) || undefined : undefined, hint: b ? asText(b.hint) || undefined : undefined },
    );
  }
  if (text && body === null) throw new ApiError(`CrossDesk returned a non-JSON response for ${path}`, res.status);
  return body as T;
}

const q = (params: Record<string, string | number | boolean | undefined>) => {
  const s = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
  return s ? `?${s}` : '';
};

const post = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) });
const put = (body: unknown): RequestInit => ({ method: 'PUT', body: JSON.stringify(body) });

/** A CSV download must carry the auth header too, so it is fetched, not linked. */
async function download(path: string, filename: string): Promise<void> {
  const auth = await authHeaders();
  const res = await fetch(`/api${path}`, { headers: auth });
  if (!res.ok) {
    const text = await res.text();
    const b = parseJson(text) as Record<string, unknown> | null;
    throw new ApiError((b && asText(b.message)) || statusSentence(res.status, path), res.status);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

export const realClient = {
  // public
  benchmarks: () => call<Benchmark[]>('/benchmarks'),
  benchmark: (id: string) => call<Benchmark>(`/benchmarks/${encodeURIComponent(id)}`),
  series: (id: string, p: { from?: string; to?: string; limit?: number } = {}) =>
    call<SeriesRow[]>(`/series/${encodeURIComponent(id)}${q(p)}`),
  seriesCsv: (id: string) => download(`/series/${encodeURIComponent(id)}.csv`, `${id}-series.csv`),
  methodology: () => call<Methodology>('/methodology'),
  signerProtocol: () => call<SignerProtocolResponse>('/signer-protocol'),
  fixingSchedule: () => call<FixingSchedule>('/fixing-schedule'),

  // identity
  me: () => call<Me>('/me'),

  // signer
  proposals: (status: 'open' | 'all' = 'open') => call<Proposal[]>(`/proposals${q({ status, mine: true })}`),
  proposalEvents: (cid: string) => call<ProposalEvent[]>(`/proposals/${encodeURIComponent(cid)}/events`),
  confirm: (cid: string, body: ConfirmBody) =>
    call<{ cid?: string; contractId?: string }>(`/proposals/${encodeURIComponent(cid)}/confirm`, post(body)),
  refuse: (cid: string, body: RefuseBody) =>
    call<{ cid?: string; contractId?: string }>(`/proposals/${encodeURIComponent(cid)}/refuse`, post(body)),
  signerSettings: () => call<SignerSettings>('/signer/settings'),
  saveSignerSettings: (s: SignerSettings) => call<SignerSettings>('/signer/settings', put(s)),
  createApiKey: () => call<{ key: string }>('/signer/apikey', { method: 'POST' }),
  revokeApiKey: () => call<unknown>('/signer/apikey', { method: 'DELETE' }),

  // AP
  apFunds: () => call<ApFund[]>('/ap/funds'),
  apCreate: (fundId: string, shares: number) => call<Receipt>('/ap/create', post({ fundId, shares })),
  apRedeem: (fundId: string, shares: number) => call<Receipt>('/ap/redeem', post({ fundId, shares })),
  apReceipts: () => call<Receipt[]>('/ap/receipts'),

  // fund admin
  fundDashboard: (id: string) => call<FundDashboard>(`/fund/${encodeURIComponent(id)}/dashboard`),

  // admin
  schedule: () => call<ScheduleRow[]>('/admin/schedule'),
  saveSchedule: (rows: ScheduleRow[]) => call<ScheduleRow[]>('/admin/schedule', put(rows)),
  strikeNow: (id: string) => call<StrikeResult>(`/admin/strike/${encodeURIComponent(id)}`, { method: 'POST' }),
  committees: () => call<Committee[]>('/admin/committees'),
  users: () => call<UserRow[]>('/admin/users'),
  /** The mapping as the REAL caller — used by "View as" while acting as someone else. */
  usersAsSelf: () => call<UserRow[]>('/admin/users', { headers: { 'X-Act-As': '' } }),
  addUser: (u: UserInput) => call<UserRow>('/admin/users', post(u)),
  updateUser: (uid: string, u: UserInput) => call<UserRow>(`/admin/users/${encodeURIComponent(uid)}`, put(u)),
  events: (p: { instrument?: string; from?: string; to?: string } = {}, base: 'admin' | 'audit' = 'admin') =>
    call<FixingEvent[]>(`/${base}/events${q(p)}`),
  eventsCsv: (p: { instrument?: string; from?: string; to?: string } = {}, base: 'admin' | 'audit' = 'admin') =>
    download(`/${base}/events.csv${q(p)}`, `crossdesk-events${p.instrument ? '-' + p.instrument : ''}.csv`),
};

export type DeskClient = typeof realClient;
