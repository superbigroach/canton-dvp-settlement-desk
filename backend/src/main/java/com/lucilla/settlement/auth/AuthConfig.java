package com.lucilla.settlement.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

/**
 * Wires the identity layer. A {@code @Configuration} rather than {@code @Component}s on
 * purpose: {@code @WebMvcTest} slices pick up every {@code Filter} bean they can see, and
 * the existing controller tests must keep running with no user store in their context.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    @Bean
    public UserStore userStore(AuthProperties props) {
        log.info("AUTH mode={} users={} data-dir={}", props.getMode(), props.getUsersFile(),
                props.getDataDir());
        return new FileUserStore(props.getUsersFile(), Path.of(props.getDataDir()));
    }

    @Bean
    public TokenVerifier tokenVerifier(AuthProperties props) {
        return new FirebaseTokenVerifier(props.getFirebaseProjectId());
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(
            AuthProperties props, UserStore users, TokenVerifier verifier,
            com.lucilla.settlement.events.EventStore events) {
        FilterRegistrationBean<AuthFilter> reg =
                new FilterRegistrationBean<>(new AuthFilter(props, users, verifier, events));
        reg.addUrlPatterns("/api/*");
        // After the acting-party alias filter (HIGHEST_PRECEDENCE), before everything else.
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
