package com.lucilla.settlement.registry;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * The wire types of the CIP-56 <b>off-ledger Registry API</b>, transcribed from the
 * official OpenAPI specifications in the Splice release — NOT invented here.
 *
 * <p>Every record below corresponds one-for-one to a schema in:
 * <pre>
 *   token-standard/splice-api-token-metadata-v1/openapi/token-metadata-v1.yaml
 *   token-standard/splice-api-token-transfer-instruction-v1/openapi/transfer-instruction-v1.yaml
 *   token-standard/splice-api-token-allocation-instruction-v1/openapi/allocation-instruction-v1.yaml
 *   token-standard/splice-api-token-allocation-v1/openapi/allocation-v1.yaml
 * </pre>
 * Field names are the spec's field names verbatim. Where the spec marks a field
 * optional it is nullable here and elided from the JSON ({@link JsonInclude}), because
 * a wallet generated from the same OpenAPI document distinguishes "absent" from "null".
 *
 * <p><b>Why this file is not generated.</b> The four specs are separate documents that
 * deliberately repeat their shared schemas ("intentionally not shared with the other
 * APIs … because not all OpenAPI codegens support such shared definitions"). Pulling
 * four generators into the Gradle build to emit ~10 records would add more moving parts
 * than it removes; instead each record carries the path of the schema it mirrors, so a
 * reviewer can diff it against the source in one step.
 */
public final class RegistryDtos {

    private RegistryDtos() {
    }

    // -----------------------------------------------------------------------
    // token-metadata-v1
    // -----------------------------------------------------------------------

    /**
     * {@code GetRegistryInfoResponse} — who this registry is.
     *
     * <p>{@code supportedApis} carries only the REGISTRY-WIDE apis, per the spec's own
     * note ("this only includes the registry-wide APIs; use the instrument lookup
     * endpoints to see which APIs are supported for a given instrument"). Canton Coin's
     * Scan answers exactly {@code {"splice-api-token-metadata-v1":1}} here.
     */
    public record RegistryInfo(String adminId, Map<String, Integer> supportedApis) {
    }

    /**
     * {@code Instrument} — an instrument this registry administers.
     *
     * <p>Only the fields this registry can answer FROM THE LEDGER are populated. The
     * later-minor-version fields of the metadata spec ({@code pauseInfo},
     * {@code showAccountInputFields}, {@code accountInputFieldsToShow}) are absent
     * because nothing on-ledger backs them — see {@link RegistryService} for what each
     * populated field is derived from.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Instrument(
            String id,
            String name,
            String symbol,
            String totalSupply,
            String totalSupplyAsOf,
            int decimals,
            Map<String, Integer> supportedApis) {
    }

    /** {@code ListInstrumentsResponse}. {@code nextPageToken} is absent when there is no next page. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ListInstrumentsResponse(List<Instrument> instruments, String nextPageToken) {
    }

    // -----------------------------------------------------------------------
    // Shared by transfer-instruction-v1 / allocation-instruction-v1 / allocation-v1
    // -----------------------------------------------------------------------

    /**
     * {@code DisclosedContract} — the bytes a caller must hand its participant so that a
     * contract it cannot otherwise see becomes visible for ONE command.
     *
     * <p>{@code createdEventBlob} is the participant's own authenticated serialisation of
     * the create event, base64-encoded. It is what makes explicit disclosure safe: the
     * receiving participant validates it against the contract id, so a registry cannot
     * fabricate a contract by serving a doctored blob.
     *
     * <p>The {@code debug*} fields are advisory ("use this data only if you trust the
     * provider, as it might not match the data in the createdEventBlob") and are omitted
     * entirely when the caller sets {@code excludeDebugFields}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DisclosedContract(
            String templateId,
            String contractId,
            String createdEventBlob,
            String synchronizerId,
            String debugPackageName,
            Map<String, Object> debugPayload,
            String debugCreatedAt) {
    }

    /**
     * {@code ChoiceContext} — everything a caller needs to exercise one standard choice.
     *
     * <p>{@code choiceContextData} is passed by the caller as {@code extraArgs.context} in
     * the choice argument, so its JSON shape is the Daml JSON encoding of
     * {@code Splice.Api.Token.MetadataV1.ChoiceContext}, i.e. {@code {"values": {...}}} —
     * <b>not</b> a bare object. See {@link RegistryService#EMPTY_CHOICE_CONTEXT_DATA}.
     */
    public record ChoiceContext(
            Map<String, Object> choiceContextData,
            List<DisclosedContract> disclosedContracts) {
    }

    /**
     * {@code TransferFactoryWithChoiceContext} — the transfer-instruction spec's factory
     * response. Identical to {@link FactoryWithChoiceContext} plus {@code transferKind}.
     */
    public record TransferFactoryWithChoiceContext(
            String factoryId, String transferKind, ChoiceContext choiceContext) {
    }

    /** {@code FactoryWithChoiceContext} — the allocation-instruction spec's factory response. */
    public record FactoryWithChoiceContext(String factoryId, ChoiceContext choiceContext) {
    }

    // -----------------------------------------------------------------------
    // Requests
    // -----------------------------------------------------------------------

    /**
     * {@code GetFactoryRequest}. {@code choiceArguments} is the JSON the caller intends to
     * pass to the factory choice, supplied "so that the registry can also provide
     * choice-argument specific contracts, e.g. the configuration for a specific
     * instrument-id". This registry reads it to VALIDATE the request up front (see
     * {@link RegistryController}) rather than let the Daml choice reject it later.
     */
    public record GetFactoryRequest(Map<String, Object> choiceArguments, Boolean excludeDebugFields) {

        public boolean hideDebug() {
            return Boolean.TRUE.equals(excludeDebugFields);
        }
    }

    /** {@code GetChoiceContextRequest}. A body is required by the spec, but every field is optional. */
    public record GetChoiceContextRequest(Map<String, String> meta, Boolean excludeDebugFields) {

        public boolean hideDebug() {
            return Boolean.TRUE.equals(excludeDebugFields);
        }
    }

    /**
     * {@code ErrorResponse} — {@code {"error": "..."}}.
     *
     * <p>Deliberately NOT the desk's own error body. The registry namespace is consumed by
     * third-party wallets generated from the OpenAPI documents above, and those clients
     * read a single {@code error} string; the desk's richer
     * {@code {timestamp,status,error,message,code,hint,commandId}} body would put an HTTP
     * reason phrase where the wallet expects the explanation. Ledger/gRPC failures still
     * fall through to {@code ApiExceptionHandler} and keep the desk's diagnosable shape —
     * only the registry's OWN 400/404s use this.
     */
    public record ErrorResponse(String error) {
    }
}
