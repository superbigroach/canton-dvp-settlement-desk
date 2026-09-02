import type { SignerRole } from '../../api';
import { useAuth } from '../../auth/AuthContext';
import { desk } from '../../desk';
import { useAsync } from '../../components/ui';

/** My seat's protocol entry, from /api/signer-protocol — the one source of condition names. */
export function useSignerRole(): { role: SignerRole | null; error: string | null; loading: boolean } {
  const { me } = useAuth();
  const proto = useAsync(() => desk.signerProtocol(), []);
  const role = proto.data?.roles.find((r) => r.key === me?.seat) ?? null;
  return { role, error: proto.error, loading: proto.loading };
}
