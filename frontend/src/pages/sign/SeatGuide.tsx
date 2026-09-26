// Signer portal · "Your seat" — the collapsible guide on the signer's first screen.
//
// One screen answers: what my seat asserts every day, which conditions and evidence that
// means (from /api/signer-protocol for the instrument in view, so the wording is what the
// confirm route accepts), what happens when I refuse, how to automate it, and which trust
// level I sign at. Prose that the API does not carry comes from src/seatGuide.ts; see
// ONBOARDING.md beside this file for where each sentence originates.
import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { useAsync } from '../../components/ui';
import {
  DEFAULT_TRUST_LEVEL, exampleConfirmBody, fetchSeatProtocol, seatGuideText, TRUST_LADDER,
  type GuideCondition, type GuideRole,
} from '../../seatGuide';

// v2 because the DEFAULT changed, and the old key holds an explicit '1' for everyone who
// ever loaded the page — leaving it would pin the guide open forever for exactly the
// people who have already read it.
const OPEN_KEY = 'sign.seatGuide.open.v2';

/**
 * CLOSED BY DEFAULT, opened once and remembered.
 *
 * This used to default open, so every signer met ~190 lines of reference — what the seat
 * asserts, a TradFi analogue, the failure policy, a 25-line signer.yml skeleton, a 20-line
 * example request body and the L1/L2/L3 trust ladder — ABOVE the thing they came to do.
 * On a day with a proposal waiting they scrolled past all of it; on a day without one they
 * scrolled past all of it to reach "nothing waiting for your signature".
 *
 * The material is not filler: a seat that does not understand what it is asserting is worse
 * than no seat. But it is read once and referred to occasionally, and the job is done daily
 * in thirty seconds. Daily work goes first; reference opens on request and stays open for
 * whoever wants it there.
 */
function readOpen(): boolean {
  try { return localStorage.getItem(OPEN_KEY) === '1'; } catch { return false; }
}
function writeOpen(v: boolean): void {
  try { localStorage.setItem(OPEN_KEY, v ? '1' : '0'); } catch { /* per-viewer convenience only */ }
}

const fmtExample = (v: string | number) => (typeof v === 'string' ? `"${v}"` : String(v));

function ConditionRow({ c, example }: { c: GuideCondition; example?: Record<string, string | number> }) {
  const ev = c.evidence;
  const fields = ev?.fields ?? [];
  return (
    <li className="check" style={{ cursor: 'default', gridTemplateColumns: '1fr' }}>
      <div>
        <span className="check-name mono">{c.name}</span>
        {ev && (
          <span className="tag" style={{ marginLeft: 8 }}>
            {ev.required ? `checked by ${ev.verifiedBy}` : 'recorded, not checked'}
          </span>
        )}
      </div>
      <span className="check-when">{c.passesWhen}</span>
      {fields.length > 0 && (
        <dl className="kv" style={{ margin: '4px 0 0' }}>
          {fields.map((f) => (
            <div key={f.name} style={{ display: 'contents' }}>
              <dt className="mono">{f.name} <span className="muted">({f.type})</span></dt>
              <dd style={{ textAlign: 'left' }}>
                <span className="muted">{f.description}</span>
                {example && example[f.name] !== undefined && (
                  <span className="mono" style={{ marginLeft: 8 }}>e.g. {fmtExample(example[f.name])}</span>
                )}
              </dd>
            </div>
          ))}
        </dl>
      )}
      {ev?.rule && <span className="hint subtle mono" style={{ margin: '4px 0 0' }}>rule: {ev.rule}</span>}
    </li>
  );
}

