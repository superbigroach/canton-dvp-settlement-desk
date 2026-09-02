package com.lucilla.settlement.signing;

import com.lucilla.settlement.auth.ApiKeys;
import com.lucilla.settlement.auth.CurrentUser;
import com.lucilla.settlement.auth.Principal;
import com.lucilla.settlement.auth.SignerSettings;
import com.lucilla.settlement.auth.UserRecord;
import com.lucilla.settlement.auth.UserStore;
import com.lucilla.settlement.ledger.SignerProtocol;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code /api/signer/settings} and {@code /api/signer/apikey} — docs/PRODUCT-PLAN.md §5.
 *
 * <p>The webhook secret is write-only: {@code GET} says whether one is set and never
 * returns it. The API key is shown exactly once, by the {@code POST} that minted it.
 */
@RestController
public class SignerSettingsController {

    private final UserStore users;

    public SignerSettingsController(UserStore users) {
        this.users = users;
    }

    @GetMapping("/api/signer/settings")
    public Map<String, Object> get(HttpServletRequest req) {
        UserRecord u = user(req);
        return view(u);
    }

    /** Partial update: absent fields are left alone; a blank secret keeps the existing one. */
    @PutMapping("/api/signer/settings")
    public Map<String, Object> put(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        UserRecord u = user(req);
        SignerSettings s = u.getSettings() == null ? new SignerSettings() : u.getSettings();
        if (body.containsKey("webhookUrl")) {
            Object v = body.get("webhookUrl");
            String url = v == null ? null : v.toString().trim();
            if (url != null && !url.isBlank() && !(url.startsWith("https://") || url.startsWith("http://"))) {
                throw new IllegalArgumentException("webhookUrl must be an http(s) URL");
            }
            s.setWebhookUrl(url == null || url.isBlank() ? null : url);
        }
        if (body.containsKey("webhookSecret")) {
            Object v = body.get("webhookSecret");
            if (v != null && !v.toString().isBlank()) s.setWebhookSecret(v.toString());
        }
        if (body.containsKey("email")) {
            Object v = body.get("email");
            s.setEmail(v == null || v.toString().isBlank() ? null : v.toString().trim());
        }
        if (body.containsKey("tolerances")) {
            Object v = body.get("tolerances");
            if (v instanceof Map<?, ?> m) {
                Map<String, Object> t = new LinkedHashMap<>();
                m.forEach((k, val) -> t.put(String.valueOf(k), val));
                s.setTolerances(t);
            } else if (v == null) {
                s.setTolerances(new LinkedHashMap<>());
            } else {
                throw new IllegalArgumentException("tolerances must be an object");
            }
        }
        u.setSettings(s);
        return view(users.save(u));
    }

    @PostMapping("/api/signer/apikey")
    public Map<String, Object> mintKey(HttpServletRequest req) {
        UserRecord u = user(req);
        String key = ApiKeys.generate();
        u.setApiKeyHash(ApiKeys.hash(key));
        u.setApiKeyPrefix(key.substring(0, 8));
        u.setApiKeyCreatedAt(Instant.now().toString());
        users.save(u);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("key", key);
        out.put("createdAt", u.getApiKeyCreatedAt());
        out.put("prefix", u.getApiKeyPrefix());
        out.put("note", "shown once; send it as Authorization: Bearer " + ApiKeys.PREFIX + "…");
        return out;
    }

    @DeleteMapping("/api/signer/apikey")
    public Map<String, Object> revokeKey(HttpServletRequest req) {
        UserRecord u = user(req);
        boolean had = u.getApiKeyHash() != null;
        u.setApiKeyHash(null);
        u.setApiKeyPrefix(null);
        u.setApiKeyCreatedAt(null);
        users.save(u);
        return Map.of("revoked", had);
    }

    private UserRecord user(HttpServletRequest req) {
        Principal p = CurrentUser.require(req);
        return users.byUid(p.uid()).orElseThrow(() ->
                new com.lucilla.settlement.auth.AuthException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "settings belong to a mapped user; the anonymous sandbox operator has none"));
    }

    private static Map<String, Object> view(UserRecord u) {
        SignerSettings s = u.getSettings() == null ? new SignerSettings() : u.getSettings();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("webhookUrl", s.getWebhookUrl());
        out.put("webhookSecret", null);
        out.put("webhookSecretSet", s.getWebhookSecret() != null && !s.getWebhookSecret().isBlank());
        out.put("email", s.getEmail() == null ? u.getEmail() : s.getEmail());
        out.put("tolerances", s.getTolerances());
        out.put("apiKeySet", u.getApiKeyHash() != null);
        out.put("apiKeyCreatedAt", u.getApiKeyCreatedAt());
        // frontend/src/desk/types.ts SignerSettings.apiKey
        out.put("apiKey", u.getApiKeyHash() == null ? null
                : Map.of("createdAt", u.getApiKeyCreatedAt() == null ? "" : u.getApiKeyCreatedAt(),
                        "prefix", u.getApiKeyPrefix() == null ? ApiKeys.PREFIX : u.getApiKeyPrefix()));
        out.put("seat", u.getSeat());
        out.put("instruments", u.getInstruments());
        SignerProtocol.Role r = SignerProtocol.role(u.getSeat());
        out.put("conditions", r == null ? java.util.List.of()
                : r.conditions().stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.name());
                    m.put("passesWhen", c.passesWhen());
                    m.put("evidence", com.lucilla.settlement.ledger.SignerEvidence.schemaOf(c.evidence()));
                    return m;
                }).toList());
        out.put("requiresEvidence", r != null && r.requiresEvidence());
        // What the lender's evidence is judged against; absent keys mean the defaults.
        out.put("toleranceDefaults", Map.of(SignerProtocol.TOLERANCE_MARK_KEY, SignerProtocol.DEFAULT_TOLERANCE_BPS,
                SignerProtocol.TOLERANCE_LIQUIDATION_KEY, SignerProtocol.DEFAULT_TOLERANCE_BPS));
        return out;
    }
}
