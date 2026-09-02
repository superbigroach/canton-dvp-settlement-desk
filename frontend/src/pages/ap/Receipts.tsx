// AP portal · Receipts — every creation and redemption I have done.
import { desk, type Receipt } from '../../desk';
import { fmtN, fmtQty, fmtTs, isOfficial, LoadState, shortCid, useAsync } from '../../components/ui';

export function ReceiptTable({ rows, showParty }: { rows: Receipt[]; showParty?: boolean }) {
  return (
    <div className="card table-wrap">
      <table className="blotter">
        <thead>
          <tr><th>When</th>{showParty && <th>Party</th>}<th>Fund</th><th>Kind</th><th className="num">Shares</th><th className="num">NAV</th><th>Units</th><th className="num">Fee</th><th>Status</th><th>cid</th></tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td className="mono when">{fmtTs(r.ts)}</td>
              {showParty && <td>{r.party ?? '—'}</td>}
              <td>{r.fundId}</td>
              <td><span className={`tag status ${r.kind}`}>{r.kind}</span></td>
              <td className="num mono">{fmtQty(r.shares)}</td>
              <td className={`num mono${isOfficial(r.navTier) ? ' official' : ''}`}>{fmtN(r.nav)}</td>
              <td className="mono small">{r.units.map((u) => `${fmtQty(u.amount)} ${u.instrumentId}`).join(', ')}</td>
              <td className="num mono">{fmtN(r.fee)}{r.feeCurrency ? ` ${r.feeCurrency}` : ''}</td>
              <td><span className={`tag status ${r.status}`}>{r.status}</span></td>
              <td className="mono muted" title={r.cid}>{shortCid(r.cid)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function Receipts() {
  const list = useAsync<Receipt[]>(() => desk.apReceipts(), []);
  return (
    <div className="page">
      <div className="page-head">
        <h1>Receipts</h1>
        <button type="button" className="ghost small" onClick={list.reload}>Refresh</button>
      </div>
      <LoadState loading={list.loading} error={list.error} onRetry={list.reload}
        empty={list.data && list.data.length === 0 ? 'No creations or redemptions yet.' : null}>
        <ReceiptTable rows={list.data ?? []} />
      </LoadState>
    </div>
  );
}
