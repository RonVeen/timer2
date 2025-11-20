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
import org.veenix.timer.service.ActivityService;
import org.veenix.timer.service.ConfigurationService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestartCommandTest {

    private ActivityRepository activityRepository;
    private ActivityService activityService;
    private ConfigurationService configurationService;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_restart_command.db";

    @BeforeEach
    void setUp() {
        dbConnection = new DatabaseConnection("jdbc:sqlite:" + TEST_DB);
        activityRepository = new ActivityRepositoryImpl(dbConnection);
        configurationService = new ConfigurationService();
        activityService = new ActivityService(activityRepository, configurationService);
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
        new File(TEST_DB).delete();
    }

    @Test
    void testRestartActivityWithNoActiveActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity sourceActivity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Original task")
                .build();

        Activity savedSource = activityRepository.save(sourceActivity);

        // Restart the activity
        Activity newActivity = activityService.restartActivity(savedSource.id());

        assertNotNull(newActivity);
        assertNotEquals(savedSource.id(), newActivity.id());
        assertEquals(sourceActivity.activityType(), newActivity.activityType());
        assertEquals(sourceActivity.description(), newActivity.description());
        assertEquals(ActivityStatus.ACTIVE, newActivity.status());
        assertNotNull(newActivity.startTime());
        assertNull(newActivity.endTime());
    }

    @Test
    void testRestartActivityWithActiveActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        // Create source activity
        Activity sourceActivity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Task to restart")
                .build();

        Activity savedSource = activityRepository.save(sourceActivity);

        // Create active activity
        Activity activeActivity = Activity.builder()
                .startTime(LocalDateTime.now().minusMinutes(30))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Currently active")
                .build();

        Activity savedActive = activityRepository.save(activeActivity);

        // Restart should stop active and create new
        Activity newActivity = activityService.restartActivity(savedSource.id());

        assertNotNull(newActivity);
        assertEquals(ActivityStatus.ACTIVE, newActivity.status());
        assertEquals(sourceActivity.description(), newActivity.description());

        // Verify old active activity is now completed
        var stoppedActivity = activityRepository.findById(savedActive.id());
        assertTrue(stoppedActivity.isPresent());
        assertEquals(ActivityStatus.COMPLETED, stoppedActivity.get().status());
        assertNotNull(stoppedActivity.get().endTime());
    }

    @Test
    void testRestartNonExistentActivity() {
        Long nonExistentId = 999L;

        Activity result = activityService.restartActivity(nonExistentId);

        assertNull(result);
    }

    @Test
    void testRestartCopiesCorrectFields() {
        Activity sourceActivity = Activity.builder()
                .startTime(LocalDateTime.of(2025, 10, 23, 9, 0))
                .endTime(LocalDateTime.of(2025, 10, 23, 10, 0))
                .activityType(ActivityType.BUG)
                .status(ActivityStatus.COMPLETED)
                .description("Fix critical bug")
                .build();

        Activity savedSource = activityRepository.save(sourceActivity);

        Activity newActivity = activityService.restartActivity(savedSource.id());

        assertNotNull(newActivity);
        assertEquals(ActivityType.BUG, newActivity.activityType());
        assertEquals("Fix critical bug", newActivity.description());
        assertNotEquals(savedSource.id(), newActivity.id());
    }

    @Test
    void testRestartSetsStatusToActive() {
        Activity sourceActivity = Activity.builder()
                .startTime(LocalDateTime.of(2025, 10, 23, 9, 0))
                .endTime(LocalDateTime.of(2025, 10, 23, 10, 0))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Help user")
                .build();

        Activity savedSource = activityRepository.save(sourceActivity);

        Activity newActivity = activityService.restartActivity(savedSource.id());

        assertNotNull(newActivity);
        assertEquals(ActivityStatus.ACTIVE, newActivity.status());
    }

    @Test
    void testRestartCreatesOnlyOneActiveActivity() {
        Activity sourceActivity = Activity.builder()
                .startTime(LocalDateTime.of(2025, 10, 23, 9, 0))
                .endTime(LocalDateTime.of(2025, 10, 23, 10, 0))
                .activityType(ActivityType.GENERAL)
                .status(ActivityStatus.COMPLETED)
                .description("General task")
                .build();

        Activity savedSource = activityRepository.save(sourceActivity);

        Activity newActivity = activityService.restartActivity(savedSource.id());

        // Verify only one active activity exists
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);
        assertEquals(1, activeActivities.size());
        assertEquals(newActivity.id(), activeActivities.get(0).id());
    }
}
