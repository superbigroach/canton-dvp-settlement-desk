// What is happening to the assets a fund holds — is today's value signed, how old is it,
// and when is the next strike.
import { useEffect, useState } from 'react';

/**
 * WHY AN AUTHORISED PARTICIPANT NEEDS THIS.
 *
 * Create and redeem settle at the fund's NAV, and that NAV is only as good as the fixings
 * underneath it. The Funds table answers "what is the NAV"; it never answered the questions
 * an AP actually asks before dealing: is that number signed, or a seeded placeholder? How
 * old is it? Is a strike overdue right now? Is there even a committee?
 *
 * Both endpoints are public and unauthenticated, so this renders for anyone — an AP, a
 * prospect, the operator — without a role check or a token.
 */

interface Benchmark {
  id: string;
  name: string;
  publishTime?: string;
  timezone?: string;
  last?: {
    price: number | null;
    tier: number;
    tierLabel?: string;
    displayLabel?: string;
    k: number;
    n: number;
    ageSeconds?: number;
  } | null;
}

interface ScheduleRow {
  instrumentId: string;
  session: string;
  strikeAt: string;
  zone: string;
  state: string;
  minutesLate?: number;
  note?: string;
}

const fmtAge = (s?: number): string => {
  if (s === undefined || s === null) return '—';
  if (s < 90) return `${Math.round(s)}s ago`;
  if (s < 5400) return `${Math.round(s / 60)} min ago`;
  if (s < 172800) return `${Math.round(s / 3600)} h ago`;
  return `${Math.round(s / 86400)} d ago`;
};

const fmtPrice = (p: number | null | undefined): string =>
  p === null || p === undefined ? '—' : p.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function AssetFixingStatus({ only }: { only?: string[] }) {
  const [rows, setRows] = useState<Benchmark[] | null>(null);
  const [sched, setSched] = useState<ScheduleRow[]>([]);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    const load = async () => {
      try {
        const [b, s] = await Promise.all([
          fetch('/api/benchmarks').then((r) => (r.ok ? r.json() : Promise.reject(new Error(`HTTP ${r.status}`)))),
          fetch('/api/fixing-schedule').then((r) => (r.ok ? r.json() : { identifiers: [] })).catch(() => ({ identifiers: [] })),
        ]);
        if (!alive) return;
        setRows(b as Benchmark[]);
        setSched((s?.identifiers ?? []) as ScheduleRow[]);
        setErr(null);
      } catch (e) {
        if (alive) setErr(e instanceof Error ? e.message : String(e));
      }
    };
    void load();
    // The indicative side moves; 30s is often enough to be current without being a stream.
    const t = window.setInterval(() => void load(), 30_000);
    return () => { alive = false; window.clearInterval(t); };
  }, []);

  const shown = (rows ?? []).filter((b) => !only || only.some((i) => i.toLowerCase() === b.id.toLowerCase()));
  if (err) {
    return <p className="hint subtle">Asset status unavailable — {err}</p>;
  }
  if (!rows || shown.length === 0) return null;

  return (
    <div className="card table-wrap">
      <div className="page-head" style={{ marginBottom: 8 }}>
        <h2 style={{ margin: 0, fontSize: '1rem' }}>Assets being fixed</h2>
        <p className="hint" style={{ margin: '4px 0 0' }}>
          What the fund's components are worth, who signed it, and when the next strike is due.
          A value that is not attested cannot settle a create or a redeem.
        </p>
      </div>
      <table>
        <thead>
          <tr>
            <th>ASSET</th>
            <th className="num">LAST VALUE</th>
            <th>ATTESTATION</th>
            <th className="num">AGE</th>
            <th>NEXT STRIKE</th>
          </tr>
        </thead>
        <tbody>
          {shown.map((b) => {
            const s = sched.find((r) => r.instrumentId.toLowerCase() === b.id.toLowerCase());
            const attested = (b.last?.tier ?? 0) === 1;
            const overdue = s?.state === 'OVERDUE';
            return (
              <tr key={b.id}>
                <td>
                  <span className="pill asset">{b.id}</span>
                  <div className="basis">{b.name}</div>
                </td>
                <td className={`num mono${attested ? ' official' : ''}`}>{fmtPrice(b.last?.price)}</td>
                <td>
                  <span className={attested ? 'tag' : 'tag mock'}>
                    {b.last?.displayLabel ?? b.last?.tierLabel ?? 'no value'}
                  </span>
                  {b.last && (
                    <div className="basis mono">
                      {b.last.n > 0 ? `K ${b.last.k} of N ${b.last.n}` : 'no committee seated'}
                    </div>
                  )}
                </td>
                <td className="num mono muted">{fmtAge(b.last?.ageSeconds)}</td>
                <td className="mono">
                  {s ? (
                    <>
                      {s.strikeAt} <span className="muted">{s.zone}</span>
                      <div className={overdue ? 'basis warn-text' : 'basis muted'}>
                        {overdue ? `OVERDUE by ${s.minutesLate ?? '?'} min` : s.state.toLowerCase()}
                      </div>
                    </>
                  ) : (
                    <span className="muted">not scheduled</span>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
