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

class ActivityServiceTest {

    private ActivityService activityService;
    private ActivityRepository activityRepository;
    private ConfigurationService configurationService;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_activity_service.db";

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
    void testStartActivity() {
        Activity activity = activityService.startActivity(ActivityType.DEVELOP, "Test task");

        assertNotNull(activity);
        assertNotNull(activity.id());
        assertEquals(ActivityType.DEVELOP, activity.activityType());
        assertEquals("Test task", activity.description());
        assertEquals(ActivityStatus.ACTIVE, activity.status());
        assertNotNull(activity.startTime());
        assertNull(activity.endTime());
    }

    @Test
    void testStartActivityCompletesExistingActive() {
        // Start first activity
        Activity first = activityService.startActivity(ActivityType.DEVELOP, "First task");
        assertEquals(ActivityStatus.ACTIVE, first.status());

        // Wait a moment to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Start second activity
        Activity second = activityService.startActivity(ActivityType.MEETING, "Second task");
        assertEquals(ActivityStatus.ACTIVE, second.status());

        // Verify first activity was completed
        List<Activity> completedActivities = activityRepository.findByStatus(ActivityStatus.COMPLETED);
        assertEquals(1, completedActivities.size());

        Activity completedActivity = completedActivities.get(0);
        assertEquals(first.id(), completedActivity.id());
        assertEquals(ActivityStatus.COMPLETED, completedActivity.status());
        assertNotNull(completedActivity.endTime());

        // Verify only one activity is active
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);
        assertEquals(1, activeActivities.size());
        assertEquals(second.id(), activeActivities.get(0).id());
    }

    @Test
    void testStartMultipleActivitiesSequentially() {
        Activity first = activityService.startActivity(ActivityType.DEVELOP, "First");
        Activity second = activityService.startActivity(ActivityType.SUPPORT, "Second");
        Activity third = activityService.startActivity(ActivityType.MEETING, "Third");

        // Only the last activity should be active
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);
        assertEquals(1, activeActivities.size());
        assertEquals(third.id(), activeActivities.get(0).id());

        // First two should be completed
        List<Activity> completedActivities = activityRepository.findByStatus(ActivityStatus.COMPLETED);
        assertEquals(2, completedActivities.size());
    }

    @Test
    void testStopActivity() {
        // Start an activity
        Activity activity = activityService.startActivity(ActivityType.DEVELOP, "Work task");
        assertNull(activity.endTime());
        assertEquals(ActivityStatus.ACTIVE, activity.status());

        // Wait a moment to have a duration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop the activity
        Activity stoppedActivity = activityService.stopActivity();

        assertNotNull(stoppedActivity);
        assertEquals(activity.id(), stoppedActivity.id());
        assertEquals(ActivityStatus.COMPLETED, stoppedActivity.status());
        assertNotNull(stoppedActivity.endTime());
        assertTrue(stoppedActivity.endTime().isAfter(stoppedActivity.startTime()));
    }

    @Test
    void testStopActivityWhenNoneActive() {
        Activity result = activityService.stopActivity();
        assertNull(result);
    }

    @Test
    void testStopActivityAfterAlreadyStopped() {
        // Start and stop an activity
        activityService.startActivity(ActivityType.DEVELOP, "First task");
        Activity stopped = activityService.stopActivity();
        assertNotNull(stopped);

        // Try to stop again - should return null
        Activity result = activityService.stopActivity();
        assertNull(result);
    }
}
