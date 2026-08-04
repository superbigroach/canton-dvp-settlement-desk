// AccrualTicker — THE FUND EARNING YIELD, ON SCREEN, IN REAL TIME.
// ============================================================================
// A committee cannot re-attest a price every second: 86,400 chances a day to be wrong,
// late, captured or offline. So it attests the INPUTS — base, rate, day-count
// convention, and the instant the mark applies from — and the LEDGER derives every
// later value. This panel is that derivation, running at 10 frames a second in front of
// you: a NAV that ticks up because the fund is earning, not because a UI is animating.
//
// WHY THE NUMBER ON SCREEN IS THE LEDGER'S NUMBER, NOT A LOOKALIKE.
//   * The four attested inputs come from the ledger (`GET /api/fixing/{cid}/nav` reads
//     the NavFixing contract) — nothing here is client state.
//   * `accrual.ts` reproduces `Governance.daml`'s arithmetic in fixed-point BigInt at
//     the ledger's own 10 decimal places, in the ledger's order of operations, with the
//     ledger's half-even rounding. Nothing is routed through a float.
//   * The AGREEMENT CHECK below re-reads the desk's own derivation every 20 seconds and
//     compares it, digit for digit, with the value this browser computed for the desk's
//     `asOf` instant. The badge says AGREES or DIVERGES. It is not decoration — if the
//     two ever parted, the demo would say so before a judge did.
//   * The ticker recomputes from the ATTESTED BASE on every frame — never from the
//     previously displayed value — so ten minutes of animation is bit-identical to one
//     evaluation at that instant. There is no accumulator, so there is no drift.
//
// NO DEMO ACCELERATION. NOT ANY. The clock is the wall clock and the rate is whatever
// the committee attested on-ledger. Motion is visible because the value is shown to the
// ledger's full ten decimal places and the moving digits are highlighted — at 3.6%
// ACT/360 on a 2,400 base the sixth decimal place onwards changes several times a
// second. If a bigger number is wanted for a room at the back, the committee ATTESTS a
// bigger rate (the panel offers presets), which is a real signed attestation on the
// ledger rather than a multiplier hidden in a component.
// ============================================================================

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, ApiError, type AccruedNav } from './api';
import {
  MICROS_PER_DAY,
  accruedAmount,
  anchorConsistentWithNav,
  formatD,
  formatGrouped,
  navAt,
  nowEpochMicros,
  parseDec,
  selfCheck,
  staleBudget,
  type AccrualRecipe,
} from './accrual';

interface Props {
  fixCid: string;
  actingAs?: string;
  /** Poll the ledger this often for the recipe + the live auction anchor. */
  refreshMs?: number;
}

/** 10fps: fast enough to read as motion, slow enough to be free. No network per frame. */
const TICK_MS = 100;

