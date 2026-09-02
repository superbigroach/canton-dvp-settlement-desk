// One proposal, as the seat sees it: what is proposed and how it was built, how long is
// left, the named conditions THIS seat verifies, evidence where the seat must attach it,
// Confirm / Refuse, and the message log. Optimistic: the card moves the moment you act
// and moves back, with the backend's sentence, if the ledger says no.
import { useState } from 'react';
import type { SignerRole } from '../../api';
import { errorMessage } from '../../api';
import { desk, type Proposal, type ProposalEvent } from '../../desk';
import { Countdown, fmtN, fmtQty, fmtTs, fmtTime, NumberField, shortCid, useAsync } from '../../components/ui';

interface Props {
  proposal: Proposal;
  role: SignerRole | null;          // my seat's protocol entry (names + passesWhen)
  onChanged: (next: Proposal) => void;
  readOnly?: boolean;
}

export default function ProposalCard({ proposal: p, role, onChanged, readOnly }: Props) {
  const conditionNames = p.conditions.length ? p.conditions : role?.conditions.map((c) => c.name) ?? [];
  const needsRange = p.requiresObservedRange || role?.requiresObservedRange || false;
  const passesWhen = (name: string) => role?.conditions.find((c) => c.name === name)?.passesWhen;

  const [checks, setChecks] = useState<string[]>([]);
  const [low, setLow] = useState('');
  const [high, setHigh] = useState('');
  const [refusing, setRefusing] = useState(false);
  const [refuseCondition, setRefuseCondition] = useState(conditionNames[0] ?? '');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState<'confirm' | 'refuse' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showLog, setShowLog] = useState(false);

  const log = useAsync<ProposalEvent[]>(() => (showLog ? desk.proposalEvents(p.cid) : Promise.resolve([])), [showLog, p.cid, p.mine?.at]);

  const open = p.status === 'open' && !p.mine;
  const deadlinePassed = new Date(p.deadline).getTime() < Date.now();
  const lowN = Number(low); const highN = Number(high);
  const rangeOk = !needsRange || (low !== '' && high !== '' && lowN <= highN);
  const rangeContains = !needsRange || (rangeOk && p.price >= lowN && p.price <= highN);
  const canConfirm = open && !readOnly && checks.length > 0 && rangeOk && busy === null;

  const toggle = (name: string) =>
    setChecks((c) => (c.includes(name) ? c.filter((x) => x !== name) : [...c, name]));

  const confirm = async () => {
    setBusy('confirm'); setError(null);
    const optimistic: Proposal = {
      ...p, confirmed: [...p.confirmed, role?.key ?? 'me'],
      mine: { action: 'confirmed', at: new Date().toISOString(), checks, evidence: needsRange ? { low: lowN, high: highN } : undefined },
    };
    onChanged(optimistic);
    try {
      const r = await desk.confirm(p.cid, { checks, evidence: needsRange ? { low: lowN, high: highN } : undefined });
      const cid = r?.cid || r?.contractId;
      onChanged({ ...optimistic, mine: { ...optimistic.mine!, cid: cid || undefined },
        status: optimistic.confirmed.length >= p.k ? 'finalized' : optimistic.status });
    } catch (e) {
      onChanged(p);
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
  };

  const refuse = async () => {
    if (!reason.trim()) { setError('Say why — a refusal without a reason is not recorded.'); return; }
    setBusy('refuse'); setError(null);
    const optimistic: Proposal = { ...p, status: 'refused', mine: { action: 'refused', at: new Date().toISOString(), reason } };
    onChanged(optimistic);
    try {
      const r = await desk.refuse(p.cid, { condition: refuseCondition, reason: reason.trim() });
      const cid = r?.cid || r?.contractId;
      onChanged({ ...optimistic, mine: { ...optimistic.mine!, cid: cid || undefined } });
      setRefusing(false);
    } catch (e) {
      onChanged(p);
      setError(errorMessage(e));
    } finally {
      setBusy(null);
    }
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
          <span className="muted">{p.confirmed.length} of {p.k} needed · {p.n} seats</span>
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
      <div className="proposal-facts mono muted">
        <span>proposed {fmtTs(p.proposedAt)}{p.proposedBy ? ` by ${p.proposedBy}` : ''}</span>
        <span>window ends {fmtTime(p.deadline)}</span>
        <span>cid {shortCid(p.cid)}</span>
        {p.confirmed.length > 0 && <span>attested: {p.confirmed.join(', ')}</span>}
      </div>

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
          <fieldset className="checklist">
            <legend>Conditions your seat verifies{role ? ` (${role.title})` : ''}</legend>
            {conditionNames.length === 0 && (
              <p className="hint subtle">No conditions are defined for your seat on this instrument — the signer protocol may not be loaded.</p>
            )}
            {conditionNames.map((name) => (
              <label key={name} className={`check${checks.includes(name) ? ' on' : ''}`}>
                <input type="checkbox" checked={checks.includes(name)} onChange={() => toggle(name)} disabled={busy !== null} />
                <span className="check-name mono">{name}</span>
                {passesWhen(name) && <span className="check-when">{passesWhen(name)}</span>}
              </label>
            ))}
          </fieldset>

          {needsRange && (
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
              <button type="button" className="primary" disabled={!canConfirm} onClick={() => void confirm()}>
                {busy === 'confirm' ? 'Submitting…' : `Confirm${checks.length ? ` (${checks.length} verified)` : ''}`}
              </button>
              <button type="button" className="ghost" disabled={busy !== null} onClick={() => { setRefusing(true); setError(null); }}>
                Refuse with reason
              </button>
            </div>
          ) : (
            <form className="refuse" onSubmit={(e) => { e.preventDefault(); void refuse(); }}>
              <div className="row tight">
                <label className="field" htmlFor={`rc-${p.cid}`}>
                  <span>Condition that fails</span>
                  <select id={`rc-${p.cid}`} value={refuseCondition} onChange={(e) => setRefuseCondition(e.target.value)}>
                    {conditionNames.map((n) => <option key={n} value={n}>{n}</option>)}
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

      {error && <div className="banner error" role="alert"><span>{error}</span></div>}

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
                      {e.price !== undefined && <span className="mono"> {fmtN(e.price)}</span>}
                      {e.detail && <span> — {e.detail}</span>}
                      {e.reason && <span> — {e.reason}</span>}
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
