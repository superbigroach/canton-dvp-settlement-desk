package com.lucilla.settlement.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Firebase ID-token verification with {@code firebase-admin}, project
 * {@code crossdesk-devnet-app}, credentials from Application Default Credentials — on
 * Cloud Run that is the service account the revision runs as; nothing is configured.
 *
 * <p>Initialised LAZILY on the first token, so a desk running in sandbox mode, or one
 * on a laptop with no ADC, boots normally and only reports the missing credentials if
 * and when a token actually arrives.
 */
public class FirebaseTokenVerifier implements TokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenVerifier.class);
    private static final String APP_NAME = "crossdesk-auth";

    private final String projectId;
    private volatile FirebaseAuth auth;
    private volatile String initFailure;

    public FirebaseTokenVerifier(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public Verified verify(String idToken) {
        FirebaseAuth a = auth();
        try {
            FirebaseToken t = a.verifyIdToken(idToken);
            return new Verified(t.getUid(), t.getEmail(), t.isEmailVerified());
        } catch (FirebaseAuthException e) {
            throw AuthException.unauthenticated("Firebase ID token rejected: "
                    + (e.getAuthErrorCode() == null ? e.getMessage() : e.getAuthErrorCode().name()));
        } catch (IllegalArgumentException e) {
            throw AuthException.unauthenticated("malformed ID token");
        }
    }

    private FirebaseAuth auth() {
        FirebaseAuth a = auth;
        if (a != null) return a;
        synchronized (this) {
            if (auth != null) return auth;
            if (initFailure != null) throw AuthException.unauthenticated(initFailure);
            try {
                FirebaseApp app;
                try {
                    app = FirebaseApp.getInstance(APP_NAME);
                } catch (IllegalStateException notYet) {
                    FirebaseOptions opts = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .setProjectId(projectId)
                            .build();
                    app = FirebaseApp.initializeApp(opts, APP_NAME);
                }
                auth = FirebaseAuth.getInstance(app);
                log.info("Firebase token verification ready for project {}", projectId);
                return auth;
            } catch (IOException e) {
                initFailure = "Firebase verification unavailable: no Application Default Credentials ("
                        + e.getMessage() + "). Set AUTH_MODE=sandbox for local work.";
                log.warn(initFailure);
                throw AuthException.unauthenticated(initFailure);
            }
        }
    }
}
