// CommitteePanel — the DECENTRALISED OPERATOR. A K-of-N committee strikes the
// official price (the NAV / close) so no single party can print it alone. You watch
// the signatures accumulate: propose → each member confirms → finalise once the
// threshold is met. The resulting NavFixing carries K genuine member signatures —
// an auction can then be bound to it (see the desk's Open/Close cross).
//
// TWO KINDS OF FIX, AND THE DIFFERENCE IS THE PRODUCT.
//   SNAPSHOT — a price, true at one instant. Between two fixings a share is worth the
//   last mark, which is wrong every second in between and gets more wrong the longer
//   the gap. Right for anything whose value is DISCOVERED: equities, cETH, a volatile
//   token. This is what `ProposeFixing` has always produced and it is untouched.
//
//   ACCRUING — the committee attests the INPUTS instead: base, rate per annum,
//   day-count convention, and the instant the mark applies from. A treasury or
//   money-market fund's value between marks is not discovered, it is EARNED, at a rate
//   already agreed on a convention already agreed. So the ledger integrates it and the
//   fund is correctly valued at 03:41 on a Sunday. Four numbers signed once, instead of
//   86,400 attestations a day — which is also the security argument, because a
//   committee that must re-attest every second has 86,400 chances a day to be wrong,
//   late, captured or offline.
//
// Choosing "Accruing" here calls a DIFFERENT Daml choice (`ProposeAccruingFixing`), and
// what each member then confirms is the whole recipe rather than just the base —
// confirming a price without the rate that will move it would be attesting a third of
// a number.

import { useEffect, useState } from 'react';
import AccrualTicker from './AccrualTicker';
import {
  api,
  errorMessage,
  type DayCountConvention,
  type FixingResponse,
  type Instrument,
  type Party,
  type Session,
} from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  flash: (m: string) => void;
}

const CASH = 'USDC';

/**
 * Rate presets. THESE ARE REAL ATTESTATIONS, NOT DISPLAY SPEEDS — picking one proposes
 * that rate on the ledger and every member signs it, so the number that then ticks is
 * genuinely what the committee agreed the fund earns. There is no acceleration
 * multiplier anywhere in this UI; a bigger number on screen requires a bigger signed
 * rate, which is the honest way to make accrual legible from the back of a room.
 */
const RATE_PRESETS: { label: string; value: string; note: string }[] = [
  { label: '3.6%', value: '0.036', note: 'money-market: exactly 1bp/day on ACT/360' },
  { label: '5%', value: '0.05', note: 'front-end USD yield' },
  { label: '36%', value: '0.36', note: 'legible from the back of the room' },
  { label: '−0.5%', value: '-0.005', note: 'EUR/CHF/JPY 2015–2022: funds accrued DOWN' },
];

const DAY_COUNTS: { value: DayCountConvention; note: string }[] = [
  { value: 'ACT/360', note: 'USD money market — SOFR, repo, T-bills' },
  { value: 'ACT/365F', note: 'GBP/AUD/NZD/HKD/SGD money market' },
];

