// The sandbox identities — MIRRORS backend users.yml (docs/PRODUCT-PLAN.md §3).
//
// Offered on the login page only when the build runs with VITE_AUTH_MODE=sandbox. The
// backend is the authority on role and party: `/api/me` wins whenever it answers. This
// list exists so the app can be developed before the identity routes land, and so the
// login page can name the seats a developer may pick from.
import type { Me, Role, Seat } from '../desk/types';

export interface SandboxUser extends Me {
  note: string;
}

const u = (
  email: string, role: Role, party: string, org: string, displayName: string, note: string,
  seat?: Seat, instruments?: string[],
): SandboxUser => ({
  uid: `sandbox:${email}`, email, role, party, org, displayName, note, seat, instruments,
});

export const SANDBOX_USERS: SandboxUser[] = [
  u('s.borjas@lucilla.ca', 'admin', 'Operator', 'CrossDesk', 'S. Borjas', 'admin · operator desk, schedule, users'),
  u('issuer@sandbox.crossdesk', 'signer', 'Issuer', 'Issuer', 'Issuer signer', 'signer · issuer seat', 'issuer', ['CBTC', 'cETH', 'LX1']),
  u('lender@sandbox.crossdesk', 'signer', 'Bank', 'Bank', 'Lender signer', 'signer · lender seat', 'lender', ['CBTC', 'cETH', 'LX1']),
  u('venue@sandbox.crossdesk', 'signer', 'Venue', 'Venue', 'Venue signer', 'signer · venue seat (traded range)', 'venue', ['CBTC', 'cETH']),
  u('alice@sandbox.crossdesk', 'ap', 'Alice', 'Alice Capital', 'Alice', 'authorised participant'),
  u('bob@sandbox.crossdesk', 'ap', 'Bob', 'Bob Markets', 'Bob', 'authorised participant'),
  u('fund@sandbox.crossdesk', 'fund_admin', 'Issuer', 'LX1 Fund', 'Fund administrator', 'fund admin · LX1'),
  u('auditor@sandbox.crossdesk', 'auditor', 'Auditor', 'Auditor', 'Auditor', 'read-only events and series'),
];

export const findSandboxUser = (email: string | null): SandboxUser | undefined =>
  email ? SANDBOX_USERS.find((s) => s.email.toLowerCase() === email.toLowerCase()) : undefined;
