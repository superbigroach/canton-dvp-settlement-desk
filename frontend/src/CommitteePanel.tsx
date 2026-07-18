// CommitteePanel — the DECENTRALISED OPERATOR. A K-of-N committee strikes the
// official price (the NAV / close) so no single party can print it alone. You watch
// the signatures accumulate: propose → each member confirms → finalise once the
// threshold is met. The resulting NavFixing carries K genuine member signatures —
// an auction can then be bound to it (see the desk's Open/Close cross).

import { useState } from 'react';
import { api, ApiError, type Instrument, type Party, type Session } from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  flash: (m: string) => void;
}

const CASH = 'USDC';

export default function CommitteePanel({ parties, instruments, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  const assets = instruments.filter((i) => i.kind !== 'Cash');

  // Committee config
  const [members, setMembers] = useState<string[]>(['Venue', 'Bank', 'Agent']);
  const [threshold, setThreshold] = useState<number>(2);
  const [admin] = useState<string>('Issuer');
  const [committeeCid, setCommitteeCid] = useState<string>('');

  // Fixing in progress
  const [instrumentId, setInstrumentId] = useState<string>(assets[0]?.id ?? 'cETH');
  const [session, setSession] = useState<Session>('Close');
  const [price, setPrice] = useState<string>('');
  const [proposalCid, setProposalCid] = useState<string>('');
  const [attestors, setAttestors] = useState<string[]>([]);
  const [fixCid, setFixCid] = useState<string>('');

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');

  const priceOf = (id: string) => instruments.find((i) => i.id === id)?.referencePrice ?? null;

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setErr('');
    try {
      return await fn();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : e instanceof Error ? e.message : String(e));
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  function toggleMember(label: string) {
    setMembers((m) => (m.includes(label) ? m.filter((x) => x !== label) : [...m, label]));
  }

  async function createCommittee() {
    const res = await run(() =>
      api.createCommittee({ admin, members, threshold, label: 'NAV Committee' }),
    );
    if (!res) return;
    setCommitteeCid(res.contractId);
    setProposalCid('');
    setAttestors([]);
    setFixCid('');
    flash(`Committee stood up — ${threshold}-of-${members.length} members must attest each fix.`);
  }

  async function propose() {
    if (!committeeCid) return;
    const proposer = members[0];
    const p = Number(price) || priceOf(instrumentId) || 0;
    const res = await run(() =>
      api.proposeFixing(committeeCid, {
        proposer,
        instrumentId,
        price: p,
        cashInstrument: CASH,
        session,
        rationale: 'uniform sealed-cross VWAP',
      }),
    );
    if (!res) return;
    setProposalCid(res.contractId);
    setAttestors([proposer]);
    setFixCid('');
    flash(`${proposer} proposed ${instrumentId} ${session} @ ${p} — 1 of ${threshold} attestations.`);
  }

  async function confirm(member: string) {
    if (!proposalCid) return;
    const res = await run(() => api.confirmFixing(proposalCid, member));
    if (!res) return;
    setProposalCid(res.contractId);
    const next = [...attestors, member];
    setAttestors(next);
    flash(`${member} attested — ${next.length} of ${threshold}.`);
  }

  async function finalize() {
    if (!proposalCid) return;
    const proposer = attestors[0];
    // Publish the fix to the venue (the auction operator that will print at it).
    const res = await run(() => api.finalizeFixing(proposalCid, proposer, ['Venue']));
    if (!res) return;
    setFixCid(res.contractId);
    setProposalCid('');
    flash(`Official NAV struck by ${attestors.length}-of-${members.length} — no single party could.`);
  }

  const enoughAttestors = attestors.length >= threshold;
  const pending = members.filter((m) => !attestors.includes(m));
  const suggested = priceOf(instrumentId);

  return (
    <section className="card committee" aria-label="Decentralised operator committee">
      <div className="card-head">
        <h2>Decentralised Operator</h2>
        <span className="who">K-of-N NAV committee</span>
      </div>
      <p className="hint">
        The official price must not be one venue&rsquo;s number. A committee of independent members
        attests it; only once <strong>{threshold} of {members.length}</strong> have signed does an
        official <strong>NavFixing</strong> exist — provable from the contract&rsquo;s own signatures.
      </p>

      {err && (
        <div className="warn" onClick={() => setErr('')}>
          {err}
        </div>
      )}

      {/* Step 1 — stand up the committee */}
      <div className="committee-step">
        <div className="step-label">1 · Committee</div>
        <div className="member-chips">
          {people.map((p) => (
            <button
              key={p.party}
              className={`chip ${members.includes(p.label) ? 'on' : ''}`}
              disabled={busy || !!committeeCid}
              onClick={() => toggleMember(p.label)}
            >
              {p.label}
            </button>
          ))}
        </div>
        <div className="row tight">
          <label className="field small">
            <span>Threshold (K)</span>
            <input
              type="number"
              min="1"
              max={members.length}
              value={threshold}
              disabled={busy || !!committeeCid}
              onChange={(e) => setThreshold(Math.max(1, Number(e.target.value) || 1))}
            />
          </label>
          <button className="primary" disabled={busy || members.length < 1 || !!committeeCid} onClick={createCommittee}>
            {committeeCid ? '✓ Committee live' : `Stand up ${threshold}-of-${members.length}`}
          </button>
        </div>
      </div>

      {/* Step 2 — propose a fix */}
      {committeeCid && (
        <div className="committee-step">
          <div className="step-label">2 · Propose the fix</div>
          <div className="row tight">
            <label className="field">
              <span>Instrument</span>
              <select value={instrumentId} disabled={busy} onChange={(e) => setInstrumentId(e.target.value)}>
                {assets.map((i) => (
                  <option key={i.id} value={i.id}>
                    {i.id}
                  </option>
                ))}
              </select>
            </label>
            <div className="field">
              <span>Session</span>
              <div className="segmented session">
                <button className={session === 'Open' ? 'on' : ''} disabled={busy} onClick={() => setSession('Open')}>
                  Open
                </button>
                <button className={session === 'Close' ? 'on' : ''} disabled={busy} onClick={() => setSession('Close')}>
                  Close
                </button>
              </div>
            </div>
            <label className="field small">
              <span>Price ({CASH})</span>
              <input
                className="mono"
                type="number"
                min="0"
                step="any"
                placeholder={suggested != null ? String(suggested) : ''}
                value={price}
                disabled={busy}
                onChange={(e) => setPrice(e.target.value)}
              />
            </label>
          </div>
          <button className="primary" disabled={busy} onClick={propose}>
            {members[0]} proposes the {session} fix
          </button>
        </div>
      )}

      {/* Step 3 — accumulate attestations */}
      {proposalCid && (
        <div className="committee-step">
          <div className="step-label">
            3 · Attestations · {attestors.length} of {threshold}
          </div>
          <div className="attest-row">
            {members.map((m) => (
              <span key={m} className={`attest ${attestors.includes(m) ? 'signed' : ''}`}>
                {attestors.includes(m) ? '✓ ' : ''}
                {m}
              </span>
            ))}
          </div>
          <div className="member-chips">
            {pending.map((m) => (
              <button key={m} className="chip confirm" disabled={busy} onClick={() => confirm(m)}>
                {m} confirms
              </button>
            ))}
          </div>
          <button className="primary" disabled={busy || !enoughAttestors} onClick={finalize}>
            {enoughAttestors
              ? `Finalise · strike the official NAV (${attestors.length}-of-${members.length})`
              : `Need ${threshold - attestors.length} more attestation(s)`}
          </button>
          {!enoughAttestors && (
            <p className="warn subtle">
              A single member cannot finalise — the ledger rejects it below threshold.
            </p>
          )}
        </div>
      )}

      {/* Result — the official fix */}
      {fixCid && (
        <div className="committee-result">
          <div className="fix-badge">OFFICIAL NAV · {attestors.length}-of-{members.length} attested</div>
          <div className="fix-line mono">
            {instrumentId} {session} @ <strong>{Number(price) || suggested} {CASH}</strong>
          </div>
          <code className="cid mono" title={fixCid}>
            NavFixing · {fixCid.length > 18 ? `${fixCid.slice(0, 10)}…${fixCid.slice(-6)}` : fixCid}
          </code>
          <p className="hint">
            Credibly neutral: this price carries {attestors.length} genuine member signatures. An
            auction bound to it will print <em>only</em> at this attested NAV.
          </p>
        </div>
      )}
    </section>
  );
}
