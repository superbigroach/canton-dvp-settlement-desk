// PendingTransfersPanel — CLAIMING AN ASSET THIS PROJECT DID NOT ISSUE.
//
// The judges' criticism of this project was that cETH and cBTC are self-issued
// stand-ins. The answer is not an argument, it is a balance: an instrument
// administered by a registry we did not write, held by a party we control.
//
// A transfer from such a registry — BitSafe's CBTC faucet, for instance — does not
// land as a balance. It lands as a PENDING TransferInstruction addressed to the
// receiver, and until the receiver accepts it, it is not a holding and shows up in no
// balance anywhere. That is not a quirk of the faucet; it is the standard's two-step
// transfer path, and the accept is the receiver's own decision by design.
//
// So this panel does one thing, calmly: it shows what is waiting, says whose registry
// it came from, and accepts it.

import { useCallback, useEffect, useState } from 'react';
import {
  api,
  errorMessage,
  type InstructionOutcome,
  type PendingTransfer,
  type PendingTransfersResponse,
} from './api';

interface Props {
  /** The desk's acting party (a label). Used as the default; overridable below. */
  acting: string;
  /** Refresh the desk's holdings after an accept — the whole point is that they change. */
  onChanged: () => void;
  flash: (m: string) => void;
}

const fmt = (n: number) =>
  n.toLocaleString(undefined, { maximumFractionDigits: 10 });

/** "alice-crossdesk::1220ab…" -> "alice-crossdesk". Full ids are unreadable on a projector. */
function label(party: string): string {
  const i = party.indexOf('::');
  return i > 0 ? party.slice(0, i) : party;
}

function when(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}

const fmtAmt = (n: number) =>
  n.toLocaleString(undefined, { maximumFractionDigits: 6 });