export default function AccrualTicker({ fixCid, actingAs, refreshMs = 20_000 }: Props) {
  const [snap, setSnap] = useState<AccruedNav | null>(null);
  const [err, setErr] = useState<string>('');
  // Offset between the DESK's clock and this browser's, measured at each read, so the
  // ticker advances on the desk's clock (which is the ledger's) rather than on a laptop
  // that happens to be four seconds fast.
  const clockOffset = useRef<bigint>(0n);
  const [nowMicros, setNowMicros] = useState<bigint>(() => nowEpochMicros());
  const [agree, setAgree] = useState<{ ok: boolean; server: string; local: string } | null>(null);

  // The browser's own arithmetic, checked against the ledger's pinned test vectors.
  const vectors = useMemo(() => selfCheck(), []);

  const load = useCallback(async () => {
    try {
      const s = await api.accruedNav(fixCid, { actingAs });
      clockOffset.current = BigInt(s.asOfEpochMicros) - BigInt(Date.now()) * 1000n;

      // THE AGREEMENT CHECK. Derive the value locally for the DESK'S OWN instant and
      // compare with what the desk derived. Same inputs, same instant, same arithmetic
      // -> the same ten digits, or this badge turns red.
      const local = navAt(
        {
          basePrice: parseDec(s.basePrice),
          ratePerAnnum: parseDec(s.ratePerAnnum),
          dayCount: s.dayCount,
          accrualFromEpochMicros: BigInt(s.accrualFromEpochMicros),
        },
        BigInt(s.asOfEpochMicros),
      );
      setAgree({
        ok: formatD(local) === formatD(parseDec(s.navNow)),
        server: formatD(parseDec(s.navNow)),
        local: formatD(local),
      });
      setSnap(s);
      setErr('');
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : e instanceof Error ? e.message : String(e));
    }
  }, [fixCid, actingAs]);

  useEffect(() => {
    void load();
    const id = window.setInterval(() => void load(), refreshMs);
    return () => window.clearInterval(id);
  }, [load, refreshMs]);

  useEffect(() => {
    const id = window.setInterval(() => setNowMicros(nowEpochMicros(clockOffset.current)), TICK_MS);
    return () => window.clearInterval(id);
  }, []);

  const recipe: AccrualRecipe | null = useMemo(() => {
    if (!snap) return null;
    return {
      basePrice: parseDec(snap.basePrice),
      ratePerAnnum: parseDec(snap.ratePerAnnum),
      dayCount: snap.dayCount,
      accrualFromEpochMicros: BigInt(snap.accrualFromEpochMicros),
    };
  }, [snap]);

  if (err) {
    return (
      <div className="warn" onClick={() => void load()}>
        {err} — click to retry
      </div>
    );
  }
  if (!snap || !recipe) {
    return <p className="hint">Reading the attested fix from the ledger…</p>;
  }

  // ---- everything below is DERIVED, live, from the four attested inputs ----
  const nav = navAt(recipe, nowMicros);
  const accrued = nav - recipe.basePrice;
  const elapsed = nowMicros - recipe.accrualFromEpochMicros;
  const perSecond = accruedAmount(recipe.basePrice, recipe.ratePerAnnum, snap.dayCount, 1_000_000n);
  const perDay = parseDec(snap.perDay);
  const yearDays = Number(BigInt(snap.yearMicros) / MICROS_PER_DAY);

  // Which decimal places move within a second — highlight exactly those, so the eye is
  // drawn to real motion rather than to a styling choice.
  const live = livePlaceFrom(perSecond);
  const shown = formatGrouped(nav, 10);
  const cut = live === null ? shown.length : shown.length - (10 - live + 1);
  const stable = shown.slice(0, cut);
  const moving = shown.slice(cut);

  const anchor = snap.anchor === null ? null : parseDec(snap.anchor);
  const anchorOk = anchor === null ? null : anchorConsistentWithNav(nav, anchor);
  const drift = anchor === null ? 0n : nav - anchor;
  const budget = staleBudget(nav);

  return (
    <div className="accrual" aria-label="Accrued NAV, derived on-ledger">
      <div className="accrual-head">
        <span className="accrual-tag">
          {snap.accruing ? 'ACCRUING · LEDGER-DERIVED' : 'SNAPSHOT · NON-ACCRUING'}
        </span>
        <span className="accrual-sub">
          {snap.instrumentId} {snap.session} · {snap.attestors.length}-of-{snap.threshold} attested
        </span>
      </div>

      {/* THE SHOT: the number, ticking. */}
      <div className="accrual-figure mono" aria-live="off">
        {stable}
        <span className="accrual-live">{moving}</span>
        <span className="accrual-unit">{snap.cashInstrument}</span>
      </div>
      <div className="accrual-delta mono">
        {accrued >= 0n ? '+' : '−'}
        {formatD(accrued < 0n ? -accrued : accrued, 10)} since the strike
      </div>

      {/* ITS WORKING, next to it. Nothing here is asserted; all of it is the input. */}
      <div className="accrual-working">
        <div className="wk">
          <span className="wk-k">Attested base</span>
          <span className="wk-v mono">
            {formatGrouped(recipe.basePrice, 4)} {snap.cashInstrument}
          </span>
        </div>
        <div className="wk">
          <span className="wk-k">Rate p.a.</span>
          <span className="wk-v mono">{pct(snap.ratePerAnnum)}</span>
        </div>
        <div className="wk">
          <span className="wk-k">Day count</span>
          <span className="wk-v mono">
            {snap.dayCount} <span className="faint">· {yearDays}d year</span>
          </span>
        </div>
        <div className="wk">
          <span className="wk-k">As of (attested)</span>
          <span className="wk-v mono">{shortIso(snap.accrualFrom)}</span>
        </div>
        <div className="wk">
          <span className="wk-k">Elapsed</span>
          <span className="wk-v mono">{humanElapsed(elapsed)}</span>
        </div>
        <div className="wk">
          <span className="wk-k">Accrues</span>
          <span className="wk-v mono">
            {formatD(perDay, 6)}/day <span className="faint">· {bps(perDay, recipe.basePrice)}</span>
          </span>
        </div>
      </div>

      <div className="accrual-formula mono">
        nav = base + base × (rate × elapsedμs) ÷ yearμs
        <span className="faint"> — multiply first, divide last (10dp, half-even)</span>
      </div>

      {/* THE AUCTION BINDING — bound, not asserted. */}
      <div className={`accrual-anchor ${anchorOk === null ? '' : anchorOk ? 'ok' : 'bad'}`}>
        <div className="wk">
          <span className="wk-k">Auction anchor</span>
          <span className="wk-v mono">
            {anchor === null ? '—' : `${formatGrouped(anchor, 4)} ${snap.cashInstrument}`}
          </span>
        </div>
        {anchor !== null && (
          <>
            <div className="wk">
              <span className="wk-k">Behind accrued NAV by</span>
              <span className="wk-v mono">
                {formatD(drift < 0n ? -drift : drift, 10)}
                {drift < 0n ? ' AHEAD' : ''}
              </span>
            </div>
            <div className="wk">
              <span className="wk-k">1bp staleness budget</span>
              <span className="wk-v mono">{formatD(budget, 10)}</span>
            </div>
          </>
        )}
        <div className="accrual-verdict">
          {anchor === null ? (
            <span className="faint">
              No live auction is anchored on this fix yet. Open one for {snap.instrumentId}{' '}
              {snap.session} and RunClose will check its anchor against this number.
            </span>
          ) : anchorOk ? (
            <span className="good">
              ✓ RunClose would accept this anchor — at or below the accrued NAV and within
              the 1bp budget.
            </span>
          ) : drift < 0n ? (
            <span className="bad-text">
              ✕ The anchor is AHEAD of the accrual — value the fund has not earned. RunClose
              rejects it outright; no elapsed time explains it.
            </span>
          ) : (
            <span className="bad-text">
              ✕ The anchor is more than 1bp behind — stale. RunClose refuses to print until it
              is re-published or a fresher fix is struck.
            </span>
          )}
        </div>
      </div>

      {/* PROVENANCE. Two independent implementations, both pinned to the Daml tests. */}
      <div className="accrual-proof">
        <span className={`proof ${vectors.ok ? 'ok' : 'bad'}`}>
          {vectors.ok ? '✓' : '✕'} browser arithmetic: {vectors.passed}/{vectors.total} ledger
          vectors
        </span>
        <span className={`proof ${agree?.ok ? 'ok' : agree ? 'bad' : ''}`}>
          {agree ? (agree.ok ? '✓ agrees with the desk' : `✕ diverged: ${agree.local} ≠ ${agree.server}`) : '…'}
        </span>
        <span className="proof">real time · no acceleration</span>
        <button className="ghost small" onClick={() => void load()}>
          Re-read from ledger
        </button>
      </div>
      {!vectors.ok && (
        <div className="warn">
          {vectors.failures.join(' · ')} — this browser is NOT reproducing the ledger's
          arithmetic. Do not trust the figure above.
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// display helpers — presentation only; no arithmetic that could disagree
// ---------------------------------------------------------------------------

/**
 * The first decimal place (1..10) that a single second of accrual reaches, or null if
 * this fix does not move at all. Used only to decide which digits to highlight.
 */
function livePlaceFrom(perSecond: bigint): number | null {
  const v = perSecond < 0n ? -perSecond : perSecond;
  if (v === 0n) return null;
  const digits = v.toString().length;      // scaled by 1e10
  const place = 10 - digits + 1;           // 1-based decimal place of its leading digit
  return Math.min(10, Math.max(1, place));
}

/** A rate as a percentage, from the ledger's own digits (display only). */
function pct(rate: string): string {
  // rate is a fraction (0.036); a percentage is the same value times 100, which in the
  // fixed-point representation is just a factor of 100 on the scaled integer.
  const s = formatD(parseDec(rate) * 100n, 6).replace(/0+$/, '').replace(/\.$/, '');
  return `${s}%`;
}

/** How much of the base a day's accrual is, in basis points (display only). */
function bps(perDay: bigint, base: bigint): string {
  if (base === 0n) return '—';
  const b = (perDay * 10000n * 100n) / base;   // hundredths of a bp
  return `${(Number(b) / 100).toFixed(2)} bp/day`;
}

function shortIso(iso: string): string {
  return iso.replace('T', ' ').replace(/\.\d+/, '');
}

function humanElapsed(micros: bigint): string {
  if (micros <= 0n) return 'not yet — the mark applies from a future instant';
  const totalSec = Number(micros / 1_000_000n);
  const d = Math.floor(totalSec / 86400);
  const h = Math.floor((totalSec % 86400) / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  const parts: string[] = [];
  if (d) parts.push(`${d}d`);
  if (d || h) parts.push(`${h}h`);
  if (d || h || m) parts.push(`${m}m`);
  parts.push(`${s}s`);
  return `${parts.join(' ')} · ${micros.toString()}μs`;
}
