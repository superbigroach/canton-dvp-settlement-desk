package com.lucilla.settlement.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The registry OPERATOR's own endpoints — <b>deliberately not part of CIP-56</b>.
 *
 * <p>They live under {@code /api/token-standard/...} and never under {@code /registry/...},
 * so the standard namespace contains nothing a wallet could mistake for a standard
 * endpoint. Two things belong here and nothing else:
 *
 * <ol>
 *   <li><b>Creating the factory contract.</b> The off-ledger Registry API can only serve
 *       what the ledger holds, and the ledger holds a {@code TokenStandardRegistry} only
 *       once somebody creates one. Before that, every {@code /registry/...} endpoint
 *       honestly answers 404. This is the one call that changes that.</li>
 *   <li><b>Issuance.</b> CIP-56 v1 says <i>nothing</i> about minting — each registry does
 *       it its own way, which is exactly why it cannot live under the standard's paths.
 *       Ours is a direct create co-signed by the admin and the owner.</li>
 * </ol>
 *
 * <p>Both are the registry operator acting as itself; a wallet never calls either.
 *
 * <p><b>These are not secured.</b> On this desk nothing is — the whole backend acts under
 * one participant token and there is no user model. On a real deployment these two
 * endpoints are the ones that would sit behind the operator's authentication, because they
 * are the only ones that write.
 */
@RestController
@RequestMapping("/api/token-standard")
public class RegistryAdminController {

    private static final Logger log = LoggerFactory.getLogger(RegistryAdminController.class);

    private final RegistryService registry;

    public RegistryAdminController(RegistryService registry) {
        this.registry = registry;
    }

    /** {@code {"admin": "onRails"}} — a party label or a full party id; resolved live. */
    public record CreateRegistryRequest(String admin) {
    }

    /** {@code {"owner": "Alice", "instrumentId": "cETH", "amount": "5.0"}} */
    public record MintRequest(String owner, String instrumentId, BigDecimal amount) {
    }

    /**
     * {@code GET /api/token-standard/registry} — what the off-ledger API currently has to
     * work with, in one call.
     *
     * <p>Answers the question that otherwise costs three requests and a guess: is there a
     * factory on this ledger, which party administers it, what is its contract id right
     * now, and which instruments does it actually cover. Always 200 with a body, like
     * {@code /api/diag} — read {@code present}, not the status code.
     */
    @GetMapping("/registry")
    public Map<String, Object> state() {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<RegistryService.FactoryContract> factory = registry.findFactory();
        out.put("present", factory.isPresent());
        if (factory.isEmpty()) {
            out.put("note", "no TokenStandardRegistry contract is on this ledger, so every "
                    + "/registry/... endpoint answers 404. POST /api/token-standard/registry "
                    + "with an admin party to create one.");
            return out;
        }
        RegistryService.FactoryContract f = factory.get();
        out.put("adminId", f.admin());
        out.put("factoryId", f.contractId());
        out.put("synchronizerId", f.synchronizerId());
        // The blob is served to callers by /registry/... but is pointless (and large) in a
        // diagnostic; report only that it is there and how big it is.
        out.put("disclosureBytes", f.createdEventBlob() == null ? 0 : f.createdEventBlob().length());
        List<Map<String, Object>> instruments = registry.instruments(f.admin()).stream()
                .map(r -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.id());
                    row.put("totalSupply", r.totalSupply().stripTrailingZeros().toPlainString());
                    row.put("lockedSupply", r.lockedSupply().stripTrailingZeros().toPlainString());
                    row.put("holdings", r.holdingCount());
                    return row;
                })
                .toList();
        out.put("instruments", instruments);
        out.put("registryApi", "/registry/metadata/v1/info");
        return out;
    }

    /**
     * {@code POST /api/token-standard/registry} — create the factory contract, or return
     * the one already there.
     *
     * <p>Idempotent: two factories for one admin would make {@code factoryId} ambiguous,
     * and a wallet that disclosed the wrong one would get an authorisation failure with
     * nothing in it to explain the cause.
     */
    @PostMapping("/registry")
    public ResponseEntity<Map<String, Object>> createRegistry(
            @RequestBody CreateRegistryRequest request) {
        if (request == null || request.admin() == null || request.admin().isBlank()) {
            throw new IllegalArgumentException(
                    "admin is required — the party that will administer this registry's "
                            + "instruments (a label like \"onRails\" or a full party id)");
        }
        String factoryId = registry.ensureRegistry(request.admin());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("factoryId", factoryId);
        registry.adminParty().ifPresent(a -> out.put("adminId", a));
        out.put("registryApi", "/registry/metadata/v1/info");
        log.info("REGISTRY bootstrap complete factoryId={}", factoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    /**
     * {@code POST /api/token-standard/holdings} — mint a free {@code TokenStandardHolding}.
     *
     * <p>Requires the authority of BOTH the registry admin and the owner: the standard's
     * holding is co-signed, so nobody can put an asset in your name without your consent.
     * That is a real difference from the legacy issuer-only {@code Holding} this project
     * still uses everywhere else, and it is why this call can fail with a 403 on a
     * participant whose token does not carry {@code actAs} for both parties — which is the
     * correct outcome, not a bug.
     */
    @PostMapping("/holdings")
    public ResponseEntity<Map<String, Object>> mint(@RequestBody MintRequest request) {
        if (request == null || request.owner() == null || request.owner().isBlank()) {
            throw new IllegalArgumentException("owner is required");
        }
        if (request.instrumentId() == null || request.instrumentId().isBlank()) {
            throw new IllegalArgumentException(
                    "instrumentId is required (the id half of InstrumentId {admin, id}, e.g. cETH)");
        }
        String cid = registry.mintHolding(
                request.owner(), request.instrumentId().trim(), request.amount());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("holdingCid", cid);
        out.put("instrumentId", request.instrumentId().trim());
        out.put("amount", request.amount() == null
                ? null : request.amount().stripTrailingZeros().toPlainString());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }
}
