package org.veenix.timer.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivityListCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_activity_list_command.db";

    @BeforeEach
    void setUp() {
        dbConnection = new DatabaseConnection("jdbc:sqlite:" + TEST_DB);
        activityRepository = new ActivityRepositoryImpl(dbConnection);
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
        new File(TEST_DB).delete();
    }

    @Test
    void testFindByStartTimeReturnsActivitiesInAscendingOrder() {
        LocalDate today = LocalDate.now();

        // Create activities in non-sequential order
        Activity activity2 = Activity.builder()
                .startTime(today.atTime(10, 0))
                .endTime(today.atTime(11, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Second activity")
                .build();

        Activity activity1 = Activity.builder()
                .startTime(today.atTime(9, 0))
                .endTime(today.atTime(9, 30))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("First activity")
                .build();

        Activity activity3 = Activity.builder()
                .startTime(today.atTime(14, 0))
                .endTime(today.atTime(15, 0))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Third activity")
                .build();

        activityRepository.save(activity2);
        activityRepository.save(activity1);
        activityRepository.save(activity3);

        // Query for today's activities
        List<Activity> activities = activityRepository.findByStartTime(today.atStartOfDay());

        // Verify they are returned in ascending order
        assertEquals(3, activities.size());
        assertEquals("First activity", activities.get(0).description());
        assertEquals("Second activity", activities.get(1).description());
        assertEquals("Third activity", activities.get(2).description());
    }

    @Test
    void testFindByDateRangeReturnsActivitiesInAscendingOrder() {
        LocalDate date1 = LocalDate.now().minusDays(2);
        LocalDate date2 = LocalDate.now().minusDays(1);
        LocalDate date3 = LocalDate.now();

        Activity activity3 = Activity.builder()
                .startTime(date3.atTime(9, 0))
                .endTime(date3.atTime(10, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Today")
                .build();

        Activity activity1 = Activity.builder()
                .startTime(date1.atTime(9, 0))
                .endTime(date1.atTime(10, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Two days ago")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(date2.atTime(9, 0))
                .endTime(date2.atTime(10, 0))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Yesterday")
                .build();

        activityRepository.save(activity3);
        activityRepository.save(activity1);
        activityRepository.save(activity2);

        // Query for date range
        LocalDateTime from = date1.atStartOfDay();
        LocalDateTime to = date3.atTime(LocalTime.MAX);
        List<Activity> activities = activityRepository.findByDateRange(from, to);

        // Verify they are returned in ascending order
        assertEquals(3, activities.size());
        assertEquals("Two days ago", activities.get(0).description());
        assertEquals("Yesterday", activities.get(1).description());
        assertEquals("Today", activities.get(2).description());
    }

    @Test
    void testFindByDateRangeWithNoActivities() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        List<Activity> activities = activityRepository.findByDateRange(
            from.atStartOfDay(),
            to.atTime(LocalTime.MAX)
        );

        assertTrue(activities.isEmpty());
    }

    @Test
    void testFindByStartTimeWithSpecificDate() {
        LocalDate targetDate = LocalDate.now().minusDays(5);
        LocalDate otherDate = LocalDate.now();

        // Create activity on target date
        Activity targetActivity = Activity.builder()
                .startTime(targetDate.atTime(10, 0))
                .endTime(targetDate.atTime(11, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Target date activity")
                .build();

        // Create activity on different date
        Activity otherActivity = Activity.builder()
                .startTime(otherDate.atTime(10, 0))
                .endTime(otherDate.atTime(11, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Other date activity")
                .build();

        activityRepository.save(targetActivity);
        activityRepository.save(otherActivity);

        // Query for specific date
        List<Activity> activities = activityRepository.findByStartTime(targetDate.atStartOfDay());

        // Verify only target date activity is returned
        assertEquals(1, activities.size());
        assertEquals("Target date activity", activities.get(0).description());
    }

    @Test
    void testListActivitiesYesterday() {
        LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(10, 0);
        LocalDateTime today = LocalDate.now().atTime(14, 0);

        // Create activity from yesterday
        Activity yesterdayActivity = Activity.builder()
                .startTime(yesterday)
                .endTime(yesterday.plusHours(2))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Yesterday's work")
                .build();

        // Create activity from today
        Activity todayActivity = Activity.builder()
                .startTime(today)
                .endTime(today.plusHours(1))
                .activityType(ActivityType.BUG)
                .status(ActivityStatus.COMPLETED)
                .description("Today's work")
                .build();

        activityRepository.save(yesterdayActivity);
        activityRepository.save(todayActivity);

        // Test --yesterday flag
        // Should only return yesterday's activity
        List<Activity> activities = activityRepository.findByStartTime(
            LocalDate.now().minusDays(1).atStartOfDay()
        );

        assertEquals(1, activities.size());
        assertEquals("Yesterday's work", activities.get(0).description());
    }
}
