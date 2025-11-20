package org.veenix.timer.service;

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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_list_command.db";

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
    void testListActiveActivities() {
        // Create some activities
        LocalDateTime now = LocalDateTime.now();

        Activity active1 = Activity.builder()
                .startTime(now.minusHours(2))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Active task 1")
                .build();

        Activity active2 = Activity.builder()
                .startTime(now.minusHours(1))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Active task 2")
                .build();

        Activity completed = Activity.builder()
                .startTime(now.minusHours(3))
                .endTime(now.minusHours(2))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Completed task")
                .build();

        activityRepository.save(active1);
        activityRepository.save(active2);
        activityRepository.save(completed);

        // List active activities
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);

        assertEquals(2, activeActivities.size());
        assertTrue(activeActivities.stream().allMatch(a -> a.status() == ActivityStatus.ACTIVE));
    }

    @Test
    void testListWhenNoActiveActivities() {
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);
        assertTrue(activeActivities.isEmpty());
    }

    @Test
    void testListShowsOnlyActiveNotCompleted() {
        LocalDateTime now = LocalDateTime.now();

        // Create multiple completed activities
        for (int i = 0; i < 5; i++) {
            Activity completed = Activity.builder()
                    .startTime(now.minusHours(i + 1))
                    .endTime(now.minusMinutes(30))
                    .activityType(ActivityType.DEVELOP)
                    .status(ActivityStatus.COMPLETED)
                    .description("Completed " + i)
                    .build();
            activityRepository.save(completed);
        }

        // Create one active activity
        Activity active = Activity.builder()
                .startTime(now)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Current task")
                .build();
        activityRepository.save(active);

        // Verify only active is returned
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);
        assertEquals(1, activeActivities.size());
        assertEquals("Current task", activeActivities.get(0).description());
    }
}
