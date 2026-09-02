package com.lucilla.settlement.web;

import com.lucilla.settlement.ledger.LedgerErrors;
import com.lucilla.settlement.ledger.LedgerService;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Turns failures into clean JSON, and decides which failures are the LEDGER'S ANSWER
 * rather than the server's fault.
 *
 * <p>A Daml rejection — {@code assertMsg "committed holding is the wrong instrument"},
 * {@code "the venue cannot clear orders once the book is sealed"}, {@code "discovered
 * price is outside the venue's price collar"} — is the model working exactly as
 * designed. It is an EXPECTED business outcome and comes back as a 4xx carrying the
 * model's own sentence, never a 500 and never a stack trace. That is the difference
 * between a desk that says "you cannot sell what you have already committed" and one
 * that says "Internal Server Error".
 *
 * <p>The status is chosen from the gRPC status code when there is one, because those
 * codes mean genuinely different things: {@code PERMISSION_DENIED} (403) is a token
 * problem, {@code CONTRACT_NOT_FOUND} (409) is a stale contract id, {@code UNAVAILABLE}
 * (503) is a participant that is down, and a Daml assertion (422) is a request that was
 * understood and refused. Collapsing them into one status would throw away the only
 * signal that says which of those to go and fix.
 *
 * <p>Every handler LOGS as well as responds — a projector shows one line, the log keeps
 * the whole thing, including the command id a node operator needs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * A ledger/command failure. When it carries a gRPC classification the HTTP status
     * follows that; otherwise it keeps the historical 422, which is right for the
     * desk's own pre-flight rejections ("Alice holds no USDC").
     */
    @ExceptionHandler(LedgerService.LedgerException.class)
    public ResponseEntity<Map<String, Object>> handleLedger(LedgerService.LedgerException e) {
        LedgerErrors.Failure f = e.failure();
        HttpStatus status = statusFor(f);
        if (f != null && !f.businessRejection()) {
            // Already logged in full by LedgerService at the point of failure; this is
            // the correlation between that log line and the HTTP response.
            log.warn("API {} {} commandId={} -> {}", status.value(), safeCode(f),
                    e.commandId(), e.getMessage());
        } else {
            log.info("API {} ledger rejection: {}", status.value(), e.getMessage());
        }
        return body(status, e.getMessage(), f, e.commandId());
    }

    /**
     * A gRPC failure that reached the web layer without being wrapped (for example from
     * the party-management admin stub). Classified and reported the same way rather
     * than falling through to a 500 with no message.
     */
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGrpc(StatusRuntimeException e) {
        LedgerErrors.Failure f = LedgerErrors.of(e);
        HttpStatus status = statusFor(f);
        String message = LedgerErrors.userMessage(f, null);
        log.error("LEDGER FAILURE (unwrapped) status={} description=\"{}\" correlationId={} | {}",
                f.codeLabel(), LedgerErrors.truncate(f.description()), f.correlationId(), f.hint());
        return body(status, message, f, null);
    }

    /** 401 / 403 raised inside a controller (a role the filter admitted, a party it lacks). */
    @ExceptionHandler(com.lucilla.settlement.auth.AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(com.lucilla.settlement.auth.AuthException e) {
        log.info("API {} auth: {}", e.status().value(), e.getMessage());
        return body(e.status(), e.getMessage(), null, null);
    }

    /**
     * An issuer or lender confirm without verifiable evidence — a bare tick, a missing
     * block, or numbers that fail the protocol's rule. 422 because the request was
     * understood and refused, carrying the schema so the caller learns what to send.
     */
    @ExceptionHandler(com.lucilla.settlement.ledger.SignerEvidence.Rejected.class)
    public ResponseEntity<Map<String, Object>> handleEvidence(com.lucilla.settlement.ledger.SignerEvidence.Rejected e) {
        log.info("API 422 evidence refused ({}): {}", e.seat(), e.getMessage());
        ResponseEntity<Map<String, Object>> base = body(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null, null);
        Map<String, Object> out = new LinkedHashMap<>(base.getBody());
        out.put("seat", e.seat());
        out.put("problems", e.problems());
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("required", true);
        ev.put("shape", "{ checks: [condition], evidence: { <condition>: { <field>: <value> } } }");
        ev.put("conditions", e.schema());
        out.put("evidence", ev);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(out);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadArg(IllegalArgumentException e) {
        log.info("API 400 bad request: {}", e.getMessage());
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), null, null);
    }

    /**
     * The desk's own consistency checks — "the close must run over the COMPLETE book",
     * "close produced no settlement batch". The request was well-formed; the ledger's
     * state does not support it, which is a CONFLICT and not a server fault. Without
     * this handler these surfaced as a bare 500 with no message at all.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException e) {
        log.warn("API 409 state conflict: {}", e.getMessage(), e);
        return body(HttpStatus.CONFLICT, e.getMessage(), null, null);
    }

    /**
     * An {@code orElseThrow()} that found nothing — the ledger moved under the request
     * (a contract consumed by a concurrent submission). Reported as a conflict with a
     * usable sentence, because {@code NoSuchElementException}'s own message ("No value
     * present") explains nothing to anybody.
     */
    /** A named resource that does not exist — a benchmark id, a fund id, a user — is a 404. */
    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFound e) {
        log.info("API 404: {}", e.getMessage());
        return body(HttpStatus.NOT_FOUND, e.getMessage(), null, null);
    }

    /** Thrown by the product routes for an unknown id. Extends NoSuchElementException so an
     *  {@code orElseThrow} site can raise it directly; the more specific handler above wins. */
    public static class NotFound extends NoSuchElementException {
        public NotFound(String message) {
            super(message);
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleMissing(NoSuchElementException e) {
        log.warn("API 409 expected contract/value not present", e);
        return body(HttpStatus.CONFLICT,
                "the ledger state moved while this request was being assembled (a contract it "
                        + "expected is no longer there). Re-read the current state and retry.",
                null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("validation failed");
        log.info("API 400 validation: {}", msg);
        return body(HttpStatus.BAD_REQUEST, msg, null, null);
    }

    /**
     * gRPC status → HTTP status. Each arm is a genuinely different thing to go and fix,
     * so they are kept apart on the wire too.
     */
    static HttpStatus statusFor(LedgerErrors.Failure f) {
        if (f == null) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (f.businessRejection()) {
            return HttpStatus.UNPROCESSABLE_ENTITY;   // the MODEL said no, and said why
        }
        String canton = f.cantonCode() == null ? "" : f.cantonCode();
        if ("CLIENT_TIMEOUT".equals(canton)) {
            return HttpStatus.GATEWAY_TIMEOUT;        // no answer — the command MAY have landed
        }
        if (canton.startsWith("CONTRACT_NOT_FOUND") || canton.startsWith("CONTRACT_NOT_ACTIVE")) {
            return HttpStatus.CONFLICT;               // a STALE contract id, not a 404
        }
        if (f.code() == null) {
            return HttpStatus.UNPROCESSABLE_ENTITY;   // never reached gRPC — keep the old default
        }
        return switch (f.code()) {
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ABORTED, FAILED_PRECONDITION, ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNAVAILABLE, RESOURCE_EXHAUSTED, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL, UNKNOWN, DATA_LOSS -> HttpStatus.BAD_GATEWAY;
            case INVALID_ARGUMENT, OUT_OF_RANGE -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    private static String safeCode(LedgerErrors.Failure f) {
        return f == null ? "-" : f.codeLabel();
    }

    /**
     * The JSON error body. {@code message} stays the primary field (every existing
     * caller reads it); {@code code}, {@code hint} and {@code commandId} are additive
     * and are what make a live failure diagnosable without opening a log.
     */
    private static ResponseEntity<Map<String, Object>> body(
            HttpStatus status, String message, LedgerErrors.Failure f, String commandId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", Instant.now().toString());
        out.put("status", status.value());
        out.put("error", status.getReasonPhrase());
        out.put("message", message == null ? "" : message);
        if (f != null) {
            out.put("code", f.codeLabel());
            out.put("grpcStatus", f.code() == null ? null : f.code().name());
            out.put("correlationId", f.correlationId());
            out.put("hint", f.hint());
            out.put("damlRejection", f.businessRejection());
        }
        if (commandId != null && !commandId.isBlank()) {
            out.put("commandId", commandId);
        }
        return ResponseEntity.status(status).body(out);
    }
}
