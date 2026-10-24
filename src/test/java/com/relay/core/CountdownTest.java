package com.relay.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountdownTest {

    @Test
    void countsDownAndExpires() {
        Countdown countdown = new Countdown();
        countdown.reset(3);
        assertEquals(2, countdown.tick());
        assertEquals(1, countdown.tick());
        assertEquals(0, countdown.tick());
        assertEquals(0, countdown.tick());
        assertTrue(countdown.expired());
    }

    @Test
    void neverGoesBelowZero() {
        Countdown countdown = new Countdown();
        countdown.reset(5);
        countdown.add(-100);
        assertEquals(0, countdown.remaining());
        assertTrue(countdown.expired());
    }

    @Test
    void bonusTimeIsClamped() {
        Countdown countdown = new Countdown();
        countdown.reset(10);
        countdown.add(60);
        assertEquals(70, countdown.remaining());
        countdown.add(-70);
        assertEquals(0, countdown.remaining());
    }

    @Test
    void pauseStopsTicking() {
        Countdown countdown = new Countdown();
        countdown.reset(10);
        countdown.setPaused(true);
        countdown.tick();
        assertEquals(10, countdown.remaining());
        countdown.setPaused(false);
        countdown.tick();
        assertEquals(9, countdown.remaining());
    }

    @Test
    void formatsClock() {
        assertEquals("10:00", Countdown.format(600));
        assertEquals("0:09", Countdown.format(9));
        assertEquals("0:00", Countdown.format(-5));
    }

    @Test
    void notExpiredRightAfterReset() {
        Countdown countdown = new Countdown();
        countdown.reset(600);
        assertFalse(countdown.expired());
    }
}
