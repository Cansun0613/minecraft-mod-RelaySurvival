package com.relay.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnQueueTest {

    @Test
    void walksThroughOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b), 1);
        assertEquals(a, queue.current());
        assertTrue(queue.advance());
        assertEquals(b, queue.current());
        assertFalse(queue.advance());
        assertNull(queue.peekNext());
        assertEquals(2, queue.totalTurns());
    }

    @Test
    void cyclesForMultipleRounds() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b), 2);
        assertEquals(a, queue.current());
        assertTrue(queue.advance());
        assertEquals(b, queue.current());
        assertTrue(queue.advance());
        assertEquals(a, queue.current());
        assertTrue(queue.advance());
        assertEquals(b, queue.current());
        assertFalse(queue.advance());
        assertEquals(4, queue.turnNumber() + 1);
        assertEquals(4, queue.totalTurns());
    }

    @Test
    void infiniteRoundsNeverEnd() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b), 0);
        for (int i = 0; i < 50; i++) {
            assertTrue(queue.advance());
        }
        assertEquals(0, queue.totalTurns());
        assertEquals(50, queue.turnNumber());
    }

    @Test
    void peekNextWrapsWhenRoundsRemain() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b), 2);
        queue.advance();
        assertEquals(a, queue.peekNext());
    }

    @Test
    void peekNextIsNullOnLastTurn() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b), 1);
        queue.advance();
        assertNull(queue.peekNext());
    }

    @Test
    void removingCurrentPlayerKeepsIndexSane() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b, c), 0);
        assertTrue(queue.remove(b));
        assertEquals(2, queue.total());
        queue.advance();
        assertEquals(c, queue.current());
    }

    @Test
    void nextOnlineSkipsOffline() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a, b, c), 1);
        assertEquals(c, queue.nextOnline(id -> id.equals(c)).orElseThrow());
        assertTrue(queue.nextOnline(id -> false).isEmpty());
    }

    @Test
    void removingLastPlayerEndsQueue() {
        UUID a = UUID.randomUUID();
        TurnQueue queue = new TurnQueue();
        queue.start(List.of(a), 0);
        queue.remove(a);
        assertFalse(queue.active());
        assertNull(queue.current());
    }
}
