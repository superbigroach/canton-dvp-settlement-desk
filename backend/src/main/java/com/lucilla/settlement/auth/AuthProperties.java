package com.lucilla.settlement.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identity settings — {@code auth.*} in {@code application.yml}, docs/PRODUCT-PLAN.md §3.
 *
 * <p>{@code mode} is the one that matters operationally: {@code firebase} (the image's
 * default) verifies ID tokens; {@code sandbox} accepts {@code X-Sandbox-User} and lets the
 * pre-existing operator-desk routes through with no headers, which is what keeps the
 * live desk working until the app ships its login page.
 */
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    public static final String MODE_FIREBASE = "firebase";
    public static final String MODE_SANDBOX = "sandbox";

    private String mode = MODE_FIREBASE;
    private String firebaseProjectId = "crossdesk-devnet-app";
    private String usersFile = "classpath:users.yml";
    private String dataDir = "./data";
    private String operatorParty = "Issuer";

    public boolean isSandbox() {
        return MODE_SANDBOX.equalsIgnoreCase(mode == null ? "" : mode.trim());
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getFirebaseProjectId() { return firebaseProjectId; }
    public void setFirebaseProjectId(String firebaseProjectId) { this.firebaseProjectId = firebaseProjectId; }
    public String getUsersFile() { return usersFile; }
    public void setUsersFile(String usersFile) { this.usersFile = usersFile; }
    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getOperatorParty() { return operatorParty; }
    public void setOperatorParty(String operatorParty) { this.operatorParty = operatorParty; }
}