export default function PendingTransfersPanel({ acting, onChanged, flash }: Props) {
  // The party to query. Defaults to the desk's acting party, but is EDITABLE on
  // purpose: a foreign transfer is addressed to a party id somebody else typed into a
  // faucet, and it must be possible to paste that id verbatim — including its
  // ::namespace suffix — rather than hope a label happens to match.
  const [party, setParty] = useState<string>(acting);
  const [direction, setDirection] = useState<'inbound' | 'all'>('inbound');
  const [rows, setRows] = useState<PendingTransfer[]>([]);
  const [note, setNote] = useState<string>('');
  const [outcome, setOutcome] = useState<InstructionOutcome | null>(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>('');
  const [loaded, setLoaded] = useState(false);

  // Holdings are summed PER INSTRUMENT: a party's position is spread across many
  // Holding contracts (reservations split them), so a raw list would show one asset
  // five times and read as a bug.
  const [balances, setBalances] = useState<{ instrumentId: string; amount: number }[]>([]);
  const [balancesLoading, setBalancesLoading] = useState(false);

  const loadBalances = useCallback(async (who: string) => {
    if (!who) {
      setBalances([]);
      return;
    }
    setBalancesLoading(true);
    try {
      const hs = await api.holdings(who);
      const byInstrument = new Map<string, number>();
      for (const h of hs) {
        byInstrument.set(h.instrumentId, (byInstrument.get(h.instrumentId) ?? 0) + Number(h.amount));
      }
      setBalances(
        [...byInstrument.entries()]
          .map(([instrumentId, amount]) => ({ instrumentId, amount }))
          .sort((a, b) => a.instrumentId.localeCompare(b.instrumentId)),
      );
    } catch {
      // A party the ledger does not know is a normal state here (the field is free
      // text), not something to shout about — the pending list already says so.
      setBalances([]);
    } finally {
      setBalancesLoading(false);
    }
  }, []);

  useEffect(() => {
    // Follow the desk's party picker unless the operator has typed their own id.
    setParty((current) => (current === '' ? acting : current));
  }, [acting]);

  const load = useCallback(
    async (who: string, dir: 'inbound' | 'all') => {
      if (!who) {
        setRows([]);
        setNote('');
        return;
      }
      setBusy(true);
      setErr('');
      try {
        const res: PendingTransfersResponse = await api.pendingTransfers(who, dir);
        setRows(res.pending);
        setNote(res.note ?? '');
        setLoaded(true);
      } catch (e) {
        setErr(errorMessage(e));
      } finally {
        setBusy(false);
      }
    },
    [],
  );

  useEffect(() => {
    void load(party, direction);
    void loadBalances(party);
    // Re-read when the party or direction changes; no polling — this is a ledger read.
  }, [party, direction, load]);

  async function act(row: PendingTransfer, choice: 'accept' | 'reject' | 'withdraw') {
    setBusy(true);
    setErr('');
    setOutcome(null);
    try {
      const res =
        choice === 'accept'
          ? await api.acceptTransfer(party, row.instructionCid)
          : choice === 'reject'
            ? await api.rejectTransfer(party, row.instructionCid)
            : await api.withdrawTransfer(party, row.instructionCid);
      setOutcome(res);
      flash(
        choice === 'accept'
          ? `Accepted ${fmt(row.amount)} ${row.instrumentId} from ${label(row.sender)}`
          : `${choice === 'reject' ? 'Rejected' : 'Withdrew'} ${fmt(row.amount)} ${row.instrumentId}`,
      );
      onChanged();
      await Promise.all([load(party, direction), loadBalances(party)]);
    } catch (e) {
      setErr(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const inbound = rows.filter((r) => r.direction === 'inbound');
  const foreign = rows.filter((r) => !r.ourRegistry);

  return (
    <section className="card pending-transfers">
      <h2>Pending transfers · CIP-56</h2>
      <p className="muted">
        A transfer from another registry arrives as a pending <code>TransferInstruction</code>,
        not as a balance. It becomes a real holding only when the receiver accepts it.
        {foreign.length > 0 && (
          <>
            {' '}
            <strong>
              {foreign.length} of these {foreign.length === 1 ? 'is' : 'are'} administered by a
              registry this project did not write.
            </strong>
          </>
        )}
      </p>

      <div className="row">
        <label>
          Party
          <input
            value={party}
            onChange={(e) => setParty(e.target.value.trim())}
            placeholder="Alice, or alice-crossdesk::1220…"
            size={46}
          />
        </label>
        <label>
          Show
          <select
            value={direction}
            onChange={(e) => setDirection(e.target.value as 'inbound' | 'all')}
          >
            <option value="inbound">Inbound (claimable)</option>
            <option value="all">All (incl. my outgoing)</option>
          </select>
        </label>
        <button className="ghost" disabled={busy} onClick={() => { void load(party, direction); void loadBalances(party); }}>
          Refresh
        </button>
      </div>

      {/* WHAT THIS PARTY ACTUALLY HOLDS, right where transfers are accepted.
          Accepting a CIP-56 transfer is the moment a foreign registry's asset becomes
          yours, and without a balance in view the click has no visible consequence —
          you are asked to believe a row disappeared for a good reason. This is also
          where the real BitSafe cBTC shows up, which is the claim worth being able to
          point at rather than assert. */}
      <div className="party-balances">
        <div className="pb-head">
          <span>Balances · {label(party) || '—'}</span>
          {balancesLoading && <span className="muted">loading…</span>}
        </div>
        {balances.length === 0 ? (
          <p className="muted">
            {balancesLoading ? '' : `${label(party) || 'This party'} holds nothing yet.`}
          </p>
        ) : (
          <div className="pb-chips">
            {balances.map((b) => (
              <span className="pb-chip" key={b.instrumentId}>
                <span className="mono strong">{fmtAmt(b.amount)}</span>{' '}
                <span className="pb-inst">{b.instrumentId}</span>
              </span>
            ))}
          </div>
        )}
      </div>

      {err && <p className="error">{err}</p>}

      {loaded && rows.length === 0 && (
        <p className="muted">
          Nothing pending for {label(party) || '—'}.
          {note && <> {note}</>}
        </p>
      )}

      {rows.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Amount</th>
              <th>Instrument</th>
              <th>From</th>
              <th>Registry</th>
              <th>Status</th>
              <th>Expires</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.instructionCid}>
                <td>
                  <strong>{fmt(r.amount)}</strong>
                </td>
                <td>{r.instrumentId}</td>
                <td>{label(r.direction === 'outbound' ? r.receiver : r.sender)}</td>
                <td>
                  {r.ourRegistry ? (
                    <span className="muted">this desk</span>
                  ) : (
                    // The interesting case. Say whose it is, and whether we know how to
                    // reach their off-ledger registry if the accept turns out to need it.
                    <span title={r.instrumentAdmin}>
                      {label(r.instrumentAdmin)}
                      {r.registryUrl ? '' : ' · no registry URL configured'}
                    </span>
                  )}
                </td>
                <td>{r.expired ? <span className="error">expired</span> : r.status}</td>
                <td className="muted">{when(r.executeBefore)}</td>
                <td>
                  {r.direction === 'inbound' ? (
                    <>
                      <button
                        disabled={busy || r.expired}
                        onClick={() => void act(r, 'accept')}
                        title={
                          r.expired
                            ? 'this instruction is past its executeBefore and can no longer be accepted'
                            : 'exercise TransferInstruction_Accept as the receiver'
                        }
                      >
                        Accept
                      </button>{' '}
                      <button
                        className="ghost"
                        disabled={busy}
                        onClick={() => void act(r, 'reject')}
                      >
                        Reject
                      </button>
                    </>
                  ) : r.direction === 'outbound' ? (
                    <button
                      className="ghost"
                      disabled={busy}
                      onClick={() => void act(r, 'withdraw')}
                    >
                      Withdraw
                    </button>
                  ) : (
                    <span className="muted">observer</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {outcome && (
        <div className="muted" style={{ marginTop: '0.75rem' }}>
          <div>
            <strong>{outcome.choice}</strong> · update {outcome.updateId}
          </div>
          {outcome.created.length > 0 && (
            <div>
              Created on the ledger:{' '}
              {outcome.created.map((c) => (
                <div key={c}>
                  <code>{c}</code>
                </div>
              ))}
            </div>
          )}
          {/* Reported deliberately: accepting WITH a registry's context and accepting
              with an empty one are different claims. */}
          <div>Choice context: {outcome.choiceContext.source}</div>
          {outcome.choiceContext.disclosedContracts > 0 && (
            <div>
              {outcome.choiceContext.disclosedContracts} disclosed contract(s) attached to the
              submission.
            </div>
          )}
        </div>
      )}

      {inbound.length > 0 && (
        <p className="muted">
          Accepting exercises the standard's own <code>TransferInstruction_Accept</code> on the
          issuing registry's contract. Nothing in this project's Daml code is involved — that is
          what makes the resulting holding a genuine third-party asset rather than a stand-in.
        </p>
      )}
    </section>
  );
}
