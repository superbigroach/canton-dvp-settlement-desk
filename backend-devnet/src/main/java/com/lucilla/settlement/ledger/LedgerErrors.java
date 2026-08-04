package com.lucilla.settlement.ledger;

import com.daml.ledger.javaapi.data.Command;
import com.daml.ledger.javaapi.data.CreateCommand;
import com.daml.ledger.javaapi.data.ExerciseCommand;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.javaapi.data.codegen.HasCommands;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a ledger failure and says something USEFUL about it.
 *
 * <p><b>Why this class exists.</b> Canton deliberately withholds authorization-failure
 * detail from the client. From the Ledger API error docs, verbatim:
 *
 * <blockquote>"This rejection is given if the supplied authorization token is not
 * sufficient for the intended command. The exact reason is logged on the participant,
 * but not given to the user for security reasons."</blockquote>
 *
 * <p>So on a {@code PERMISSION_DENIED} the message the client receives is useless BY
 * DESIGN, and the real reason sits in the participant's log — on a node this desk does
 * not operate. The single most valuable thing the desk can therefore emit is whatever
 * lets the NODE OPERATOR find that entry: the command id, the application/user id, the
 * acting party as a FULL party id, the template and choice, the participant's own
 * correlation id when it leaks one, and a timestamp. Without those you cannot even ask
 * the operator the right question.
 *
 * <p>This is the project's own war story: a {@code PERMISSION_DENIED} where reads
 * worked and writes did not, resolved only by reading the participant's logs.
 *
 * <p><b>Pure and side-effect free</b> — no logging, no I/O, no Spring. It turns a
 * {@link Throwable} into a {@link Failure} and lets the caller decide what to do with
 * it, which is what makes it unit-testable ({@code LedgerErrorsTest}).
 *
 * <p><b>Nothing here can leak a secret.</b> It only ever reads gRPC status codes,
 * status descriptions and Daml command shapes. It never touches a token.
 */
public final class LedgerErrors {

    private LedgerErrors() {
    }

    /** How much of a command argument / status description a log line will carry. */
    private static final int MAX_RENDER = 300;

    /**
     * Canton stamps its own error id on the status description, e.g.
     * {@code DAML_INTERPRETATION_ERROR(9,0a1b2c3d): …}. That id is FINER-GRAINED than the
     * gRPC code (both {@code CONTRACT_NOT_FOUND} and a missing party arrive as gRPC
     * {@code NOT_FOUND}), so it is preferred whenever present. The second field is the
     * participant's correlation id — the string to quote to the node operator.
     */
    private static final Pattern CANTON_CODE =
            Pattern.compile("([A-Z][A-Z0-9_]{2,})\\((\\d+),([0-9a-fA-F]+)\\)");

    /** The redacted-security-error form: "…inquire about the request &lt;correlationId&gt;". */
    private static final Pattern CORRELATION_HINT =
            Pattern.compile("inquire about the request ([0-9a-fA-F][0-9a-fA-F-]{3,})");

