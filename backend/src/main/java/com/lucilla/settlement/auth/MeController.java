package com.lucilla.settlement.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@code GET /api/me} — docs/PRODUCT-PLAN.md §5. */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, Object> me(HttpServletRequest req) {
        Principal p = CurrentUser.require(req);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("uid", p.uid());
        out.put("email", p.email());
        out.put("role", p.role().wire());
        out.put("party", p.party());
        out.put("seat", p.seat());
        out.put("instruments", p.instruments());
        out.put("org", p.org());
        out.put("displayName", p.displayName());
        out.put("source", p.source());
        out.put("actingAs", p.isActedAs() ? Map.of("by", p.actedBy()) : null);
        return out;
    }
}