export default function CommitteePanel({ parties, instruments, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  const assets = instruments.filter((i) => i.kind !== 'Cash');

  // Committee config
  // Default members exist on BOTH the local sandbox and the devnet roster.
  const [members, setMembers] = useState<string[]>(['Issuer', 'Bank', 'Auditor']);
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

  // The accrual recipe under attestation. `accruing=false` is the original snapshot
  // path, byte-for-byte — a fixing with rate 0 accrues nothing at any instant.
  const [accruing, setAccruing] = useState<boolean>(true);
  const [rate, setRate] = useState<string>('0.036');
  const [dayCount, setDayCount] = useState<DayCountConvention>('ACT/360');

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');

  // FIXES ALREADY ON THE LEDGER. The committee flow above is client state and a page
  // reload loses it — but the NavFixings do not go anywhere, they are contracts. This
  // reads them back so a refresh (or a second browser, or a judge opening the URL
  // themselves) can attach the ticker to a fix that was struck minutes ago and watch it
  // carry on accruing from its attested origin. Nothing is recomputed or re-struck: it
  // is the same contract, valued at a later instant.
  const [existing, setExisting] = useState<FixingResponse[]>([]);
  useEffect(() => {
    let live = true;
    api
      .fixings()
      .then((fs) => {
        if (live) setExisting(fs);
      })
      .catch(() => {
        /* an empty roster is a normal state, not an error to shout about */
      });
    return () => {
      live = false;
    };
  }, [fixCid]);

  const priceOf = (id: string) => instruments.find((i) => i.id === id)?.referencePrice ?? null;

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setErr('');
    try {
      return await fn();
    } catch (e) {
      // ONE formatter for every failure path — see errorMessage() in api.ts.
      setErr(errorMessage(e));
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

    // TWO DIFFERENT DAML CHOICES. The snapshot path is unchanged and still takes
    // exactly the arguments it always took; the accruing path attests three more.
    const res = await run(() =>
      accruing
        ? api.proposeAccruingFixing(committeeCid, {
            proposer,
            instrumentId,
            price: p,
            ratePerAnnum: rate.trim() === '' ? '0' : rate.trim(),
            dayCount,
            cashInstrument: CASH,
            session,
            rationale: `sealed-cross VWAP base, accruing ${rate} ${dayCount}`,
            // `accrualFrom` deliberately omitted: the desk stamps its own clock. A real
            // committee agreeing a mark "as of 16:00" would send that instant instead —
            // the field is attested, not clocked, precisely so the two can differ.
          })
        : api.proposeFixing(committeeCid, {
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
    flash(
      accruing
        ? `${proposer} proposed ${instrumentId} ${session}: base ${p}, accruing ${rate} ${dayCount} — 1 of ${threshold}.`
        : `${proposer} proposed ${instrumentId} ${session} @ ${p} — 1 of ${threshold} attestations.`,
    );
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
    // The fix is already visible to the committee + auditor; publish more widely
    // only where a dedicated venue party exists (not on every roster).
    const res = await run(() => api.finalizeFixing(proposalCid, proposer, []));
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
        Attest a <strong>rate</strong> as well as a base and the ledger keeps deriving the value
        every second after: <strong>the committee attests the inputs, the ledger computes.</strong>
      </p>

      {err && (
        <div className="warn" onClick={() => setErr('')}>
          {err}
        </div>
      )}

      {/* Fixes already struck — a reload loses the client flow, never the contracts. */}
      {!fixCid && existing.length > 0 && (
        <div className="committee-step">
          <div className="step-label">Already on the ledger</div>
          <div className="member-chips">
            {existing.slice(0, 4).map((f) => (
              <button
                key={f.contractId}
                className="chip"
                disabled={busy}
                title={`${f.attestors.join(', ')} attested · base ${f.basePrice} · struck ${f.finalizedAt}`}
                onClick={() => {
                  setFixCid(f.contractId);
                  setInstrumentId(f.instrumentId);
                  setSession(f.session);
                  setAccruing(f.accruing);
                  // The wire carries the ledger's full 10dp ("0.0360000000"); the form
                  // shows the rate the committee actually typed.
                  setRate(f.ratePerAnnum.replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, ''));
                  if (f.dayCount !== 'NONE') setDayCount(f.dayCount);
                  setAttestors(f.attestors);
                  flash(`Watching the ${f.instrumentId} ${f.session} fix struck by ${f.attestors.length} members.`);
                }}
              >
                {f.instrumentId} {f.session} {f.accruing ? '· accruing' : '· snapshot'}
              </button>
            ))}
          </div>
          <p className="hint subtle">
            A NavFixing is a contract, not a session. Attach the ticker to one struck earlier and it
            carries on from its own attested origin — same inputs, later instant, no re-striking.
          </p>
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
              <span>{accruing ? `Base NAV (${CASH})` : `Price (${CASH})`}</span>
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

          {/* SNAPSHOT vs ACCRUING — the choice that decides which Daml choice runs. */}
          <div className="field">
            <span>What is being attested</span>
            <div className="segmented accrual-mode">
              <button className={!accruing ? 'on' : ''} disabled={busy} onClick={() => setAccruing(false)}>
                Snapshot
              </button>
              <button className={accruing ? 'on' : ''} disabled={busy} onClick={() => setAccruing(true)}>
                Accruing
              </button>
            </div>
          </div>

          {accruing ? (
            <>
              <div className="row tight">
                <label className="field small">
                  <span>Rate p.a.</span>
                  <input
                    className="mono"
                    type="number"
                    step="any"
                    value={rate}
                    disabled={busy}
                    onChange={(e) => setRate(e.target.value)}
                  />
                </label>
                <label className="field small">
                  <span>Day count</span>
                  <select
                    value={dayCount}
                    disabled={busy}
                    onChange={(e) => setDayCount(e.target.value as DayCountConvention)}
                  >
                    {DAY_COUNTS.map((d) => (
                      <option key={d.value} value={d.value}>
                        {d.value}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="member-chips">
                {RATE_PRESETS.map((r) => (
                  <button
                    key={r.value}
                    className={`chip ${rate === r.value ? 'on' : ''}`}
                    disabled={busy}
                    title={r.note}
                    onClick={() => setRate(r.value)}
                  >
                    {r.label}
                  </button>
                ))}
              </div>
              <p className="hint subtle">
                {DAY_COUNTS.find((d) => d.value === dayCount)?.note}. The convention is
                ATTESTED, not assumed — &ldquo;4%&rdquo; on ACT/360 and on ACT/365F differ by
                1.389% of the yield, which on a $1bn fund is $560,000 a year. 30/360 and
                ACT/ACT are <strong>refused by the ledger</strong> rather than defaulted, so a
                typo is a rejection and never a silent mis-accrual.
              </p>
            </>
          ) : (
            <p className="hint subtle">
              A snapshot is true at one instant and stale at every instant after it. Correct
              for anything whose value is <em>discovered</em> — an equity, cETH, a volatile
              token — where accruing would be inventing data.
            </p>
          )}

          <button className="primary" disabled={busy} onClick={propose}>
            {members[0]} proposes the {session} {accruing ? 'accrual' : 'fix'}
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

      {/* Result — the official fix, and (if it accrues) the value MOVING */}
      {fixCid && (
        <div className="committee-result">
          <div className="fix-badge">OFFICIAL NAV · {attestors.length}-of-{members.length} attested</div>
          <div className="fix-line mono">
            {instrumentId} {session}{' '}
            {accruing ? (
              <>
                base <strong>{Number(price) || suggested} {CASH}</strong>
                <span className="faint"> · {rate} {dayCount}</span>
              </>
            ) : (
              <>
                @ <strong>{Number(price) || suggested} {CASH}</strong>
              </>
            )}
          </div>
          <code className="cid mono" title={fixCid}>
            NavFixing · {fixCid.length > 18 ? `${fixCid.slice(0, 10)}…${fixCid.slice(-6)}` : fixCid}
          </code>
          <p className="hint">
            Credibly neutral: this fix carries {attestors.length} genuine member signatures — the
            contract&rsquo;s own signatory set IS the proof. An auction bound to it can only print
            against this attested NAV.
          </p>

          {/*
            THE DEMO MOMENT. The committee attested four numbers; from here the LEDGER
            derives the value at every instant, and this is that derivation running live.
            Read straight off the NavFixing contract — no client state, no acceleration.
          */}
          <AccrualTicker fixCid={fixCid} />
        </div>
      )}
    </section>
  );
}