    /** A Daml exception payload: {@code … { message = "the venue cannot clear orders …" }}. */
    private static final Pattern DAML_MESSAGE =
            Pattern.compile("message\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Older / alternative interpretation-failure renderings. */
    private static final Pattern DAML_ASSERTION =
            Pattern.compile("(?:Assertion failed|Template precondition violated)[:\\s]+([^\\n]+)");

    // -----------------------------------------------------------------------
    // The verdict
    // -----------------------------------------------------------------------

    /**
     * Everything worth knowing about one failed ledger interaction.
     *
     * @param code          the gRPC status code, or {@code null} when the failure never
     *                      reached gRPC (a timeout, a decode error, a bug in the desk)
     * @param cantonCode    Canton's own error id ({@code CONTRACT_NOT_FOUND},
     *                      {@code DAML_INTERPRETATION_ERROR}, …), or {@code null}
     * @param description   the full status description, verbatim and untruncated
     * @param correlationId the participant's correlation id, the handle the operator
     *                      greps for, or {@code null} when none was disclosed
     * @param damlMessage   the model's OWN message ("committed holding is the wrong
     *                      instrument"), or {@code null} when this was not a Daml rejection
     * @param businessRejection {@code true} when the ledger did its job and said no — an
     *                      expected outcome to show the user, not a server fault
     * @param hint          what this code means operationally and WHERE TO LOOK NEXT
     */
    public record Failure(
            Status.Code code, String cantonCode, String description, String correlationId,
            String damlMessage, boolean businessRejection, String hint) {

        /** The code as a string for logs/JSON — Canton's own id when it gave one. */
        public String codeLabel() {
            if (cantonCode != null) {
                return cantonCode;
            }
            return code != null ? code.name() : "NO_STATUS";
        }

        /** True when the participant refused the command's authorization. */
        public boolean permissionDenied() {
            return code == Status.Code.PERMISSION_DENIED || code == Status.Code.UNAUTHENTICATED;
        }
    }

    /**
     * Classify a throwable, looking THROUGH the wrappers rxjava and gRPC futures put
     * around a {@link StatusRuntimeException} rather than only at the top of the chain.
     */
    public static Failure of(Throwable t) {
        StatusRuntimeException sre = grpcCause(t).orElse(null);
        if (sre == null) {
            boolean timedOut = hasCause(t, TimeoutException.class);
            String msg = rootMessage(t);
            return new Failure(null, timedOut ? "CLIENT_TIMEOUT" : null, msg, null, null, false,
                    timedOut
                            ? "no response from the participant within the desk's 30s call timeout. "
                                    + "The command MAY still have been accepted — check the ledger "
                                    + "before retrying, and check the participant is not overloaded."
                            : "this failure never reached gRPC, so it is the desk's own or the "
                                    + "network's. The stack trace (logged at ERROR) is the evidence.");
        }
        Status status = sre.getStatus();
        String description = status.getDescription() != null
                ? status.getDescription()
                : String.valueOf(sre.getMessage());
        String cantonCode = firstGroup(CANTON_CODE, description, 1);
        String correlationId = firstGroup(CANTON_CODE, description, 3);
        if (correlationId == null) {
            correlationId = firstGroup(CORRELATION_HINT, description, 1);
        }
        String damlMessage = damlMessageOf(description);
        return new Failure(status.getCode(), cantonCode, description, correlationId, damlMessage,
                damlMessage != null, hintFor(status.getCode(), cantonCode));
    }

    /**
     * The model's own rejection message, pulled out of an interpretation error.
     *
     * <p>{@code Settlement.daml} and friends reject with SPECIFIC, human sentences —
     * "committed holding is the wrong instrument", "the venue cannot clear orders once
     * the book is sealed", "discovered price is outside the venue's price collar". Those
     * are the product speaking, and they belong in front of the user unmangled; the
     * {@code DAML_INTERPRETATION_ERROR(9,…): Interpretation error: Error: Unhandled Daml
     * exception: DA.Exception.AssertionFailed:AssertionFailed@…{ message = }} scaffolding
     * around them does not.
     *
     * @return the bare message, or {@code null} when this was not a Daml rejection
     */
    public static String damlMessageOf(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String m = firstGroup(DAML_MESSAGE, description, 1);
        if (m == null) {
            m = firstGroup(DAML_ASSERTION, description, 1);
        }
        if (m == null) {
            return null;
        }
        m = m.replace("\\\"", "\"").replace("\\n", " ").replace("\\\\", "\\").trim();
        // Trailing scaffolding from the "Assertion failed: …" form.
        m = m.replaceAll("\\s*\\}\\s*$", "").trim();
        return m.isEmpty() ? null : m;
    }

    /**
     * What a code MEANS operationally, and where to look next. Each of these fails a
     * demo in a different way and is fixed in a different place, so they must not be
     * collapsed into "ledger error".
     */
    public static String hintFor(Status.Code code, String cantonCode) {
        if ("CONTRACT_NOT_FOUND".equals(cantonCode) || "CONTRACT_NOT_ACTIVE".equals(cantonCode)) {
            return "the contract id is STALE — it was already archived or consumed (every "
                    + "consuming choice re-creates a successor with a NEW id). Re-read the "
                    + "current state and retry against the successor id; do not retry the same id.";
        }
        if ("DAML_INTERPRETATION_ERROR".equals(cantonCode)
                || "UNHANDLED_EXCEPTION".equals(cantonCode)) {
            return "the MODEL rejected this, not the infrastructure — the Daml assertion "
                    + "message is the whole explanation and the request should be corrected, "
                    + "not retried.";
        }
        if ("PACKAGE_NOT_FOUND".equals(cantonCode) || "NO_DOMAIN_FOR_SUBMISSION".equals(cantonCode)) {
            return "the participant cannot serve the package this command names — compare the "
                    + "backend's compiled-in package id (GET /api/diag) with what is uploaded "
                    + "to the node; a fresh DAR upload leaves an old backend pointing at a "
                    + "package the node no longer prefers.";
        }
        if (code == null) {
            return "no gRPC status was attached to this failure.";
        }
        return switch (code) {
            case PERMISSION_DENIED -> "the participant refused this command's AUTHORIZATION and "
                    + "Canton withholds the reason from the client on purpose (\"the exact reason "
                    + "is logged on the participant, but not given to the user for security "
                    + "reasons\"). The reason is in the PARTICIPANT'S LOG. Check the token's "
                    + "actAs/readAs claims name this exact FULL party id (not the label), then "
                    + "quote the command id + correlation id below to the node operator. Reads "
                    + "working while writes fail is the classic shape of a token with readAs but "
                    + "not actAs.";
            case UNAUTHENTICATED -> "the participant rejected the TOKEN itself — absent, malformed, "
                    + "or expired. GET /api/diag reports the token's expiry (never its value). On "
                    + "the hosted demo the refresher may have failed; on a local sandbox no token "
                    + "should be set at all.";
            case NOT_FOUND -> "the participant has no such contract, party or user. On Canton a "
                    + "party id carries a per-allocation namespace suffix, so a party id from an "
                    + "earlier run is simply gone — re-resolve it (GET /api/parties).";
            case INVALID_ARGUMENT -> "the participant rejected the command as malformed or "
                    + "inconsistent before it ever ran. This is the desk's request to fix, not "
                    + "the ledger's state.";
            case ABORTED -> "the submission LOST A RACE — contention on a contract another "
                    + "in-flight command consumed, or a synchronizer timeout. Canton treats this "
                    + "as retryable: re-read the current state and submit again.";
            case UNAVAILABLE -> "the participant is unreachable or restarting. Check the ledger "
                    + "endpoint and TLS setting (GET /api/diag echoes both) and that the node is "
                    + "up. Nothing was committed.";
            case DEADLINE_EXCEEDED -> "the call outran its deadline. The command MAY still have "
                    + "been committed — check the ledger before resubmitting.";
            case RESOURCE_EXHAUSTED -> "the participant is rate-limiting or out of headroom "
                    + "(this also shows up as a mid-frame ACS stream drop under load). Back off "
                    + "and retry; pure reads are safe to retry.";
            case FAILED_PRECONDITION -> "the ledger's state does not permit this command right "
                    + "now — typically a contract that has moved on. Re-read and retry.";
            case ALREADY_EXISTS -> "a command with this id was already submitted (deduplication). "
                    + "The first submission is the one that counts.";
            case INTERNAL, UNKNOWN, DATA_LOSS -> "an internal participant failure. The full "
                    + "description below is the only client-side evidence; the participant's log "
                    + "has the rest.";
            case CANCELLED -> "the call was cancelled before it completed.";
            default -> "an unexpected gRPC status; the full description below is the evidence.";
        };
    }

    /**
     * The sentence to put in front of a HUMAN — in the HTTP body, and on a projector.
     *
     * <p>Three different things, kept distinct:
     * <ul>
     *   <li>A <b>Daml rejection</b> is returned VERBATIM. "committed holding is the wrong
     *       instrument" is the product explaining itself; wrapping it in apology or
     *       gRPC scaffolding only obscures it. These are expected business outcomes.</li>
     *   <li>A <b>PERMISSION_DENIED</b> cannot be explained to the client at all, so
     *       instead of pretending, it hands over THE HANDLES someone can take to the
     *       node operator: the command id, and the participant's correlation id when it
     *       disclosed one.</li>
     *   <li>Anything else states the code and the participant's own description.</li>
     * </ul>
     */
    public static String userMessage(Failure f, String commandId) {
        if (f == null) {
            return "the ledger call failed";
        }
        if (f.damlMessage() != null) {
            return f.damlMessage();
        }
        StringBuilder sb = new StringBuilder();
        if (f.permissionDenied()) {
            sb.append("the participant refused this command (").append(f.codeLabel())
                    .append("). Canton withholds the reason from the client on purpose — the "
                            + "exact reason is written to the PARTICIPANT'S log, not returned here");
        } else {
            String d = truncate(f.description());
            if (f.code() != null || f.cantonCode() != null) {
                sb.append(f.codeLabel());
                if (!d.isBlank()) {
                    sb.append(": ").append(d);
                }
            } else {
                // Never reached gRPC at all — the raw sentence is more use than a
                // placeholder code no operator can search for.
                sb.append(d.isBlank() ? "the ledger call failed" : d);
            }
        }
        appendHandles(sb, f, commandId);
        return sb.toString();
    }

    /** Append the strings a node operator can actually search their logs for. */
    private static void appendHandles(StringBuilder sb, Failure f, String commandId) {
        List<String> handles = new ArrayList<>();
        if (commandId != null && !commandId.isBlank()) {
            handles.add("command id " + commandId);
        }
        if (f.correlationId() != null && !f.correlationId().isBlank()) {
            handles.add("correlation id " + f.correlationId());
        }
        if (!handles.isEmpty()) {
            sb.append(" — quote ").append(String.join(" / ", handles))
                    .append(" to the node operator");
        }
    }

    // -----------------------------------------------------------------------
    // Describing what was submitted (so a failure is greppable by WHAT, not just when)
    // -----------------------------------------------------------------------

    /**
     * A one-line, log-safe description of a submission: the template and choice, plus
     * the contract id an exercise targets.
     *
     * <p>Derived from the command itself rather than passed in by the caller, so it can
     * never drift away from what was actually put on the wire.
     */
    public static String describe(HasCommands command) {
        List<String> parts = new ArrayList<>();
        for (Command c : safeCommands(command)) {
            Optional<ExerciseCommand> ex = c.asExerciseCommand();
            if (ex.isPresent()) {
                ExerciseCommand e = ex.get();
                parts.add("exercise " + name(e.getTemplateId()) + "." + e.getChoice()
                        + " on " + e.getContractId());
                continue;
            }
            Optional<CreateCommand> cr = c.asCreateCommand();
            if (cr.isPresent()) {
                parts.add("create " + name(cr.get().getTemplateId()));
                continue;
            }
            parts.add(c.getClass().getSimpleName());
        }
        return parts.isEmpty() ? "(no commands)" : String.join(" + ", parts);
    }

    /**
     * The KEY ARGUMENTS of a submission, truncated — the choice argument for an
     * exercise, the create arguments for a create.
     *
     * <p>These are ledger payloads (parties, instrument ids, amounts), never
     * credentials: the access token travels in gRPC call metadata and is not part of
     * any {@link Command}, so it cannot reach this string.
     */
    public static String describeArgs(HasCommands command) {
        List<String> parts = new ArrayList<>();
        for (Command c : safeCommands(command)) {
            Optional<ExerciseCommand> ex = c.asExerciseCommand();
            if (ex.isPresent()) {
                parts.add(truncate(String.valueOf(ex.get().getChoiceArgument())));
                continue;
            }
            Optional<CreateCommand> cr = c.asCreateCommand();
            if (cr.isPresent()) {
                parts.add(truncate(String.valueOf(cr.get().getCreateArguments())));
            }
        }
        return parts.isEmpty() ? "" : String.join(" + ", parts);
    }

    /** {@code Module:Entity} — the package id is reported once at startup, not per line. */
    public static String name(Identifier id) {
        return id == null ? "?" : id.getModuleName() + ":" + id.getEntityName();
    }

    // -----------------------------------------------------------------------
    // Small shared utilities
    // -----------------------------------------------------------------------

    /** The deepest cause's message — the sentence a human actually wants. */
    public static String rootMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : t.toString();
    }

