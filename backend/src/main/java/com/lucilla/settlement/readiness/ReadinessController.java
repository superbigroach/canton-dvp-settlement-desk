package com.lucilla.settlement.readiness;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The readiness endpoints — the answer to a vendor questionnaire, generated.
 *
 * <p>Two routes, and the difference between them is deliberate.
 *
 * <p>{@code /api/admin/readiness} is the whole register, admin-gated
 * ({@code AuthRoutes} sends everything under {@code /api/admin/} to an admin). A client
 * under an NDA gets this, and so does the operator, because the value of the register is
 * that it lists what is MISSING.
 *
 * <p>{@code /api/readiness} is public and carries only the controls explicitly marked
 * {@code publish: true}, plus counts. The open web does not get an itemised list of what
 * this desk does not have — not because it is a secret, but because an inventory of
 * absent controls, out of context and permanently indexed, is a different artefact from
 * an honest answer to somebody who asked. What is published is published because it is
 * true and load-bearing: the trust level, the daily record, the fact that we are not a
 * regulated administrator.
 */
@RestController
public class ReadinessController {

    private final ControlsRegister register;

    public ReadinessController(ControlsRegister register) {
        this.register = register;
    }

    /** Public: the counts, and the controls we choose to stand behind in the open. */
    @GetMapping("/api/readiness")
    public Map<String, Object> publicReadiness() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", register.summary());
        out.put("controls", register.published());
        out.put("note", "A published subset. The full register, including every control not "
                + "in place, is available to a counterparty under NDA — ask for it; it is a "
                + "single endpoint, not a document we assemble by hand.");
        return out;
    }

    /** Admin: everything, worst first. */
    @GetMapping("/api/admin/readiness")
    public Map<String, Object> fullReadiness() {
        List<Control> all = register.all();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", register.summary());
        out.put("controls", all);
        return out;
    }
}
