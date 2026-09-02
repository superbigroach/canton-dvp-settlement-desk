package com.lucilla.settlement.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a signer configures in its portal (docs/PRODUCT-PLAN.md §5, {@code /api/signer/settings}).
 * Mutable bean so it round-trips through Jackson and the file store.
 */
public class SignerSettings {
    private String webhookUrl;
    private String webhookSecret;
    private String email;
    private Map<String, Object> tolerances = new LinkedHashMap<>();

    public SignerSettings() {}

    public SignerSettings copy() {
        SignerSettings s = new SignerSettings();
        s.webhookUrl = webhookUrl;
        s.webhookSecret = webhookSecret;
        s.email = email;
        s.tolerances = tolerances == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tolerances);
        return s;
    }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Map<String, Object> getTolerances() { return tolerances; }
    public void setTolerances(Map<String, Object> tolerances) {
        this.tolerances = tolerances == null ? new LinkedHashMap<>() : tolerances;
    }
}
