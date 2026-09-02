package com.lucilla.settlement.events;

import com.lucilla.settlement.auth.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class EventsConfig {

    @Bean
    public EventStore eventStore(AuthProperties props) {
        return new JsonlEventStore(Path.of(props.getDataDir()));
    }
}
