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
import type { SignerRole } from './api';
import AccrualTicker from './AccrualTicker';
import {
  api,
  errorMessage,
  type DayCountConvention,
  type FixingResponse,
  type Instrument,
  type LiveMark,
  type Party,
  type Session,
} from './api';

interface Props {
  parties: Party[];
  instruments: Instrument[];
  /** The desk's selected asset. The committee strikes THIS, not a random default. */
  asset?: string;
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

export default function CommitteePanel({ parties, instruments, asset, flash }: Props) {
  const people = parties.filter((p) => p.label.toLowerCase() !== 'sandbox');
  const assets = instruments.filter((i) => i.kind !== 'Cash');

  // Committee config
  // Default members exist on BOTH the local sandbox and the devnet roster.
  // THE SEATS PERSIST WITH THE COMMITTEE. A refresh used to reset the chips to the
  // default trio while the cid still pointed at a committee with different members,
  // so the buttons offered "Auditor confirms" against a committee Auditor is not on.
  const MEMBERS_KEY = 'crossdesk.committeeMembers';
  const [members, setMembers] = useState<string[]>(() => {
    try {
      const raw = window.localStorage.getItem(MEMBERS_KEY);
      const arr = raw ? (JSON.parse(raw) as unknown) : null;
      if (Array.isArray(arr) && arr.every((x) => typeof x === 'string') && arr.length) return arr;
    } catch {
      /* fall through to the default */
    }
    return ['Issuer', 'Bank', 'Auditor'];
  });
  useEffect(() => {
    try {
      window.localStorage.setItem(MEMBERS_KEY, JSON.stringify(members));
    } catch {
      /* private browsing */
    }
  }, [members]);
  const [threshold, setThreshold] = useState<number>(2);
  const [admin] = useState<string>('Issuer');
  // THE COMMITTEE IS A CONTRACT ON THE LEDGER; ONLY THE POINTER WAS IN MEMORY.
  // Standing one up creates a real OperatorCommittee that outlives the tab, but the
  // cid lived in React state, so a refresh made the desk offer to stand up a SECOND
  // committee over a book that already had one. Keep the handle where the page can
  // find it again.
  const CID_KEY = 'crossdesk.committeeCid';
  const [committeeCid, setCommitteeCid] = useState<string>(
    () => {
      try {
        return window.localStorage.getItem(CID_KEY) ?? '';
      } catch {
        return '';
      }
    },
  );
  useEffect(() => {
    try {
      if (committeeCid) window.localStorage.setItem(CID_KEY, committeeCid);
      else window.localStorage.removeItem(CID_KEY);
    } catch {
      /* private browsing — the desk still works, it just forgets */
    }
  }, [committeeCid]);

  // Fixing in progress
  const [instrumentId, setInstrumentId] = useState<string>(asset || assets[0]?.id || 'cETH');
  const [session, setSession] = useState<Session>('Close');
  const [price, setPrice] = useState<string>('');
  const [proposalCid, setProposalCid] = useState<string>('');
  const [attestors, setAttestors] = useState<string[]>([]);
  const [fixCid, setFixCid] = useState<string>('');

  // The accrual recipe under attestation. `accruing=false` is the original snapshot
  // path, byte-for-byte — a fixing with rate 0 accrues nothing at any instant.
  const [accruing, setAccruing] = useState<boolean>(true);

  // THE FORM STRIKES WHAT THE DESK IS LOOKING AT. Opening on whatever happened to be
  // first in the instrument list meant the committee card described a different asset
  // from every other card on the page — which reads as a bug even when it is not.
  useEffect(() => {
    if (asset) setInstrumentId(asset);
  }, [asset]);

  // AND THE MODE IS A PROPERTY OF THE INSTRUMENT, NOT A QUESTION FOR THE OPERATOR.
  // A money-market share earns continuously, so its value is a recipe (base + rate +
  // day count) the ledger keeps deriving. Anything with a market has a mark, and a
  // mark is a snapshot. Asking a human to classify that every time is an invitation
  // to attest a T-bill as a static number, or cETH as though it accrued interest.
  useEffect(() => {
    const kind = instruments.find((i) => i.id === instrumentId)?.kind ?? '';
    setAccruing(kind === 'MoneyMarket');
  }, [instrumentId, instruments]);

  // A FUND HAS NO OPENING NAV. Equity markets genuinely run two auctions and both
  // prints are official, which is why the Open/Close choice exists at all — but a
  // fund strikes ONE net asset value per day, at the close. Offering "Open" on a
  // basket or a money-market share invites an attestation that has no meaning in
  // fund accounting, so for those the session is stated rather than chosen.
  const kindOf = instruments.find((i) => i.id === instrumentId)?.kind ?? '';
  const closeOnly = kindOf === 'MoneyMarket' || kindOf === 'Fund';
  useEffect(() => {
    if (closeOnly) setSession('Close');
  }, [closeOnly]);
  const [rate, setRate] = useState<string>('0.036');
  const [dayCount, setDayCount] = useState<DayCountConvention>('ACT/360');

  // A WRAPPED ASSET IS PRICED AS TWO SIGNED FIELDS, NOT ONE NUMBER. cBTC is a claim on
  // BTC held under an attestor multisig: its value is the benchmark print (CME CF BRR —
  // free, public, nobody argues) times the market's confidence that redemption works
  // (the par factor — the thing the committee actually decides). The ledger multiplies
  // them, so the factor can never disagree with the price it produced, and an issuer
  // that will not attest par has to refuse in a field rather than inside a number.
  const wrapped = kindOf === 'CryptoWrapped';
  const [benchmark, setBenchmark] = useState<string>('');
  const [parFactor, setParFactor] = useState<string>('0.998');
  const benchmarkNum =
    Number(benchmark) || (instruments.find((i) => i.id === instrumentId)?.referencePrice ?? 0);
  const parFactorNum = Number(parFactor) || 0;
  const wrappedStrike = benchmarkNum * parFactorNum;
  const wrappedBps = (1 - parFactorNum) * 10000;

  // CANDIDATE marks from an outside feed. These are NOT prices — nothing values
  // against them until this committee signs. They exist so a member proposes today's
  // real number rather than one typed from memory. The feed proposes; the committee
  // disposes, and it is the signatures that make the result provable.
  const [liveMarks, setLiveMarks] = useState<LiveMark[]>([]);
  const liveMark = liveMarks.find((m) => m.instrumentId === instrumentId);

  // NAV is a FUND concept; a single asset has a MARK. An accruing fix values a
  // fund-like instrument (a money-market share, whose value is earned) so it really is
  // a NAV. A snapshot fix on cETH is a mark. Saying "NAV" for both invites exactly the
  // question "why does one market have a NAV?" — which has no good answer.
  const strikeNoun = accruing ? 'NAV' : 'mark';
  const strikeNounCaps = accruing ? 'NAV' : 'MARK';

  // ---- The signer protocol -------------------------------------------------
  // FETCHED, NOT HARD-CODED. The backend validates against the same list, so a box
  // that appears here is a box `/confirm-checked` will accept. A local copy would
  // drift from the rule, and a signer ticking something the API then refuses learns
  // that their seat is decorative.
  const [protocolVersion, setProtocolVersion] = useState<string>('');
  const [signerRoles, setSignerRoles] = useState<SignerRole[]>([]);
  // Which member is filling in evidence right now, and what they have ticked.
  const [signingMember, setSigningMember] = useState<string>('');
  const [signingRole, setSigningRole] = useState<string>('');
  const [ticked, setTicked] = useState<string[]>([]);
  const [obsLow, setObsLow] = useState<string>('');
  const [obsHigh, setObsHigh] = useState<string>('');

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

  // Candidate marks, refreshed on a slow tick. A minute is plenty: these only ever
  // pre-fill a proposal a human then attests, so there is nothing to chase.
  useEffect(() => {
    let live = true;
    const load = () =>
      api
        .liveMarks()
        .then((m) => {
          if (live) setLiveMarks(m);
        })
        .catch(() => {
          /* no feed = type the mark in; never a broken panel */
        });
    void load();
    const t = setInterval(load, 60_000);
    return () => {
      live = false;
      clearInterval(t);
    };
  }, []);

  const priceOf = (id: string) => instruments.find((i) => i.id === id)?.referencePrice ?? null;

  async function run<T>(fn: () => Promise<T>): Promise<T | undefined> {
    setBusy(true);
    setErr('');
    try {
      return await fn();
    } catch (e) {
      // ONE formatter for every failure path — see errorMessage() in api.ts.
      const msg = errorMessage(e);
      // THE SANDBOX RESEEDS ON EVERY RESTART. A committee cid remembered from before
      // a restart points at a contract that no longer exists, and every action on it
      // fails with CONTRACT_NOT_FOUND. Forget it and say so, instead of showing the
      // same opaque error on every click.
      if (/CONTRACT_NOT_FOUND/i.test(msg) && committeeCid) {
        forgetCommittee();
        setErr('The ledger was reset since this committee was stood up, so it no longer exists. Stand up a new one.');
        return undefined;
      }
      setErr(msg);
      return undefined;
    } finally {
      setBusy(false);
    }
  }

  function forgetCommittee() {
    setCommitteeCid('');
    setProposalCid('');
    setAttestors([]);
    setFixCid('');
  }

  // ON LOAD, CHECK THE REMEMBERED COMMITTEE IS STILL ON THE LEDGER. The ledger end
  // only moves forward within one sandbox life; if it is now BEHIND where it was when
  // the committee was stood up, the sandbox restarted and the committee is gone.
  const END_KEY = 'crossdesk.committeeLedgerEnd';
  useEffect(() => {
    if (!committeeCid) return;
    let cancelled = false;
    (async () => {
      try {
        const d = await api.diag();
        const now = Number(d?.ledger?.ledgerEnd ?? NaN);
        const then = Number(window.localStorage.getItem(END_KEY) ?? NaN);
        if (!cancelled && Number.isFinite(now) && Number.isFinite(then) && now < then) {
          forgetCommittee();
          setErr('The ledger was reset since this committee was stood up, so it no longer exists. Stand up a new one.');
        }
      } catch {
        /* diag unavailable — leave the committee and let the next action decide */
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    try {
      const d = await api.diag();
      window.localStorage.setItem(END_KEY, String(d?.ledger?.ledgerEnd ?? ''));
    } catch {
      /* best effort */
    }
    flash(`Committee stood up — ${threshold}-of-${members.length} members must attest each fix.`);
  }

  async function propose() {
    if (!committeeCid) return;
    const proposer = members[0];
    const p = Number(price) || priceOf(instrumentId) || 0;

    // THREE DIFFERENT DAML CHOICES. The snapshot path is unchanged and still takes
    // exactly the arguments it always took; the accruing path attests three more; the
    // wrapped path attests a benchmark and a par factor and lets the ledger multiply.
    if (wrapped) {
      const b = benchmarkNum;
      const f = parFactorNum;
      const w = await run(() =>
        api.proposeWrappedFixing(committeeCid, {
          proposer,
          instrumentId,
          benchmarkPrice: b,
          parFactor: f,
          cashInstrument: CASH,
          session,
          rationale: `benchmark print ${b} (CME CF BRR-style, 16:00 London) × par factor ${f}`,
        }),
      );
      if (!w) return;
      setProposalCid(w.contractId);
      setAttestors([proposer]);
      setFixCid('');
      const bps = Number(w.discountBps);
      flash(
        `${proposer} proposed ${instrumentId} ${session}: ${b.toLocaleString()} × ${f} = ` +
          `${Number(w.strikePrice).toLocaleString()} — ${Math.abs(bps).toFixed(0)} bp ` +
          `${bps >= 0 ? 'below' : 'above'} par, struck on-ledger — 1 of ${threshold}.`,
      );
      return;
    }
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

  useEffect(() => {
    // Best-effort: an older backend has no /signer-protocol, and the panel must still
    // work on the plain Confirm path rather than breaking on a 404.
    api
      .signerProtocol()
      .then((p) => {
        setProtocolVersion(p.version);
        setSignerRoles(p.roles);
      })
      .catch(() => setSignerRoles([]));
  }, []);

  const activeRole = signerRoles.find((r) => r.key === signingRole);

  /** Open the evidence form for one member. Nothing is submitted until they attest. */
  function beginSigning(member: string) {
    setSigningMember(member);
    // DEFAULT THE SEAT FROM THE MEMBER, where the demo roster makes it obvious: the
    // Venue party is the venue seat, the Issuer the issuer, the Bank the lender.
    // A signer can still change it — but the protocol refuses a condition claimed by
    // the wrong seat, and a demo should not open on a form that is about to be refused.
    const guessed: Record<string, string> = { Venue: 'venue', Issuer: 'issuer', Bank: 'lender' };
    const guess = guessed[member];
    const known = guess && signerRoles.some((r) => r.key === guess) ? guess : undefined;
    setSigningRole(known ?? signerRoles[0]?.key ?? '');
    setTicked([]);
    setObsLow('');
    setObsHigh('');
  }

  /**
   * Attest WITH evidence. The member names the conditions it verified rather than
   * clicking yes to a price — the difference between an oversight record and a
   * signature count.
   */
  async function confirmWithChecks() {
    if (!proposalCid || !signingMember || !activeRole) return;
    const res = await run(() =>
      api.confirmFixingWithChecks(proposalCid, {
        member: signingMember,
        role: activeRole.key,
        checksPassed: ticked,
        ...(activeRole.requiresObservedRange
          ? { observedLow: obsLow.trim(), observedHigh: obsHigh.trim() }
          : {}),
      }),
    );
    if (!res) return;
    setProposalCid(res.contractId);
    const next = [...attestors, signingMember];
    setAttestors(next);
    flash(
      `${signingMember} attested as ${activeRole.key} (${ticked.length} condition(s)) — ${next.length} of ${threshold}.`,
    );
    setSigningMember('');
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
    flash(`Official ${strikeNoun} struck by ${attestors.length}-of-${members.length} — no single party could.`);
  }

  const enoughAttestors = attestors.length >= threshold;
  const pending = members.filter((m) => !attestors.includes(m));
  const suggested = priceOf(instrumentId);

  return (
    <section className="card committee" aria-label="Decentralised operator committee">
      <div className="card-head">
        <h2>Decentralised Operator</h2>
        <span className="who">K-of-N pricing committee</span>
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
          {committeeCid && (
            <button className="ghost small" disabled={busy} onClick={forgetCommittee} title="Forget this committee and stand up a new one">
              new committee
            </button>
          )}
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
              {closeOnly ? (
                <p className="hint attest-mode">
                  <strong>Close</strong> — a fund strikes one NAV a day. There is no opening NAV.
                </p>
              ) : (
                <div className="segmented session">
                  <button className={session === 'Open' ? 'on' : ''} disabled={busy} onClick={() => setSession('Open')}>
                    Open
                  </button>
                  <button className={session === 'Close' ? 'on' : ''} disabled={busy} onClick={() => setSession('Close')}>
                    Close
                  </button>
                </div>
              )}
            </div>
            {wrapped ? (
              <div className="field" style={{ flex: '1 1 100%' }}>
                <span>Benchmark × par factor ({CASH})</span>
                <div className="row tight">
                  <input
                    className="mono"
                    type="number"
                    min="0"
                    step="any"
                    placeholder={suggested != null ? String(suggested) : 'benchmark print'}
                    title="The underlying benchmark print — e.g. CME CF BRR at 16:00 London. Free, public, nobody argues."
                    value={benchmark}
                    disabled={busy}
                    onChange={(e) => setBenchmark(e.target.value)}
                  />
                  <span className="mono">×</span>
                  <input
                    className="mono"
                    type="number"
                    min="0"
                    max="2"
                    step="0.001"
                    title="The par factor — 1.000 attests the wrapper at par; 0.998 is 20 bp below. This is the product."
                    value={parFactor}
                    disabled={busy}
                    onChange={(e) => setParFactor(e.target.value)}
                  />
                </div>
                <p className="hint subtle">
                  = <strong className="mono">{wrappedStrike.toLocaleString(undefined, { maximumFractionDigits: 2 })}</strong>
                  {' '}· {Math.abs(wrappedBps).toFixed(0)} bp {wrappedBps >= 0 ? 'below' : 'above'} par ·
                  the ledger multiplies, so the factor cannot disagree with the price.
                </p>
              </div>
            ) : (
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
              {/* A CANDIDATE, not a price. Clicking this fills the box; the number
                  becomes official only when the threshold has signed it. */}
              {liveMark && (
                <button
                  type="button"
                  className="link live-mark"
                  disabled={busy}
                  title={`${liveMark.source} · ${liveMark.symbol} · ${liveMark.note}`}
                  onClick={() => setPrice(String(liveMark.price))}
                >
                  use {liveMark.symbol} {liveMark.price.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}{' '}
                  <span className="src">({liveMark.source})</span>
                </button>
              )}
            </label>
            )}
          </div>

          {/* WHAT IS BEING ATTESTED — derived from the instrument, stated not asked.
              It still decides which Daml choice runs; the operator simply no longer
              has to know that, because the instrument already does. */}
          <div className="field">
            <span>What is being attested</span>
            <p className="hint attest-mode">
              {wrapped ? (
                <>
                  <strong>A benchmark and a factor, signed apart.</strong> {instrumentId} is a
                  wrapper: its value is the benchmark print times the market&rsquo;s confidence
                  that redemption works. Marking it at par is an assertion, not a fact — so par
                  is a field the committee signs, and refusing it is something a seat can do.
                </>
              ) : accruing ? (
                <>
                  <strong>A recipe</strong> — base, rate and day count. {instrumentId} earns
                  continuously, so the ledger derives its value every second from here.
                </>
              ) : (
                <>
                  <strong>A mark</strong> — one number, true at this instant. {instrumentId} has
                  a market, so it is snapshotted rather than accrued.
                </>
              )}
            </p>
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
          ) : wrapped ? null : (
            <p className="hint subtle">
              A snapshot is true at one instant and stale at every instant after it. Correct
              for anything whose value is <em>discovered</em> — an equity, cETH, a volatile
              token — where accruing would be inventing data.
            </p>
          )}

          <button className="primary" disabled={busy} onClick={propose}>
            {members[0]} proposes the {session} {wrapped ? 'wrapped mark' : accruing ? 'accrual' : 'fix'}
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
              <span key={m} className="member-chip-pair">
                <button className="chip confirm" disabled={busy} onClick={() => confirm(m)}>
                  {m} confirms
                </button>
                {signerRoles.length > 0 && (
                  <button
                    className="chip"
                    disabled={busy}
                    title="Attest naming the conditions this seat verified"
                    onClick={() => beginSigning(m)}
                  >
                    {m} attests with evidence
                  </button>
                )}
              </span>
            ))}
          </div>

          {/* THE SIGNER PROTOCOL FORM.
              No member is asked whether it agrees with the price. Each is asked to
              assert a fact only its seat can see, which is what keeps an unpaid
              committee from decaying into a rubber stamp. */}
          {signingMember && activeRole && (
            <div className="signer-protocol">
              <div className="sp-head">
                <strong>{signingMember}</strong> attests ·{' '}
                <span className="subtle">{protocolVersion}</span>
              </div>

              <label className="sp-row">
                <span>Seat</span>
                <select
                  value={signingRole}
                  onChange={(e) => {
                    setSigningRole(e.target.value);
                    setTicked([]);
                  }}
                >
                  {signerRoles.map((r) => (
                    <option key={r.key} value={r.key}>
                      {r.title}
                    </option>
                  ))}
                </select>
              </label>

              <p className="subtle">
                What only this seat can see: <em>{activeRole.uniquelyKnows}</em>
              </p>

              <ul className="sp-checks">
                {activeRole.conditions.map((c) => (
                  <li key={c.name}>
                    <label>
                      <input
                        type="checkbox"
                        checked={ticked.includes(c.name)}
                        onChange={(e) =>
                          setTicked((t) =>
                            e.target.checked ? [...t, c.name] : t.filter((x) => x !== c.name),
                          )
                        }
                      />{' '}
                      <code>{c.name}</code>
                      <span className="subtle"> — {c.passesWhen}</span>
                    </label>
                  </li>
                ))}
              </ul>

              {activeRole.requiresObservedRange && (
                <div className="sp-range">
                  <label>
                    <span>Traded low</span>
                    <input value={obsLow} onChange={(e) => setObsLow(e.target.value)} />
                  </label>
                  <label>
                    <span>Traded high</span>
                    <input value={obsHigh} onChange={(e) => setObsHigh(e.target.value)} />
                  </label>
                  <p className="warn subtle">
                    Enforced on-ledger: a price outside this range is refused on-chain, not
                    here. The one seat with real transaction data cannot rubber-stamp.
                  </p>
                </div>
              )}

              <div className="member-chips">
                <button
                  className="primary"
                  disabled={busy || ticked.length === 0}
                  onClick={confirmWithChecks}
                >
                  {ticked.length === 0
                    ? 'Name at least one condition'
                    : `Attest as ${activeRole.key} · ${ticked.length} condition(s)`}
                </button>
                <button className="chip" disabled={busy} onClick={() => setSigningMember('')}>
                  Cancel
                </button>
              </div>
            </div>
          )}
          <button className="primary" disabled={busy || !enoughAttestors} onClick={finalize}>
            {enoughAttestors
              ? `Finalise · strike the official ${strikeNoun} (${attestors.length}-of-${members.length})`
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
          <div className="fix-badge">OFFICIAL {strikeNounCaps} · {attestors.length}-of-{members.length} attested</div>
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
            against this attested {strikeNoun}.
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
