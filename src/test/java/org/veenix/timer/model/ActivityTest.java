package org.veenix.timer.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {

    @Test
    void testRecordConstructor() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);
        Activity activity = new Activity(1L, start, end, ActivityType.DEVELOP, ActivityStatus.ACTIVE, "Test activity");

        assertEquals(1L, activity.id());
        assertEquals(start, activity.startTime());
        assertEquals(end, activity.endTime());
        assertEquals(ActivityType.DEVELOP, activity.activityType());
        assertEquals(ActivityStatus.ACTIVE, activity.status());
        assertEquals("Test activity", activity.description());
    }

    @Test
    void testNullValues() {
        Activity activity = new Activity(null, null, null, null, null, null);
        assertNull(activity.id());
        assertNull(activity.startTime());
        assertNull(activity.endTime());
        assertNull(activity.activityType());
        assertNull(activity.status());
        assertNull(activity.description());
    }

    @Test
    void testRecordEquality() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);
        Activity activity1 = new Activity(1L, start, end, ActivityType.MEETING, ActivityStatus.COMPLETED, "Team meeting");
        Activity activity2 = new Activity(1L, start, end, ActivityType.MEETING, ActivityStatus.COMPLETED, "Team meeting");
        Activity activity3 = new Activity(2L, start, end, ActivityType.MEETING, ActivityStatus.COMPLETED, "Team meeting");

        assertEquals(activity1, activity2);
        assertNotEquals(activity1, activity3);
    }

    @Test
    void testRecordHashCode() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);
        Activity activity1 = new Activity(1L, start, end, ActivityType.SUPPORT, ActivityStatus.PAUSED, "Support ticket");
        Activity activity2 = new Activity(1L, start, end, ActivityType.SUPPORT, ActivityStatus.PAUSED, "Support ticket");

        assertEquals(activity1.hashCode(), activity2.hashCode());
    }

    @Test
    void testRecordToString() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);
        Activity activity = new Activity(1L, start, end, ActivityType.GENERAL, ActivityStatus.ACTIVE, "General task");

        String result = activity.toString();
        assertTrue(result.contains("Activity"));
        assertTrue(result.contains("GENERAL"));
        assertTrue(result.contains("ACTIVE"));
    }

    @Test
    void testBuilder() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity = Activity.builder()
                .id(1L)
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Development task")
                .build();

        assertEquals(1L, activity.id());
        assertEquals(start, activity.startTime());
        assertEquals(end, activity.endTime());
        assertEquals(ActivityType.DEVELOP, activity.activityType());
        assertEquals(ActivityStatus.ACTIVE, activity.status());
        assertEquals("Development task", activity.description());
    }

    @Test
    void testBuilderPartial() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.PAUSED)
                .build();

        assertNull(activity.id());
        assertEquals(start, activity.startTime());
        assertNull(activity.endTime());
        assertEquals(ActivityType.MEETING, activity.activityType());
        assertEquals(ActivityStatus.PAUSED, activity.status());
        assertNull(activity.description());
    }

    @Test
    void testBuilderEmpty() {
        Activity activity = Activity.builder().build();

        assertNull(activity.id());
        assertNull(activity.startTime());
        assertNull(activity.endTime());
        assertNull(activity.activityType());
        assertNull(activity.status());
        assertNull(activity.description());
    }

    @Test
    void testStatusTransitions() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activeActivity = new Activity(1L, start, null, ActivityType.DEVELOP, ActivityStatus.ACTIVE, "Task");
        assertEquals(ActivityStatus.ACTIVE, activeActivity.status());

        Activity pausedActivity = new Activity(1L, start, null, ActivityType.DEVELOP, ActivityStatus.PAUSED, "Task");
        assertEquals(ActivityStatus.PAUSED, pausedActivity.status());

        Activity completedActivity = new Activity(1L, start, end, ActivityType.DEVELOP, ActivityStatus.COMPLETED, "Task");
        assertEquals(ActivityStatus.COMPLETED, completedActivity.status());
    }
}
