// A small SVG line chart — no library. Built for a benchmark/NAV series: one line,
// official values in gold, a restated or fallback-tier point drawn distinctly, a hover
// readout, first/last date on the axis, min/max on the side.
//
// A series can carry rows WITHOUT a value: a tier-5 miss is published as a gap (§4), so
// `y` may be undefined/NaN. Those rows keep their slot on the x axis (the gap is visible)
// but never enter the geometry — a NaN in a path attribute is a console error and a blank
// chart, and "low NaN" is not a readout.
import { useId, useMemo, useState } from 'react';

export interface ChartPoint {
  x: string;        // label (date)
  y?: number | null;
  /** 1 = attested by K; anything else is drawn hollow so it never passes for official. */
  tier?: number;
  /** The backend's own word for the tier ("seed", "missed"), when it sends one. */
  tierLabel?: string;
  restated?: boolean;
}

interface Props {
  points: ChartPoint[];          // oldest → newest
  height?: number;
  formatY?: (n: number) => string;
  ariaLabel: string;
}

const W = 640;                   // viewBox width; the SVG scales to its container
const PAD = { top: 14, right: 14, bottom: 24, left: 8 };

const has = (p: ChartPoint): p is ChartPoint & { y: number } => typeof p.y === 'number' && Number.isFinite(p.y);

