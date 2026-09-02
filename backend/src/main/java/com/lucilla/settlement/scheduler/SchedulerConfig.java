package com.lucilla.settlement.scheduler;

import com.lucilla.settlement.auth.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class SchedulerConfig {

    @Bean
    public ScheduleStore scheduleStore(AuthProperties props) {
        return new ScheduleStore(Path.of(props.getDataDir()));
    }
}
