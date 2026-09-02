// Small shared pieces: async loading, countdowns, a confirm dialog, formatting.
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { errorMessage } from '../api';

// ---- data loading -----------------------------------------------------------

export interface Async<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
  setData: (updater: (prev: T | null) => T | null) => void;
}

/** Load once (and on demand). Errors are sentences, never thrown into the tree. */
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[] = []): Async<T> {
  const [data, setDataState] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fn().then((d) => { if (!cancelled && alive.current) { setDataState(d); setError(null); } })
      .catch((e) => { if (!cancelled && alive.current) setError(errorMessage(e)); })
      .finally(() => { if (!cancelled && alive.current) setLoading(false); });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tick, ...deps]);
  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((u: (prev: T | null) => T | null) => setDataState(u), []);
  return { data, error, loading, reload, setData };
}

export function LoadState({ loading, error, empty, children, onRetry }: {
  loading: boolean; error: string | null; empty?: string | null; children?: ReactNode; onRetry?: () => void;
}) {
  if (loading && !error) return <div className="empty" role="status">Loading…</div>;
  if (error) {
    return (
      <div className="banner error" role="alert">
        <span>{error}</span>
        {onRetry && <button type="button" className="ghost small" onClick={onRetry}>Retry</button>}
      </div>
    );
  }
  if (empty) return <div className="empty">{empty}</div>;
  return <>{children}</>;
}

// ---- formatting -------------------------------------------------------------

export const fmtN = (n: number | null | undefined, dp = 2) =>
  n === null || n === undefined || Number.isNaN(n) ? '—'
    : n.toLocaleString(undefined, { minimumFractionDigits: dp, maximumFractionDigits: dp });
export const fmtQty = (n: number | null | undefined) =>
  n === null || n === undefined ? '—' : n.toLocaleString(undefined, { maximumFractionDigits: 6 });
export const fmtTs = (iso: string | null | undefined) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
};
export const fmtTime = (iso: string | null | undefined) => {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' });
};
export const shortCid = (cid: string | null | undefined) =>
  !cid ? '—' : cid.length > 18 ? `${cid.slice(0, 8)}…${cid.slice(-6)}` : cid;

/** Gold is for a committee-attested value and nothing else (§8). */
export const isOfficial = (tier: number | null | undefined) => tier === 1;

/** "attested by K of N" ONLY when K real signatures exist (§8). */
export function TierTag({ tier, k, n, label }: { tier?: number; k?: number; n?: number; label?: string }) {
  if (tier === 1 && k && n) return <span className="tag attested">attested {k} of {n}</span>;
  if (tier === undefined || tier === null) return null;
  const word = label ?? (tier === 2 ? 'alternate seats' : tier === 3 ? 'benchmark × factor' : tier === 4 ? 'prior fixing' : tier === 5 ? 'missed' : undefined);
  // Tier 0 is the backend's seed ("seed" is its own label for it): say what that means.
  const text = tier === 0 ? `${label && label !== 'seed' ? label : 'seed'} · not attested` : word ? `tier ${tier} · ${word}` : `tier ${tier}`;
  return <span className="tag fallback" title={tier === 0 ? 'A seeded value: no committee has attested it, so it is not official.' : undefined}>{text}</span>;
}

// ---- countdown --------------------------------------------------------------

export function useNow(intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), intervalMs); return () => clearInterval(t); }, [intervalMs]);
  return now;
}

export function Countdown({ to }: { to: string }) {
  const now = useNow();
  const end = new Date(to).getTime();
  if (Number.isNaN(end)) return <span className="mono muted">{to}</span>;
  const s = Math.round((end - now) / 1000);
  if (s <= 0) return <span className="mono countdown over">window closed</span>;
  const m = Math.floor(s / 60);
  const rest = s % 60;
  const cls = s < 300 ? 'countdown urgent' : 'countdown';
  // Under an hour: m:ss, the restrike window. Longer (a cutoff tomorrow): hours and minutes.
  if (m >= 60) return <span className={`mono ${cls}`}>{Math.floor(m / 60)}h {(m % 60).toString().padStart(2, '0')}m left</span>;
  return <span className={`mono ${cls}`}>{m}:{rest.toString().padStart(2, '0')} left</span>;
}

// ---- confirm dialog ---------------------------------------------------------

export function ConfirmDialog({ open, title, children, confirmLabel, onConfirm, onCancel, busy, danger }: {
  open: boolean; title: string; children: ReactNode; confirmLabel: string;
  onConfirm: () => void; onCancel: () => void; busy?: boolean; danger?: boolean;
}) {
  const first = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    first.current?.focus();
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onCancel(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onCancel]);
  if (!open) return null;
  return (
    <div className="dialog-backdrop" onClick={onCancel}>
      <div className="dialog card" role="dialog" aria-modal="true" aria-labelledby="dlg-title" onClick={(e) => e.stopPropagation()}>
        <h2 id="dlg-title">{title}</h2>
        <div className="dialog-body">{children}</div>
        <div className="dialog-actions">
          <button type="button" className="ghost" onClick={onCancel} disabled={busy}>Cancel</button>
          <button ref={first} type="button" className={`primary${danger ? ' sell' : ''}`} onClick={onConfirm} disabled={busy}>
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

/** A number input that keeps its own text so a half-typed "0." is not clobbered. */
export function NumberField({ label, value, onChange, step, min, placeholder, mono = true, required, id, hint }: {
  label: string; value: string; onChange: (v: string) => void; step?: string; min?: number;
  placeholder?: string; mono?: boolean; required?: boolean; id?: string; hint?: string;
}) {
  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <input id={id} className={mono ? 'mono' : undefined} type="number" inputMode="decimal" step={step ?? 'any'} min={min}
        value={value} placeholder={placeholder} required={required} onChange={(e) => onChange(e.target.value)} />
      {hint && <small className="field-hint">{hint}</small>}
    </label>
  );
}

export function Stat({ label, value, gold, sub }: { label: string; value: ReactNode; gold?: boolean; sub?: ReactNode }) {
  return (
    <div className="stat">
      <span className="stat-label">{label}</span>
      <span className={`stat-value mono${gold ? ' official' : ''}`}>{value}</span>
      {sub && <span className="stat-sub">{sub}</span>}
    </div>
  );
}
