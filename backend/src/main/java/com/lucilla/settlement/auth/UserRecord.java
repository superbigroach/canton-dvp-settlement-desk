package com.lucilla.settlement.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * One row of the user → role + party mapping (docs/PRODUCT-PLAN.md §3), as stored.
 *
 * <p>{@code apiKeyHash} is the SHA-256 of the key handed out once at
 * {@code POST /api/signer/apikey}; the key itself is never kept. Mutable bean so the
 * YAML seed, the JSON persistence file and the admin endpoints all read and write the
 * same shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRecord {
    private String uid;
    private String email;
    private String role = "viewer";
    private String party;
    private String seat;
    private List<String> instruments = new ArrayList<>();
    private String org;
    private String displayName;
    private String apiKeyHash;
    private String apiKeyPrefix;      // the first characters, so a portal can show WHICH key
    private String apiKeyCreatedAt;
    private SignerSettings settings = new SignerSettings();

    public UserRecord() {}

    public UserRecord copy() {
        UserRecord u = new UserRecord();
        u.uid = uid; u.email = email; u.role = role; u.party = party; u.seat = seat;
        u.instruments = instruments == null ? new ArrayList<>() : new ArrayList<>(instruments);
        u.org = org; u.displayName = displayName; u.apiKeyHash = apiKeyHash;
        u.apiKeyPrefix = apiKeyPrefix; u.apiKeyCreatedAt = apiKeyCreatedAt;
        u.settings = settings == null ? new SignerSettings() : settings.copy();
        return u;
    }

    public Role roleEnum() {
        return Role.parse(role);
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }
    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }
    public List<String> getInstruments() { return instruments; }
    public void setInstruments(List<String> instruments) {
        this.instruments = instruments == null ? new ArrayList<>() : instruments;
    }
    public String getOrg() { return org; }
    public void setOrg(String org) { this.org = org; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public String getApiKeyPrefix() { return apiKeyPrefix; }
    public void setApiKeyPrefix(String apiKeyPrefix) { this.apiKeyPrefix = apiKeyPrefix; }
    public String getApiKeyCreatedAt() { return apiKeyCreatedAt; }
    public void setApiKeyCreatedAt(String apiKeyCreatedAt) { this.apiKeyCreatedAt = apiKeyCreatedAt; }
    public SignerSettings getSettings() { return settings; }
    public void setSettings(SignerSettings settings) {
        this.settings = settings == null ? new SignerSettings() : settings;
    }
}
