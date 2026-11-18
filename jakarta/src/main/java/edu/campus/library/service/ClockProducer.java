package edu.campus.library.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

/**
 * Tells CDI how to create a {@link Clock}. Code that needs the time injects a Clock
 * instead of calling Instant.now() directly, so a test could swap in a fixed clock.
 */
@ApplicationScoped
public class ClockProducer {

    @Produces
    public Clock clock() {
        return Clock.systemUTC();
    }
}
