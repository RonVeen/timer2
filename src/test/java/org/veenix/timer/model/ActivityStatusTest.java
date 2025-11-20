package org.veenix.timer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityStatusTest {

    @Test
    void testEnumValues() {
        ActivityStatus[] statuses = ActivityStatus.values();
        assertEquals(3, statuses.length);
    }

    @Test
    void testEnumContainsActive() {
        assertNotNull(ActivityStatus.valueOf("ACTIVE"));
    }

    @Test
    void testEnumContainsPaused() {
        assertNotNull(ActivityStatus.valueOf("PAUSED"));
    }

    @Test
    void testEnumContainsCompleted() {
        assertNotNull(ActivityStatus.valueOf("COMPLETED"));
    }

    @Test
    void testInvalidEnumValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActivityStatus.valueOf("INVALID");
        });
    }
}
