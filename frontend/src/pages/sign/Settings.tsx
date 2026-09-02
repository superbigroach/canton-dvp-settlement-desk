// Signer portal · Settings — where notifications go, what my automated signer tolerates,
// and the API key an automated signer authenticates with (shown once).
import { useEffect, useState, type FormEvent } from 'react';
import { errorMessage } from '../../api';
import { desk, type SignerSettings } from '../../desk';
import { ConfirmDialog, fmtTs, LoadState, useAsync } from '../../components/ui';

const TOLERANCES: { key: string; label: string; hint: string }[] = [
  { key: 'maxDeviationBps', label: 'Max deviation from benchmark × factor (bps)', hint: 'Above this an automated signer refuses rather than confirms.' },
  { key: 'maxAgeSeconds', label: 'Max age of the reference print (seconds)', hint: 'A stale benchmark input is a reason to refuse.' },
];

export default function Settings() {
  const loaded = useAsync<SignerSettings>(() => desk.signerSettings(), []);
  const [form, setForm] = useState<SignerSettings | null>(null);
  const [secret, setSecret] = useState('');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [newKey, setNewKey] = useState<string | null>(null);
  const [keyBusy, setKeyBusy] = useState(false);
  const [keyError, setKeyError] = useState<string | null>(null);
  const [confirmRevoke, setConfirmRevoke] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => { if (loaded.data && !form) setForm(loaded.data); }, [loaded.data, form]);

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!form) return;
    setSaving(true); setError(null); setSaved(null);
    try {
      const body: SignerSettings = { ...form, webhookSecret: secret || undefined };
      const r = await desk.saveSignerSettings(body);
      setForm({ ...r, apiKey: r.apiKey ?? form.apiKey, webhookSecretSet: r.webhookSecretSet ?? (secret ? true : form.webhookSecretSet) });
      setSecret('');
      setSaved(`Saved at ${new Date().toLocaleTimeString()}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const generate = async () => {
    setKeyBusy(true); setKeyError(null); setCopied(false);
    try {
      const r = await desk.createApiKey();
      setNewKey(r.key);
      setForm((f) => (f ? { ...f, apiKey: { createdAt: new Date().toISOString(), prefix: r.key.slice(0, 8) } } : f));
    } catch (err) {
      setKeyError(errorMessage(err));
    } finally {
      setKeyBusy(false);
    }
  };

  const revoke = async () => {
    setKeyBusy(true); setKeyError(null);
    try {
      await desk.revokeApiKey();
      setNewKey(null);
      setForm((f) => (f ? { ...f, apiKey: null } : f));
      setConfirmRevoke(false);
    } catch (err) {
      setKeyError(errorMessage(err));
    } finally {
      setKeyBusy(false);
    }
  };

  const copy = async () => {
    if (!newKey) return;
    try { await navigator.clipboard.writeText(newKey); setCopied(true); } catch { setCopied(false); }
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>Settings</h1>
        <p className="hint">Where CrossDesk notifies your seat, what your automated signer tolerates, and the key it signs with.</p>
      </div>
      <LoadState loading={loaded.loading && !form} error={loaded.error} onRetry={loaded.reload}>
        {form && (
          <div className="two-col">
            <form className="card" onSubmit={(e) => void save(e)}>
              <h2>Notifications</h2>
              <p className="hint subtle">
                Every proposal, restrike, finalisation and miss is POSTed to your webhook, signed with
                <code> X-CrossDesk-Signature: sha256=HMAC(secret, body)</code>, and emailed.
              </p>
              <label className="field" htmlFor="webhookUrl">
                <span>Webhook URL</span>
                <input id="webhookUrl" type="url" className="mono" placeholder="https://signer.example.com/crossdesk" value={form.webhookUrl ?? ''}
                  onChange={(e) => setForm({ ...form, webhookUrl: e.target.value })} />
              </label>
              <label className="field" htmlFor="webhookSecret">
                <span>Webhook secret {form.webhookSecretSet ? '(stored, never shown — enter to replace)' : '(none stored)'}</span>
                <input id="webhookSecret" type="password" className="mono" autoComplete="new-password" value={secret}
                  placeholder={form.webhookSecretSet ? 'leave blank to keep the current secret' : 'shared secret for the HMAC signature'} onChange={(e) => setSecret(e.target.value)} />
              </label>
              <label className="field" htmlFor="email">
                <span>Notification email</span>
                <input id="email" type="email" value={form.email ?? ''} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </label>

              <h2 className="section-h">Tolerances</h2>
              {TOLERANCES.map((t) => (
                <label key={t.key} className="field" htmlFor={`tol-${t.key}`}>
                  <span>{t.label}</span>
                  <input id={`tol-${t.key}`} type="number" className="mono" inputMode="numeric" min={0}
                    value={form.tolerances?.[t.key] ?? ''}
                    onChange={(e) => setForm({ ...form, tolerances: { ...form.tolerances, [t.key]: Number(e.target.value) } })} />
                  <small className="field-hint">{t.hint}</small>
                </label>
              ))}
              {Object.keys(form.tolerances ?? {}).filter((k) => !TOLERANCES.some((t) => t.key === k)).map((k) => (
                <label key={k} className="field" htmlFor={`tol-${k}`}>
                  <span>{k}</span>
                  <input id={`tol-${k}`} type="number" className="mono" value={form.tolerances[k]}
                    onChange={(e) => setForm({ ...form, tolerances: { ...form.tolerances, [k]: Number(e.target.value) } })} />
                </label>
              ))}
              <div className="proposal-actions">
                <button type="submit" className="primary" disabled={saving}>{saving ? 'Saving…' : 'Save settings'}</button>
                {saved && <span className="good mono small">{saved}</span>}
              </div>
              {error && <div className="banner error" role="alert"><span>{error}</span></div>}
            </form>

            <div className="card">
              <h2>API key</h2>
              <p className="hint subtle">
                An automated signer sends <code>Authorization: Bearer ck_…</code>. The key is shown ONCE, here, and stored hashed.
                Generating a new one replaces the old one.
              </p>
              {newKey ? (
                <div className="apikey">
                  <div className="banner warn"><span>Copy this now. It will not be shown again.</span></div>
                  <code className="apikey-value mono">{newKey}</code>
                  <div className="proposal-actions">
                    <button type="button" className="ghost" onClick={() => void copy()}>{copied ? 'Copied' : 'Copy'}</button>
                    <button type="button" className="ghost" onClick={() => setNewKey(null)}>Done, I have it</button>
                  </div>
                </div>
              ) : form.apiKey ? (
                <p className="hint">
                  A key starting <code className="mono">{form.apiKey.prefix}…</code> exists, created {fmtTs(form.apiKey.createdAt)}.
                </p>
              ) : (
                <p className="hint">No API key. The portal alone (this page) needs none.</p>
              )}
              <div className="proposal-actions">
                <button type="button" className="primary" disabled={keyBusy} onClick={() => void generate()}>
                  {keyBusy ? 'Working…' : form.apiKey ? 'Generate a new key' : 'Generate API key'}
                </button>
                {form.apiKey && (
                  <button type="button" className="ghost" disabled={keyBusy} onClick={() => setConfirmRevoke(true)}>Revoke</button>
                )}
              </div>
              {keyError && <div className="banner error" role="alert"><span>{keyError}</span></div>}
            </div>
          </div>
        )}
      </LoadState>
      <ConfirmDialog open={confirmRevoke} title="Revoke the API key?" confirmLabel="Revoke" danger busy={keyBusy}
        onConfirm={() => void revoke()} onCancel={() => setConfirmRevoke(false)}>
        <p>Any automated signer using it stops being able to confirm. Proposals then wait for this portal.</p>
      </ConfirmDialog>
    </div>
  );
}
