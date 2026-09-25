// Signer portal · Open proposals.
import { useAuth } from '../../auth/AuthContext';
import { desk, type Proposal } from '../../desk';
import { LoadState, useAsync } from '../../components/ui';
import ProposalCard from './ProposalCard';
import SeatGuide from './SeatGuide';
import { useSignerRole } from './useSignerRole';

export default function Proposals() {
  const { me } = useAuth();
  const { role, error: protoError } = useSignerRole();
  const list = useAsync<Proposal[]>(() => desk.proposals('open'), []);

  const replace = (next: Proposal) =>
    list.setData((prev) => (prev ? prev.map((p) => (p.cid === next.cid ? next : p)) : prev));

  const open = (list.data ?? []).filter((p) => p.status === 'open' || p.mine);

  return (
    <div className="page">
      <div className="page-head">
        <h1>Open proposals</h1>
        <p className="hint">
          {me?.seat ? <>Your seat: <strong>{me.seat}</strong>. </> : null}
          {me?.instruments?.length ? <>Instruments: <span className="mono">{me.instruments.join(', ')}</span>. </> : null}
          Each card lists the conditions your seat verifies. Tick what you checked; the ledger records exactly that.
        </p>
        <button type="button" className="ghost small" onClick={list.reload}>Refresh</button>
      </div>
      {protoError && <div className="banner warn" role="status"><span>Signer protocol not loaded — {protoError}. Conditions come from the proposal itself.</span></div>}
      {/* A background reload (after a confirm or a refusal the ledger answered) must not unmount
          the cards: before 24 Sep 2026 `loading` alone swapped the whole list for "Loading…", so
          a 422 the card had just rendered was wiped by its own onRefresh before anyone read it. */}
      <LoadState loading={list.loading && !list.data} error={list.error} onRetry={list.reload}
        empty={list.data && open.length === 0 ? 'Nothing waiting for your signature. Proposals appear here at the strike time and by webhook/email.' : null}>
        <div className="proposal-list">
          {open.map((p) => <ProposalCard key={p.cid} proposal={p} role={role} onChanged={replace} onRefresh={list.reload} />)}
        </div>
      </LoadState>
      {/* THE JOB FIRST, THE MANUAL AFTER. The guide used to sit here, above the list, so a
          signer with a proposal waiting scrolled past the whole reference section to reach
          it. It is collapsed by default now and lives below the work. */}
      <SeatGuide />
    </div>
  );
}
