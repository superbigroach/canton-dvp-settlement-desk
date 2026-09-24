// One proposal, as the seat sees it: what is proposed and how it was built, how long is
// left, the named conditions THIS seat verifies, the evidence each one needs (one input
// per field, from /api/signer-protocol for THIS instrument), Confirm / Refuse, and the
// message log. Optimistic: the card moves the moment you act and moves back, with the
// backend's sentence, if the ledger says no.
//
// THE WIRE. POST /api/proposals/{cid}/confirm takes `{ checks: [names], evidence }`
// (ProposalController.ConfirmRequest). For the venue the server reads `evidence.low` /
// `evidence.high` and the ledger checks the range; for every other seat it reads
// `evidence[<condition>][<field>]` and SignerEvidence.verify applies the rule before the
// submit. A tick without numbers is a 422, and the 422's message is shown verbatim.
import { useMemo, useState } from 'react';
import type { SignerRole } from '../../api';
import { ApiError, errorMessage } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import { desk, type ConfirmBody, type Proposal, type ProposalEvent } from '../../desk';
import { Countdown, fmtN, fmtQty, fmtTs, fmtTime, NumberField, shortCid, useAsync } from '../../components/ui';
import { fetchSeatProtocol, type EvidenceField, type GuideCondition } from '../../seatGuide';

interface Props {
  proposal: Proposal;
  role: SignerRole | null;          // my seat's protocol entry (names + passesWhen), unqualified by instrument
  onChanged: (next: Proposal) => void;
  /** Re-read the list from the backend — after the ledger says no, the card is stale. */
  onRefresh?: () => void;
  readOnly?: boolean;
}

const NO_PRINTS = 'no-prints-attested';
const TRADED_RANGE = 'traded-range';

/** condition → field → raw input text. */
type Inputs = Record<string, Record<string, string>>;

/** Parse one field the way the server will (SignerEvidence.number / instant), or null. */
function parseField(f: EvidenceField, raw: string): number | string | null {
  const v = raw.trim();
  if (!v) return null;
  if (f.type === 'instant') {
    const t = Date.parse(v);
    if (Number.isNaN(t)) return null;
    return new Date(t).toISOString();
  }
  const n = Number(v);
  if (!Number.isFinite(n)) return null;
  if (f.type === 'integer' && !Number.isInteger(n)) return null;
  return n;
}

