package com.lucilla.settlement.auth;

import org.springframework.http.HttpStatus;

/** 401 (no usable identity) or 403 (identity known, role insufficient). */
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static AuthException unauthenticated(String why) {
        return new AuthException(HttpStatus.UNAUTHORIZED, why);
    }

    public static AuthException forbidden(String why) {
        return new AuthException(HttpStatus.FORBIDDEN, why);
    }
}
