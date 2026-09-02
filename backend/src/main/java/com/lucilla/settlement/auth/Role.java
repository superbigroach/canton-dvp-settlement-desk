package com.lucilla.settlement.auth;

import java.util.Locale;

/** The roles of docs/PRODUCT-PLAN.md §3. Serialised in lower case with an underscore. */
public enum Role {
    ADMIN, SIGNER, AP, FUND_ADMIN, AUDITOR, VIEWER;

    public String wire() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Role parse(String raw) {
        if (raw == null || raw.isBlank()) return VIEWER;
        String s = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown role '" + raw + "'; expected one of "
                    + java.util.Arrays.stream(values()).map(Role::wire).toList());
        }
    }
}