export default function SeatGuide() {
  const { me } = useAuth();
  const seat = me?.seat ? String(me.seat) : null;
  const instruments = me?.instruments ?? [];
  const [open, setOpen] = useState<boolean>(() => readOpen());
  const [instrument, setInstrument] = useState<string>(instruments[0] ?? '');
  useEffect(() => { if (!instrument && instruments[0]) setInstrument(instruments[0]); }, [instrument, instruments]);

  const proto = useAsync(() => fetchSeatProtocol(instrument || undefined), [instrument]);
  const role: GuideRole | null = proto.data?.roles.find((r) => r.key === seat) ?? null;
  const text = seatGuideText(seat);

  const toggle = () => setOpen((o) => { writeOpen(!o); return !o; });

  if (!seat) return null;

  return (
    // `reference` (DESIGN.md §5): dashed, transparent, quieter than a working card — it
    // reads as the manual sitting under the work rather than as another task.
    <section className={`card${open ? '' : ' reference'}`} aria-labelledby="seat-guide-h">
      <div className="card-head">
        <h2 id="seat-guide-h">
          Your seat{role ? ` · ${role.title}` : ` · ${seat}`}
          {!open && <span className="hint" style={{ marginLeft: 10, fontWeight: 400 }}>
            what you assert, what happens when a check fails, and how to automate it
          </span>}
        </h2>
        <button type="button" className="ghost small" aria-expanded={open} aria-controls="seat-guide-body" onClick={toggle}>
          {open ? 'Hide guide' : 'Show guide'}
        </button>
      </div>
      <p className="hint" style={{ marginBottom: open ? undefined : 0 }}>
        {text ? <><strong>Every day you assert:</strong> {text.assertion}</> : <>Your seat asserts a fact only it can see — never an opinion about the price.</>}
        {role?.uniquelyKnows ? <> <span className="muted">Only your seat knows: {role.uniquelyKnows.toLowerCase()}.</span></> : null}
      </p>

      {open && (
        <div id="seat-guide-body">
          {text && (
            <p className="hint subtle">
              <strong>TradFi analogue.</strong> {text.analogue} You act for your firm on the fact you attest, not as an independent individual — that is the deliberate difference from a regulated oversight committee.
            </p>
          )}

          <h2 className="section-h">Conditions and evidence{instrument ? ` · ${instrument}` : ''}</h2>
          {instruments.length > 1 && (
            <div className="proposal-actions" role="tablist" aria-label="Instrument">
              {instruments.map((i) => (
                <button key={i} type="button" role="tab" aria-selected={i === instrument}
                  className={`ghost small${i === instrument ? ' on' : ''}`} onClick={() => setInstrument(i)}>{i}</button>
              ))}
            </div>
          )}
          {proto.loading && <div className="empty" role="status">Loading the signer protocol…</div>}
          {proto.error && <div className="banner warn" role="status"><span>Signer protocol not loaded — {proto.error}.</span></div>}
          {proto.data && !role && (
            <div className="banner warn" role="status"><span>The protocol {proto.data.version} has no seat named <code>{seat}</code>. Ask the administrator to check the roster.</span></div>
          )}
          {role && (
            <>
              <p className="hint subtle">
                Protocol <span className="mono">{proto.data?.version}</span>. Each condition needs the numbers behind it; a tick alone is refused
                {role.requiresObservedRange ? ' for every seat except the venue, whose traded range is checked by the ledger itself' : ''}.
                A missing block or a failing number comes back as a 422 naming the number.
              </p>
              <ul className="checklist" style={{ listStyle: 'none', margin: '0 0 12px' }}>
                {role.conditions.map((c) => <ConditionRow key={c.name} c={c} example={text?.examples[c.name]} />)}
              </ul>
              <p className="hint subtle">Example values are illustrative. The point of the seat is that the real ones come from your systems — somewhere ETP Foundry cannot see.</p>
            </>
          )}

          <h2 className="section-h">When a condition fails</h2>
          <ul className="hint" style={{ margin: '0 0 12px', paddingLeft: 18 }}>
            {(text?.refuse ?? ['Refuse, naming the condition that failed.']).map((line, i) => <li key={i}>{line}</li>)}
            <li>In the portal: <strong>Refuse with reason</strong> on the proposal card, choosing the condition. A refusal without a reason is not recorded.</li>
            <li>If fewer than K seats confirm, no fixing exists for the day: the gap is published as a gap, and nothing settles against a guess.</li>
            <li>A checker that <em>cannot evaluate</em> a condition halts and sends nothing — a halt is never a confirm, and never a refusal either.</li>
          </ul>

          <h2 className="section-h">Automate this</h2>
          <p className="hint subtle">
            Every condition is a query against your own systems; none needs a human to form a view. An unpaid seat that needs daily attention
            becomes a rubber stamp within weeks, so run a checker that confirms with evidence when every condition passes, refuses naming the
            condition when one fails, and halts when one cannot be evaluated. It must never widen a tolerance to make a check pass.
          </p>
          <ol className="hint" style={{ margin: '0 0 10px', paddingLeft: 18 }}>
            <li>Generate an API key under <strong>Settings → API key</strong>. It is shown once and stored hashed; it can only confirm or refuse fixings for your instruments.</li>
            <li>Clone <span className="mono">signer-service/</span> from the repository, then <span className="mono">npm ci &amp;&amp; npm run build</span> (Node 20+) or <span className="mono">docker build -t crossdesk/signer .</span></li>
            <li>Write <span className="mono">signer.yml</span> — start from the skeleton below and point each field at your system (<span className="mono">static</span>, <span className="mono">http</span> or <span className="mono">command</span> sources).</li>
            <li>Check it without acting: <span className="mono">node dist/index.js --config signer.yml --check</span>. Then run it; it polls every 30 s and serves <span className="mono">POST /webhook</span> and <span className="mono">GET /health</span> on :8787.</li>
          </ol>
          {text && (
            <>
              {!text.checkerShipped && (
                <div className="banner warn" role="status">
                  <span>The reference checker ships seat rules for issuer, lender and venue today. Your seat sends the same confirm body to the same route; the skeleton shows the fields.</span>
                </div>
              )}
              <code className="apikey-value mono" style={{ whiteSpace: 'pre', overflowX: 'auto', display: 'block', fontSize: 12 }}>{text.quickstart}</code>
            </>
          )}
          {role && (
            <>
              <p className="hint subtle" style={{ marginTop: 10 }}>
                What the checker sends on a pass — <span className="mono">POST /api/proposals/{'{cid}'}/confirm</span> with <span className="mono">Authorization: Bearer ck_…</span>:
              </p>
              <code className="apikey-value mono" style={{ whiteSpace: 'pre', overflowX: 'auto', display: 'block', fontSize: 12 }}>{exampleConfirmBody(role, text)}</code>
            </>
          )}

          <h2 className="section-h">Trust level</h2>
          <p className="hint subtle">
            A signature is worth exactly what it costs to forge. Every published value states the level each signature was made at
            (e.g. <span className="mono">K=3 of N=5 — L1:2, L2:1</span>). You are at <strong>{DEFAULT_TRUST_LEVEL}</strong>: honest for a pilot, not
            acceptable for an OFFICIAL fixing a third party settles against. Target: every signer at L2 or above before that happens.
          </p>
          <div className="stat-row">
            {TRUST_LADDER.map((t) => (
              <div key={t.level} className="stat" style={t.level === DEFAULT_TRUST_LEVEL ? { borderColor: 'var(--accent)' } : undefined}>
                <span className="stat-label">{t.level} · {t.name}{t.level === DEFAULT_TRUST_LEVEL ? ' · you are here' : ''}</span>
                <span className="stat-sub"><strong>Key:</strong> {t.key}</span>
                <span className="stat-sub"><strong>Administrator could forge?</strong> {t.forgeable}</span>
                <span className="stat-sub"><strong>Your cost:</strong> {t.cost}</span>
                <span className="stat-sub"><strong>Requires:</strong> {t.requires}</span>
              </div>
            ))}
          </div>
          {/* This used to read "To move up: L2 — generate an API key and run the checker…",
              which is false in the way that matters. The key authenticates a request to US;
              we still exercise the choice as your party with our own ledger token. A checker
              automates your side, it does not move the signing key. L2 and L3 are the
              destination, not an option you can take today. */}
          <p className="hint subtle" style={{ marginTop: 10 }}>
            <strong>Only L1 exists today.</strong> An API key and a checker automate your side of the
            work and are worth doing — but they do not move you off L1, because the desk still
            exercises the choice as your party. L2 needs Canton interactive submission and L3 needs
            your own participant; neither is built. We will tell you the day that changes rather
            than let you assume it already has.
          </p>
          <p className="hint subtle">
            Disclosed at onboarding: the administrator can assume a mapped user's identity for a single request (support). Every such act is recorded and it is never used to confirm a fixing on your behalf.
          </p>
        </div>
      )}
    </section>
  );
}
