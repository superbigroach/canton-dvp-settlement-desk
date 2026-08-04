package com.lucilla.settlement.registry;

import com.daml.ledger.javaapi.data.DisclosedContract;
import com.daml.ledger.javaapi.data.Identifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.lucilla.settlement.model.splice.api.token.metadatav1.ChoiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A client for SOMEBODY ELSE'S CIP-56 registry.
 *
 * <p><b>Why this exists.</b> The token standard is only worth anything if an asset issued
 * by a registry we did not write can be held and moved by a party of ours. That is exactly
 * the situation with BitSafe's CBTC: the transfer arrives as a
 * {@code TransferInstruction} on BitSafe's own template, and accepting it may require a
 * {@code ChoiceContext} and disclosed contracts that ONLY BitSafe's registry can produce.
 * The endpoints are the same ones {@link RegistryController} serves for our own
 * instruments — this class is the other end of that same contract.
 *
 * <p><b>Nothing is hardcoded and nothing is guessed.</b> A registry base URL is supplied
 * by configuration, per instrument admin or as a default:
 * <pre>
 *   registry.remote-url  (REGISTRY_REMOTE_URL)   a single base URL used for any foreign admin
 *   registry.remote-urls (REGISTRY_REMOTE_URLS)  comma-separated  admin=url  overrides, where
 *                                                `admin` is a full party id or its label
 * </pre>
 * With nothing configured this class is INERT: it reports that no registry is known for an
 * admin, and the caller proceeds with an empty context and says so out loud. That is the
 * honest behaviour — a fabricated context cannot be better than none, and a wrong URL is
 * worse than an absent one.
 *
 * <p><b>Base URL, not endpoint URL.</b> The paths below are the specification's and are
 * appended by this class, so an operator configures {@code https://registry.example} and
 * not a path — the same way the reference CLI takes a
 * {@code --transfer-factory-registry-url}.
 */
@Component
public class RemoteRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteRegistryClient.class);

    /** A foreign registry is an external dependency; do not let it hold a request open. */
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper json = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${registry.remote-url:}")
    private String defaultBaseUrl = "";

    @Value("${registry.remote-urls:}")
    private String perAdminBaseUrls = "";

    /** A foreign registry that answered, but not with something usable. */
    public static class RemoteRegistryException extends RuntimeException {
        public RemoteRegistryException(String message) {
            super(message);
        }

        public RemoteRegistryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A choice context fetched from a foreign registry, converted into the exact types a
     * Daml command needs.
     *
     * @param context   decoded with the GENERATED {@code ChoiceContext.fromJson}, so the
     *                  variant encoding of every {@code AnyValue} is the codegen's problem
     *                  and not a hand-rolled parser's
     * @param disclosed the contracts to attach to the submission; without them the
     *                  command fails on contracts the acting party cannot see
     * @param sourceUrl the endpoint that produced this, for the log line
     */
    public record FetchedContext(
            ChoiceContext context, List<DisclosedContract> disclosed, String sourceUrl) {
    }

    /**
     * The configured registry base URL for an instrument admin, if there is one.
     *
     * <p>Matched on the full party id first, then on the party's label (the part before
     * {@code ::}), then the default. The label fallback exists because a devnet party id
     * carries a namespace suffix an operator should not have to paste correctly.
     */
    public Optional<String> baseUrlFor(String admin) {
        Map<String, String> overrides = overrides();
        if (admin != null && !admin.isBlank()) {
            String exact = overrides.get(admin);
            if (exact != null) {
                return Optional.of(trimSlash(exact));
            }
            String label = admin.contains("::") ? admin.substring(0, admin.indexOf("::")) : admin;
            String byLabel = overrides.get(label);
            if (byLabel != null) {
                return Optional.of(trimSlash(byLabel));
            }
        }
        return (defaultBaseUrl == null || defaultBaseUrl.isBlank())
                ? Optional.empty() : Optional.of(trimSlash(defaultBaseUrl));
    }

    /**
     * {@code POST {base}/registry/transfer-instruction/v1/{cid}/choice-contexts/{choice}}
     * — the spec's endpoint for accept / reject / withdraw.
     */
    public FetchedContext transferInstructionContext(String baseUrl, String cid, String choice) {
        return fetch(baseUrl + "/registry/transfer-instruction/v1/" + encode(cid)
                + "/choice-contexts/" + choice);
    }

    /**
     * {@code POST {base}/registry/allocations/v1/{cid}/choice-contexts/{choice}} — the
     * spec's endpoint for execute-transfer / withdraw / cancel.
     */
    public FetchedContext allocationContext(String baseUrl, String cid, String choice) {
        return fetch(baseUrl + "/registry/allocations/v1/" + encode(cid)
                + "/choice-contexts/" + choice);
    }

    /** {@code GET {base}/registry/metadata/v1/info} — used to confirm a URL is a registry at all. */
    public Optional<String> adminIdOf(String baseUrl) {
        try {
            HttpResponse<String> res = http.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/registry/metadata/v1/info"))
                            .timeout(HTTP_TIMEOUT)
                            .header("Accept", "application/json")
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                return Optional.empty();
            }
            JsonNode body = json.readTree(res.body());
            JsonNode adminId = body.get("adminId");
            return adminId == null ? Optional.empty() : Optional.of(adminId.asText());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("REGISTRY remote {} did not answer /registry/metadata/v1/info: {}",
                    baseUrl, e.toString());
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private FetchedContext fetch(String url) {
        // The spec's GetChoiceContextRequest has only optional fields, so an empty object
        // is a complete, valid request body.
        String requestBody = "{}";
        log.info("REGISTRY remote choice-context POST {}", url);
        HttpResponse<String> res;
        try {
            res = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(HTTP_TIMEOUT)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody,
                                    StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteRegistryException("interrupted while calling " + url, e);
        } catch (Exception e) {
            throw new RemoteRegistryException(
                    "the registry at " + url + " could not be reached (" + e + "). The choice "
                            + "cannot be assembled without its context; check the configured "
                            + "registry.remote-url / registry.remote-urls and that the host is "
                            + "reachable from this backend.", e);
        }
        if (res.statusCode() / 100 != 2) {
            throw new RemoteRegistryException(
                    "the registry at " + url + " answered HTTP " + res.statusCode() + ": "
                            + truncate(res.body()) + ". A 404 here usually means the contract id "
                            + "belongs to a different registry, or that this URL is not that "
                            + "registry's API root.");
        }
        try {
            JsonNode body = json.readTree(res.body());
            JsonNode data = body.get("choiceContextData");
            // Decoded by the GENERATED decoder, so a registry that sends a richer context
            // than we anticipated still round-trips correctly into the Daml type.
            ChoiceContext context = (data == null || data.isNull())
                    ? new ChoiceContext(Map.of())
                    : ChoiceContext.fromJson(data.toString());
            List<DisclosedContract> disclosed = new ArrayList<>();
            JsonNode arr = body.get("disclosedContracts");
            if (arr != null && arr.isArray()) {
                for (JsonNode node : arr) {
                    disclosed.add(disclosedOf(node, url));
                }
            }
            log.info("REGISTRY remote choice-context OK {} contextValues={} disclosed={}",
                    url, context.values.size(), disclosed.size());
            return new FetchedContext(context, disclosed, url);
        } catch (RemoteRegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new RemoteRegistryException(
                    "the registry at " + url + " answered with a body this client could not "
                            + "decode (" + e + "). Body: " + truncate(res.body()), e);
        }
    }

    private static DisclosedContract disclosedOf(JsonNode node, String url) {
        String templateId = text(node, "templateId");
        String contractId = text(node, "contractId");
        String blob = text(node, "createdEventBlob");
        String synchronizerId = text(node, "synchronizerId");
        if (templateId == null || contractId == null || blob == null) {
            throw new RemoteRegistryException("the registry at " + url + " returned a disclosed "
                    + "contract without templateId/contractId/createdEventBlob; it cannot be "
                    + "attached to a command");
        }
        ByteString bytes;
        try {
            bytes = ByteString.copyFrom(Base64.getDecoder().decode(blob));
        } catch (IllegalArgumentException e) {
            throw new RemoteRegistryException("the registry at " + url + " returned a "
                    + "createdEventBlob that is not base64", e);
        }
        Identifier id = identifierOf(templateId, url);
        // synchronizerId is REQUIRED by the spec, but a registry that omits it still yields
        // a usable disclosure on a single-synchronizer deployment — so it is tolerated
        // rather than rejected, and the omission shows up in the log above.
        return synchronizerId == null || synchronizerId.isBlank()
                ? new DisclosedContract(id, contractId, bytes)
                : new DisclosedContract(id, contractId, bytes, synchronizerId);
    }

    /** {@code pkgId:Module:Entity} or {@code #package-name:Module:Entity}. */
    private static Identifier identifierOf(String templateId, String url) {
        int firstColon = templateId.indexOf(':');
        int lastColon = templateId.lastIndexOf(':');
        if (firstColon <= 0 || lastColon <= firstColon) {
            throw new RemoteRegistryException("the registry at " + url + " returned templateId '"
                    + templateId + "', which is not in packageId:Module:Entity form");
        }
        return new Identifier(
                templateId.substring(0, firstColon),
                templateId.substring(firstColon + 1, lastColon),
                templateId.substring(lastColon + 1));
    }

    private Map<String, String> overrides() {
        Map<String, String> out = new LinkedHashMap<>();
        if (perAdminBaseUrls == null || perAdminBaseUrls.isBlank()) {
            return out;
        }
        for (String pair : perAdminBaseUrls.split(",")) {
            String p = pair.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            out.put(p.substring(0, eq).trim(), p.substring(eq + 1).trim());
        }
        return out;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String trimSlash(String url) {
        String u = url.trim();
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        String flat = s.replace('\n', ' ').replace('\r', ' ');
        return flat.length() <= 300 ? flat : flat.substring(0, 300) + "…(truncated)";
    }
}