    /** The {@link StatusRuntimeException} anywhere in the cause chain, if there is one. */
    public static Optional<StatusRuntimeException> grpcCause(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof StatusRuntimeException sre) {
                return Optional.of(sre);
            }
            if (c.getCause() == c) {
                break;
            }
            c = c.getCause();
        }
        return Optional.empty();
    }

    /** Truncate for a log line, marking that it was cut rather than silently eliding. */
    public static String truncate(String s) {
        if (s == null) {
            return "";
        }
        String flat = s.replace('\n', ' ').replace('\r', ' ');
        return flat.length() <= MAX_RENDER ? flat : flat.substring(0, MAX_RENDER) + "…(truncated)";
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        Throwable c = t;
        while (c != null) {
            if (type.isInstance(c)) {
                return true;
            }
            if (c.getCause() == c) {
                break;
            }
            c = c.getCause();
        }
        return false;
    }

    private static List<Command> safeCommands(HasCommands command) {
        if (command == null) {
            return List.of();
        }
        try {
            List<Command> cs = command.commands();
            return cs == null ? List.of() : cs;
        } catch (RuntimeException e) {
            // Describing a command must NEVER be the reason a submission fails.
            return List.of();
        }
    }

    private static String firstGroup(Pattern p, String haystack, int group) {
        if (haystack == null) {
            return null;
        }
        Matcher m = p.matcher(haystack);
        return m.find() ? m.group(group) : null;
    }
}
