package com.lucilla.settlement.config;

import com.lucilla.settlement.ledger.SignerProtocol;
import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code signer.reserve-models} and hands it to {@link SignerProtocol} at startup.
 *
 * <p>WHY THIS EXISTS. The issuer conditions were written for cBTC — an attestor set with a
 * periodic proof-of-reserve. Asked of cETH, whose ETH is locked in protocol-controlled
 * contracts, {@code attestor-quorum} demands a number that does not exist, and the signer's
 * only ways forward are to fail or to invent one. Inventing a number to pass a check is the
 * exact failure the protocol exists to prevent, so the protocol has to ask each asset what
 * its model can actually prove.
 *
 * <p>Configuration rather than code so onboarding an asset is a line in a file. An
 * unconfigured instrument stays on the strictest profile.
 */
@Configuration
@ConfigurationProperties(prefix = "signer")
public class SignerProtocolConfig {

    private static final Logger log = LoggerFactory.getLogger(SignerProtocolConfig.class);

    /** instrument id -> reserve model wire name. */
    private Map<String, String> reserveModels = new LinkedHashMap<>();

    public Map<String, String> getReserveModels() {
        return reserveModels;
    }

    public void setReserveModels(Map<String, String> reserveModels) {
        this.reserveModels = reserveModels == null ? new LinkedHashMap<>() : reserveModels;
    }

    @PostConstruct
    void apply() {
        SignerProtocol.configureReserveModels(reserveModels);
        if (reserveModels.isEmpty()) {
            log.warn("SIGNER no reserve models configured — every instrument will be held to the "
                    + "'attested' issuer profile, which demands an attestor quorum");
        } else {
            SignerProtocol.reserveModels().forEach((instrument, model) ->
                    log.info("SIGNER {} reserve model = {}", instrument, model.wire()));
        }
    }
}
