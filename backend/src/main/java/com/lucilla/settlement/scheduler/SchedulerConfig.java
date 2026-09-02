package com.lucilla.settlement.scheduler;

import com.lucilla.settlement.auth.AuthProperties;
import com.lucilla.settlement.ledger.StrikeCalendars;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class SchedulerConfig {

    /**
     * The strike calendars: the built-in {@code calendars/*.yml}, each overridable from
     * {@code CALENDARS_DIR} ({@code scheduler.calendars-dir}) without a rebuild.
     */
    @Bean
    public StrikeCalendars strikeCalendars(@Value("${scheduler.calendars-dir:}") String dir) {
        return StrikeCalendars.load(dir == null || dir.isBlank() ? null : Path.of(dir.trim()));
    }

    @Bean
    public ScheduleStore scheduleStore(AuthProperties props, StrikeCalendars calendars) {
        return new ScheduleStore(Path.of(props.getDataDir()), calendars);
    }
}