export default function ProposalCard({ proposal: p, role, onChanged, onRefresh, readOnly }: Props) {
  const { me } = useAuth();
  const seatKey = (me?.seat ? String(me.seat) : role?.key ?? '').toLowerCase();

  // The seat as it applies to THIS instrument: an issuer on an on-chain-verifiable asset
  // is shown three conditions, not four, because the API said so.
  const proto = useAsync(() => fetchSeatProtocol(p.instrument), [p.instrument]);
  const instRole = proto.data?.roles.find((r) => r.key === seatKey) ?? null;

  const conditions: GuideCondition[] = useMemo(() => {
    if (instRole) return instRole.conditions;
    if (role) return role.conditions.map((c) => ({ ...c }));
    return p.conditions.map((name) => ({ name, passesWhen: '' }));
  }, [instRole, role, p.conditions]);

  const needsRange = p.requiresObservedRange || instRole?.requiresObservedRange || role?.requiresObservedRange || false;
  const isVenue = needsRange;
  const noPrintsCondition = conditions.find((c) => c.name === NO_PRINTS) ?? null;

  const [checks, setChecks] = useState<string[]>([]);
  const [inputs, setInputs] = useState<Inputs>({});
  const [low, setLow] = useState('');
  const [high, setHigh] = useState('');
  const [noPrints, setNoPrints] = useState(false);
  const [refusing, setRefusing] = useState(false);
  const [refuseCondition, setRefuseCondition] = useState(conditions[0]?.name ?? '');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState<'confirm' | 'refuse' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorHint, setErrorHint] = useState<string | null>(null);
  const [showLog, setShowLog] = useState(false);

  const log = useAsync<ProposalEvent[]>(() => (showLog ? desk.proposalEvents(p.cid) : Promise.resolve([])), [showLog, p.cid, p.mine?.at]);

  const open = p.status === 'open' && !p.mine;
  const deadlinePassed = new Date(p.deadline).getTime() < Date.now();

  // What is on screen. The venue's thin-market toggle swaps the whole claim: either the
  // book traded (range + the other venue conditions) or it did not (no-prints only) —
  // the server refuses both on one proposal.
  const visibleConditions = isVenue
    ? (noPrints ? conditions.filter((c) => c.name === NO_PRINTS) : conditions.filter((c) => c.name !== NO_PRINTS))
    : conditions;
  const effectiveChecks = isVenue && noPrints ? [NO_PRINTS] : checks.filter((n) => visibleConditions.some((c) => c.name === n));

  const setField = (cond: string, field: string, v: string) =>
    setInputs((prev) => ({ ...prev, [cond]: { ...(prev[cond] ?? {}), [field]: v } }));

  const toggle = (name: string) =>
    setChecks((c) => (c.includes(name) ? c.filter((x) => x !== name) : [...c, name]));

  // Which conditions need typed inputs on this card. The venue's traded range keeps its
  // own two inputs (the ledger's check), so its fields are not rendered generically.
  const fieldsFor = (c: GuideCondition): EvidenceField[] => {
    if (!c.evidence?.required) return [];
    if (isVenue && c.name === TRADED_RANGE) return [];
    return c.evidence.fields ?? [];
  };

  // ---- completeness: the first missing or malformed field, named ----------------------
  const lowN = Number(low); const highN = Number(high);
  const rangeNeeded = isVenue && !noPrints;
  const rangeOk = !rangeNeeded || (low !== '' && high !== '' && Number.isFinite(lowN) && Number.isFinite(highN) && lowN <= highN);
  const rangeContains = !rangeNeeded || (rangeOk && p.price >= lowN && p.price <= highN);

  let missing: string | null = null;
  if (rangeNeeded && (low === '' || high === '')) missing = low === '' ? 'traded low' : 'traded high';
  else if (rangeNeeded && !rangeOk) missing = 'a range with low ≤ high';
  if (!missing) {
    for (const name of effectiveChecks) {
      const c = conditions.find((x) => x.name === name);
      if (!c) continue;
      for (const f of fieldsFor(c)) {
        const raw = inputs[name]?.[f.name] ?? '';
        if (parseField(f, raw) === null) { missing = `${name}.${f.name}${raw.trim() ? ` (not a valid ${f.type})` : ''}`; break; }
      }
      if (missing) break;
    }
  }
  if (!missing && effectiveChecks.length === 0) missing = 'at least one condition';
  const canConfirm = open && !readOnly && !missing && busy === null;

  // ---- the body, exactly as the API accepts it ------------------------------------------
  const buildBody = (): { checks: string[]; evidence: Record<string, unknown> } => {
    const evidence: Record<string, unknown> = {};
    for (const name of effectiveChecks) {
      const c = conditions.find((x) => x.name === name);
      if (!c) continue;
      const block: Record<string, number | string> = {};
      for (const f of fieldsFor(c)) {
        const v = parseField(f, inputs[name]?.[f.name] ?? '');
        if (v !== null) block[f.name] = v;
      }
      if (Object.keys(block).length) evidence[name] = block;
    }
    if (rangeNeeded) {
      // Top-level low/high is what the server reads and the ledger checks; the nested
      // block mirrors what the reference checker sends.
      evidence.low = lowN;
      evidence.high = highN;
      if (effectiveChecks.includes(TRADED_RANGE)) evidence[TRADED_RANGE] = { low: lowN, high: highN };
    }
    return { checks: effectiveChecks, evidence };
  };

  const fail = (e: unknown) => {
    if (e instanceof ApiError && e.status === 422) {
      // The server's refusal names the number that failed; show it as it came.
      setError(`Refused (422): ${e.message}`);
      setErrorHint(e.hint ?? null);
    } else {
      setError(errorMessage(e));
      setErrorHint(e instanceof ApiError ? e.hint ?? null : null);
    }
  };

  const confirm = async () => {
    setBusy('confirm'); setError(null); setErrorHint(null);
    const body = buildBody();
    const optimistic: Proposal = {
      ...p, confirmed: [...p.confirmed, seatKey || 'me'],
      mine: { action: 'confirmed', at: new Date().toISOString(), checks: body.checks, evidence: rangeNeeded ? { low: lowN, high: highN } : undefined },
    };
    onChanged(optimistic);
    try {
      const r = await desk.confirm(p.cid, body as unknown as ConfirmBody);
      const cid = r?.cid || r?.contractId;
      onChanged({ ...optimistic, mine: { ...optimistic.mine!, cid: cid || undefined },
        status: optimistic.confirmed.length >= p.k ? 'finalized' : optimistic.status });
    } catch (e) {
      onChanged(p);
      fail(e);
      // "already attested", "outside the window": the ledger knows more than this card did.
      onRefresh?.();
    } finally {
      setBusy(null);
    }
  };

  const refuse = async () => {
    if (!reason.trim()) { setError('Say why — a refusal without a reason is not recorded.'); return; }
    setBusy('refuse'); setError(null); setErrorHint(null);
    const optimistic: Proposal = { ...p, status: 'refused', mine: { action: 'refused', at: new Date().toISOString(), reason } };
    onChanged(optimistic);
    try {
      const r = await desk.refuse(p.cid, { condition: refuseCondition, reason: reason.trim() });
      const cid = r?.cid || r?.contractId;
      onChanged({ ...optimistic, mine: { ...optimistic.mine!, cid: cid || undefined } });
      setRefusing(false);
    } catch (e) {
      onChanged(p);
      fail(e);
      onRefresh?.();
    } finally {
      setBusy(null);
    }
  };

  const renderField = (cond: string, f: EvidenceField) => {
    const id = `ev-${p.cid}-${cond}-${f.name}`;
    const raw = inputs[cond]?.[f.name] ?? '';
    const bad = raw.trim() !== '' && parseField(f, raw) === null;
    if (f.type === 'instant') {
      return (
        <label key={f.name} className="field grow" htmlFor={id}>
          <span>{f.description}</span>
          <div className="row tight" style={{ margin: 0 }}>
            <input id={id} className="mono" type="text" value={raw} placeholder="2026-09-24T16:00:00Z"
              disabled={busy !== null} onChange={(e) => setField(cond, f.name, e.target.value)} style={{ flex: 1 }} />
            <button type="button" className="ghost small" disabled={busy !== null}
              onClick={() => setField(cond, f.name, new Date().toISOString())}>now</button>
          </div>
          <small className={`field-hint${bad ? ' bad-text' : ''}`}>{f.name} · ISO-8601{bad ? ' — not a valid timestamp' : ''}</small>
        </label>
      );
    }
    return (
      <label key={f.name} className="field" htmlFor={id}>
        <span>{f.description}</span>
        <input id={id} className="mono" type="number" inputMode={f.type === 'integer' ? 'numeric' : 'decimal'}
          step={f.type === 'integer' ? 1 : 'any'} value={raw} disabled={busy !== null}
          onChange={(e) => setField(cond, f.name, e.target.value)} />
        <small className={`field-hint${bad ? ' bad-text' : ''}`}>{f.name} · {f.type}{bad ? ` — not a valid ${f.type}` : ''}</small>
      </label>
    );
  };

  return (
    <article className={`card proposal ${p.status}`} aria-labelledby={`p-${p.cid}`}>
      <div className="card-head">
        <h2 id={`p-${p.cid}`}>
          {p.instrument}{p.session ? ` · ${p.session}` : ''}
          <span className="tag kind">{p.kind === 'nav' ? 'fund NAV' : p.kind === 'wrapped' ? 'benchmark × factor' : 'snapshot'}</span>
        </h2>
        <div className="proposal-meta mono">
          {p.status === 'open' ? <Countdown to={p.deadline} /> : <span className={`tag status ${p.status}`}>{p.status}</span>}
          <span className="muted">
            {p.confirmed.length >= p.k
              ? `${p.confirmed.length} signed · ${p.k} needed`
              : `${p.confirmed.length} of ${p.k} needed`} · {p.n} seats
          </span>
        </div>
      </div>

      <div className="proposal-price">
        <span className="proposal-label">proposed</span>
        <span className="proposal-value mono">{fmtN(p.price)}</span>
        {p.kind === 'wrapped' && p.referencePrice !== undefined && p.wrapperFactor !== undefined && (
          <span className="proposal-build mono muted">
            = {fmtN(p.referencePrice)} benchmark × {p.wrapperFactor} factor
          </span>
        )}
        {p.kind === 'nav' && p.navComponents && p.navComponents.length > 0 && (
          <span className="proposal-build mono muted">
            = Σ {p.navComponents.map((c) => `${fmtQty(c.unitsPerShare)} ${c.instrumentId} × ${fmtN(c.mark)}`).join(' + ')}
          </span>
        )}
      </div>
      {p.rationale && <p className="hint subtle proposal-rationale">{p.rationale}</p>}
      <div className="proposal-facts mono muted">
        <span>proposed {fmtTs(p.proposedAt)}{p.proposedBy ? ` by ${p.proposedBy}` : ''}</span>
        <span>window ends {fmtTime(p.deadline)}</span>
        <span>cid {shortCid(p.cid)}</span>
        {p.confirmed.length > 0 && <span>attested: {p.confirmed.join(', ')}</span>}
      </div>

      {(p.refusals ?? []).filter((r) => !(p.mine?.action === 'refused' && r.seat && r.seat === seatKey)).map((r, i) => (
        <div key={i} className="banner warn" role="status">
          <span>
            <strong>{r.actor ?? r.seat ?? 'a seat'}</strong> refused{r.condition ? <> on <code>{r.condition}</code></> : ''}
            {r.reason ? ` — ${r.reason}` : ''}{r.ts ? ` · ${fmtTime(r.ts)}` : ''}
          </span>
        </div>
      ))}
      {p.mine && (
        <div className={`banner ${p.mine.action === 'confirmed' ? 'ok' : 'warn'}`} role="status">
          <span>
            You {p.mine.action} this at {fmtTime(p.mine.at)}
            {p.mine.checks?.length ? ` — verified ${p.mine.checks.join(', ')}` : ''}
            {p.mine.evidence ? ` — range ${fmtN(p.mine.evidence.low)}–${fmtN(p.mine.evidence.high)}` : ''}
            {p.mine.reason ? ` — ${p.mine.reason}` : ''}
            {p.mine.cid ? ` · cid ${shortCid(p.mine.cid)}` : ''}
          </span>
        </div>
      )}

      {open && !readOnly && (
        <>
          {proto.error && (
            <div className="banner warn" role="status">
              <span>Signer protocol for {p.instrument} not loaded — {proto.error}. Evidence fields may be missing; the server will say what it needs.</span>
            </div>
          )}

          {isVenue && noPrintsCondition && (
            <label className={`check${noPrints ? ' on' : ''}`} style={{ borderBottom: 'none', padding: '4px 4px 10px' }}>
              <input type="checkbox" checked={noPrints} disabled={busy !== null}
                onChange={() => { setNoPrints((v) => !v); setError(null); }} />
              <span className="check-name mono">no prints in the window</span>
              <span className="check-when">Your book showed no trades: attest the absence with your best bid/ask instead of a range. Never both.</span>
            </label>
          )}

          <fieldset className="checklist">
            <legend>Conditions your seat verifies{instRole ? ` (${instRole.title})` : role ? ` (${role.title})` : ''}{proto.data ? ` · ${proto.data.version}` : ''}</legend>
            {visibleConditions.length === 0 && (
              <p className="hint subtle">No conditions are defined for your seat on this instrument — the signer protocol may not be loaded.</p>
            )}
            {visibleConditions.map((c) => {
              const on = effectiveChecks.includes(c.name);
              const forced = isVenue && noPrints && c.name === NO_PRINTS;
              const fields = fieldsFor(c);
              return (
                <div key={c.name}>
                  <label className={`check${on ? ' on' : ''}`}>
                    <input type="checkbox" checked={on} onChange={() => !forced && toggle(c.name)} disabled={busy !== null || forced} />
                    <span className="check-name mono">{c.name}</span>
                    {c.passesWhen && <span className="check-when">{c.passesWhen}</span>}
                  </label>
                  {on && fields.length > 0 && (
                    <div className="evidence" style={{ padding: '0 4px 8px 28px' }}>
                      <div className="row tight" style={{ marginBottom: 4 }}>
                        {fields.map((f) => renderField(c.name, f))}
                      </div>
                      {c.evidence?.rule && <p className="hint subtle mono" style={{ margin: 0 }}>rule: {c.evidence.rule}</p>}
                    </div>
                  )}
                </div>
              );
            })}
          </fieldset>

          {rangeNeeded && (
            <div className="evidence">
              <div className="row tight">
                <NumberField id={`low-${p.cid}`} label="Traded low" value={low} onChange={setLow} placeholder="0.00" />
                <NumberField id={`high-${p.cid}`} label="Traded high" value={high} onChange={setHigh} placeholder="0.00" />
              </div>
              <p className={`hint subtle${low !== '' && high !== '' && !rangeContains ? ' bad-text' : ''}`}>
                {low === '' || high === ''
                  ? 'The ledger checks that the proposed price sits inside the range your book traded. Attach it or the confirmation is refused on-ledger.'
                  : !rangeOk ? 'Low must not exceed high.'
                    : rangeContains ? `${fmtN(p.price)} sits inside ${fmtN(lowN)}–${fmtN(highN)}.`
                      : `${fmtN(p.price)} is OUTSIDE ${fmtN(lowN)}–${fmtN(highN)} — the ledger will refuse this confirmation. Refuse with a reason instead.`}
              </p>
            </div>
          )}

          {deadlinePassed && <p className="hint subtle bad-text">The window has closed; the backend may no longer accept this.</p>}

          {!refusing ? (
            <div className="proposal-actions">
              <button type="button" className="primary" disabled={!canConfirm} onClick={() => void confirm()}
                title={missing ? `Missing: ${missing}` : undefined}>
                {busy === 'confirm' ? 'Submitting…' : `Confirm${effectiveChecks.length ? ` (${effectiveChecks.length} verified)` : ''}`}
              </button>
              <button type="button" className="ghost" disabled={busy !== null} onClick={() => { setRefusing(true); setError(null); setErrorHint(null); }}>
                Refuse with reason
              </button>
              {missing && busy === null && <span className="hint subtle" style={{ margin: 0 }}>Missing: <span className="mono">{missing}</span></span>}
            </div>
          ) : (
            <form className="refuse" onSubmit={(e) => { e.preventDefault(); void refuse(); }}>
              <div className="row tight">
                <label className="field" htmlFor={`rc-${p.cid}`}>
                  <span>Condition that fails</span>
                  <select id={`rc-${p.cid}`} value={refuseCondition} onChange={(e) => setRefuseCondition(e.target.value)}>
                    {conditions.map((c) => <option key={c.name} value={c.name}>{c.name}</option>)}
                    <option value="other">other</option>
                  </select>
                </label>
                <label className="field grow" htmlFor={`rr-${p.cid}`}>
                  <span>Reason</span>
                  <input id={`rr-${p.cid}`} value={reason} onChange={(e) => setReason(e.target.value)} required
                    placeholder="What you saw that the proposal does not reflect" />
                </label>
              </div>
              <div className="proposal-actions">
                <button type="submit" className="primary sell" disabled={busy !== null || !reason.trim()}>
                  {busy === 'refuse' ? 'Recording…' : 'Refuse'}
                </button>
                <button type="button" className="ghost" disabled={busy !== null} onClick={() => setRefusing(false)}>Back</button>
              </div>
            </form>
          )}
        </>
      )}

      {error && (
        <div className="banner error" role="alert">
          <span>{error}{errorHint ? <><br /><span className="muted">{errorHint}</span></> : null}</span>
        </div>
      )}

      <div className="msglog">
        <button type="button" className="link" aria-expanded={showLog} onClick={() => setShowLog((s) => !s)}>
          {showLog ? 'Hide message log' : 'Message log'}
        </button>
        {showLog && (
          <>
            {log.loading && <div className="empty">Loading…</div>}
            {log.error && <div className="error">{log.error}</div>}
            {log.data && log.data.length === 0 && <div className="empty">No events recorded for this proposal.</div>}
            {log.data && log.data.length > 0 && (
              <ol className="msglog-list">
                {log.data.map((e, i) => (
                  <li key={e.id ?? i} className="msglog-row">
                    <span className="mono muted">{fmtTime(e.ts)}</span>
                    <span className={`tag ev ${e.kind.replace(/\W+/g, '-')}`}>{e.kind}</span>
                    <span className="msglog-text">
                      {e.actor && <strong>{e.actor}{e.seat ? ` (${e.seat})` : ''}</strong>}
                      {e.price !== undefined && e.price !== null && <span className="mono"> {fmtN(e.price)}</span>}
                      {/* The backend's `detail` usually restates `reason`; say each thing once. */}
                      {e.detail && <span> — {e.detail}</span>}
                      {e.reason && !(e.detail ?? '').includes(e.reason) && <span> — {e.reason}</span>}
                      {e.cid && <span className="mono muted"> · {shortCid(e.cid)}</span>}
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </>
        )}
      </div>
    </article>
  );
}
