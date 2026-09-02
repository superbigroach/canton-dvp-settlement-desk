package com.lucilla.settlement.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUserStoreTest {

    @Test
    void seedLoadsTheSandboxMappingFromClasspath() {
        FileUserStore s = new FileUserStore("classpath:users.yml", null);
        assertTrue(s.byEmail("s.borjas@lucilla.ca").isPresent());
        assertEquals("admin", s.byEmail("s.borjas@lucilla.ca").get().getRole());
        assertEquals("Issuer", s.byEmail("s.borjas@lucilla.ca").get().getParty());
        // §3: Issuer → signer/issuer, Bank → signer/lender, Venue → signer/venue,
        // Alice & Bob → ap, Auditor → auditor, admin@crossdesk → admin.
        assertEquals("issuer", s.bySandboxReference("Issuer").get().getSeat());
        assertEquals("lender", s.bySandboxReference("bank").get().getSeat());
        assertEquals("venue", s.bySandboxReference("Venue").get().getSeat());
        assertEquals("ap", s.bySandboxReference("alice").get().getRole());
        assertEquals("ap", s.bySandboxReference("Bob").get().getRole());
        assertEquals("auditor", s.bySandboxReference("Auditor").get().getRole());
        assertEquals("admin", s.bySandboxReference("s.borjas@lucilla.ca").get().getRole());
        assertEquals("lender", s.bySandboxReference("lender@sandbox.crossdesk").get().getSeat());
        assertEquals("fund_admin", s.bySandboxReference("fund@sandbox.crossdesk").get().getRole());
        assertEquals("Bank", s.bySandboxReference("fund").get().getParty());
        assertTrue(s.bySandboxReference("eve").isEmpty(), "the outsider maps to nobody");
    }

    @Test
    void editsPersistAndOverlayTheSeed(@TempDir Path dir) {
        FileUserStore a = new FileUserStore("classpath:users.yml", dir);
        UserRecord alice = a.byEmail("alice@sandbox.crossdesk").orElseThrow();
        alice.getSettings().setWebhookUrl("https://alice.example/hook");
        alice.setApiKeyHash(ApiKeys.hash("ck_x"));
        a.save(alice);
        UserRecord fresh = new UserRecord();
        fresh.setEmail("New.Person@Example.com");
        fresh.setRole("viewer");
        UserRecord saved = a.save(fresh);
        assertEquals("new.person@example.com", saved.getEmail());
        assertTrue(saved.getUid().startsWith("u-"));

        FileUserStore b = new FileUserStore("classpath:users.yml", dir);
        assertEquals("https://alice.example/hook",
                b.byEmail("alice@sandbox.crossdesk").orElseThrow().getSettings().getWebhookUrl());
        assertTrue(b.byApiKeyHash(ApiKeys.hash("ck_x")).isPresent());
        assertTrue(b.byEmail("new.person@example.com").isPresent());
        assertTrue(b.delete(saved.getUid()));
        assertFalse(new FileUserStore("classpath:users.yml", dir).byEmail("new.person@example.com").isPresent());
    }

    @Test
    void apiKeysHashAndNeverRoundTrip() {
        String k = ApiKeys.generate();
        assertTrue(ApiKeys.looksLikeKey(k));
        assertEquals(64, ApiKeys.hash(k).length());
        assertEquals(ApiKeys.hash(k), ApiKeys.hash(k));
        assertFalse(ApiKeys.hash(k).contains(k.substring(3, 20)));
    }
}
