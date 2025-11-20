package org.veenix.timer.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTypeTest {

    @Test
    void testEnumValues() {
        ActivityType[] types = ActivityType.values();
        assertEquals(8, types.length);
    }

    @Test
    void testEnumContainsBug() {
        assertNotNull(ActivityType.valueOf("BUG"));
    }

    @Test
    void testEnumContainsDevelop() {
        assertNotNull(ActivityType.valueOf("DEVELOP"));
    }

    @Test
    void testEnumContainsGeneral() {
        assertNotNull(ActivityType.valueOf("GENERAL"));
    }

    @Test
    void testEnumContainsInfra() {
        assertNotNull(ActivityType.valueOf("INFRA"));
    }

    @Test
    void testEnumContainsMeeting() {
        assertNotNull(ActivityType.valueOf("MEETING"));
    }

    @Test
    void testEnumContainsOutOfOffice() {
        assertNotNull(ActivityType.valueOf("OUT_OF_OFFICE"));
    }

    @Test
    void testEnumContainsProblem() {
        assertNotNull(ActivityType.valueOf("PROBLEM"));
    }

    @Test
    void testEnumContainsSupport() {
        assertNotNull(ActivityType.valueOf("SUPPORT"));
    }

    @Test
    void testInvalidEnumValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ActivityType.valueOf("INVALID");
        });
    }
}