export default function LineChart({ points, height = 200, formatY, ariaLabel }: Props) {
  const [hover, setHover] = useState<number | null>(null);
  const gid = useId();
  const fmt = formatY ?? ((n: number) => n.toLocaleString(undefined, { maximumFractionDigits: 2 }));

  const geo = useMemo(() => {
    const ys = points.filter(has).map((p) => p.y);
    if (ys.length === 0) return null;
    const min = Math.min(...ys);
    const max = Math.max(...ys);
    const span = max - min || Math.abs(max) * 0.02 || 1;
    const lo = min - span * 0.08;
    const hi = max + span * 0.08;
    const iw = W - PAD.left - PAD.right;
    const ih = height - PAD.top - PAD.bottom;
    const xs = points.map((_, i) => PAD.left + (points.length === 1 ? iw / 2 : (i / (points.length - 1)) * iw));
    const yOf = (v: number) => PAD.top + ih - ((v - lo) / (hi - lo)) * ih;
    // One sub-path per run of valued points: a gap in the series is a gap in the line.
    const segments: string[] = [];
    let seg: string[] = [];
    points.forEach((p, i) => {
      if (has(p)) {
        seg.push(`${seg.length === 0 ? 'M' : 'L'}${xs[i].toFixed(1)},${yOf(p.y).toFixed(1)}`);
      } else if (seg.length) { segments.push(seg.join(' ')); seg = []; }
    });
    if (seg.length) segments.push(seg.join(' '));
    const path = segments.join(' ');
    const valued = points.map((p, i) => (has(p) ? i : -1)).filter((i) => i >= 0);
    const first = valued[0]; const last = valued[valued.length - 1];
    const base = (PAD.top + ih).toFixed(1);
    const area = `${path} L${xs[last].toFixed(1)},${base} L${xs[first].toFixed(1)},${base} Z`;
    return { xs, yOf, path, area, min, max, ih, gaps: points.length - ys.length };
  }, [points, height]);

  if (points.length === 0) {
    return <div className="chart-empty">No series yet.</div>;
  }
  if (!geo) {
    return <div className="chart-empty">No published value in this series yet — {points.length === 1 ? 'the one fixing' : `all ${points.length} fixings`} missed.</div>;
  }

  const nearest = (clientX: number, el: SVGSVGElement) => {
    const rect = el.getBoundingClientRect();
    const x = ((clientX - rect.left) / rect.width) * W;
    let best = 0;
    let dist = Infinity;
    geo.xs.forEach((px, i) => { const d = Math.abs(px - x); if (d < dist) { dist = d; best = i; } });
    setHover(best);
  };

  const h = hover !== null ? points[hover] : null;
  const hx = hover !== null ? geo.xs[hover] : 0;
  const tierWord = (p: ChartPoint) => p.tierLabel ? `tier ${p.tier} · ${p.tierLabel}` : `tier ${p.tier}`;

  return (
    <div className="chart">
      <div className="chart-readout mono" aria-live="polite">
        {h ? (
          <>
            <span className="chart-readout-date">{h.x}</span>
            {has(h)
              ? <span className={h.tier === 1 ? 'official' : ''}>{fmt(h.y)}</span>
              : <span className="muted">no value published</span>}
            {h.tier !== undefined && h.tier !== 1 && <span className="tag fallback">{tierWord(h)}</span>}
            {h.restated && <span className="tag">restated</span>}
          </>
        ) : (
          <>
            <span className="muted">low {fmt(geo.min)}</span>
            <span className="muted">high {fmt(geo.max)}</span>
            {geo.gaps > 0 && <span className="muted">{geo.gaps} missed</span>}
          </>
        )}
      </div>
      <svg
        viewBox={`0 0 ${W} ${height}`}
        className="chart-svg"
        role="img"
        aria-label={ariaLabel}
        onMouseMove={(e) => nearest(e.clientX, e.currentTarget)}
        onMouseLeave={() => setHover(null)}
        onTouchStart={(e) => { const t = e.touches[0]; if (t) nearest(t.clientX, e.currentTarget); }}
      >
        <defs>
          <linearGradient id={`${gid}-fill`} x1="0" x2="0" y1="0" y2="1">
            <stop offset="0" stopColor="#E6B450" stopOpacity="0.22" />
            <stop offset="1" stopColor="#E6B450" stopOpacity="0" />
          </linearGradient>
        </defs>
        {[0.25, 0.5, 0.75].map((f) => (
          <line key={f} x1={PAD.left} x2={W - PAD.right} y1={PAD.top + geo.ih * f} y2={PAD.top + geo.ih * f}
            stroke="rgba(255,255,255,0.05)" strokeWidth="1" />
        ))}
        <path d={geo.area} fill={`url(#${gid}-fill)`} />
        <path d={geo.path} fill="none" stroke="#E6B450" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
        {points.map((p, i) => {
          if (!has(p)) {
            // A gap: a hollow marker on the baseline so the miss is seen, not hidden.
            return (
              <g key={i}>
                <line x1={geo.xs[i]} x2={geo.xs[i]} y1={PAD.top} y2={PAD.top + geo.ih} stroke="rgba(198,91,91,0.25)" strokeWidth="1" strokeDasharray="2 4" />
                <circle cx={geo.xs[i]} cy={PAD.top + geo.ih} r={i === hover ? 4.5 : 3.5} fill="#12141A" stroke="#C65B5B" strokeWidth="1.5" />
              </g>
            );
          }
          const off = (p.tier !== undefined && p.tier !== 1) || p.restated;
          if (!off && i !== hover) return null;
          return (
            <circle key={i} cx={geo.xs[i]} cy={geo.yOf(p.y)} r={i === hover ? 4.5 : 3.5}
              fill={off ? '#12141A' : '#E6B450'} stroke={off ? '#8A909C' : '#E6B450'} strokeWidth="1.5" />
          );
        })}
        {h && (
          <line x1={hx} x2={hx} y1={PAD.top} y2={PAD.top + geo.ih} stroke="rgba(232,234,237,0.25)" strokeWidth="1" strokeDasharray="3 3" />
        )}
        <text x={PAD.left} y={height - 6} fill="#5B616D" fontSize="10" fontFamily="JetBrains Mono, monospace">{points[0].x}</text>
        <text x={W - PAD.right} y={height - 6} fill="#5B616D" fontSize="10" textAnchor="end" fontFamily="JetBrains Mono, monospace">
          {points[points.length - 1].x}
        </text>
      </svg>
    </div>
  );
}
