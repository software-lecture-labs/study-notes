package com.lectures.business.design.sales.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A test double for time: stands still until the test moves it.
 *
 * <p>
 * {@code Clock.fixed} cannot move and the system clock cannot be controlled.
 * Use cases that span days (order today, ship next week) need something in
 * between.
 */
public final class SteppingClock extends Clock {

    private Instant now;

    public SteppingClock(String isoInstant) {
        this.now = Instant.parse(isoInstant);
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("SteppingClock is UTC only");
    }
}
