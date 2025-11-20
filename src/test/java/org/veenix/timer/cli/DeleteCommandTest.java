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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeleteCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_delete_command.db";

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
    void testDeleteExistingActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("To be deleted")
                .build();

        Activity saved = activityRepository.save(activity);

        // Verify it exists
        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());

        // Delete it
        activityRepository.delete(saved.id());

        // Verify it's gone
        Optional<Activity> afterDelete = activityRepository.findById(saved.id());
        assertFalse(afterDelete.isPresent());
    }

    @Test
    void testDeleteNonExistingActivity() {
        Long nonExistingId = 999L;

        // Verify it doesn't exist
        Optional<Activity> found = activityRepository.findById(nonExistingId);
        assertFalse(found.isPresent());

        // Delete should not throw exception
        assertDoesNotThrow(() -> activityRepository.delete(nonExistingId));
    }

    @Test
    void testDeleteDoesNotAffectOtherActivities() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity1 = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 1")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(start.plusHours(1))
                .endTime(end.plusHours(1))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 2")
                .build();

        Activity activity3 = Activity.builder()
                .startTime(start.plusHours(2))
                .endTime(end.plusHours(2))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 3")
                .build();

        Activity saved1 = activityRepository.save(activity1);
        Activity saved2 = activityRepository.save(activity2);
        Activity saved3 = activityRepository.save(activity3);

        // Delete activity 2
        activityRepository.delete(saved2.id());

        // Verify only activity 2 is deleted
        Optional<Activity> found1 = activityRepository.findById(saved1.id());
        Optional<Activity> found2 = activityRepository.findById(saved2.id());
        Optional<Activity> found3 = activityRepository.findById(saved3.id());

        assertTrue(found1.isPresent());
        assertFalse(found2.isPresent());
        assertTrue(found3.isPresent());
    }

    @Test
    void testDeleteReducesTotalCount() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity1 = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 1")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(start.plusHours(1))
                .endTime(end.plusHours(1))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 2")
                .build();

        Activity saved1 = activityRepository.save(activity1);
        Activity saved2 = activityRepository.save(activity2);

        List<Activity> beforeDelete = activityRepository.findAll();
        assertEquals(2, beforeDelete.size());

        activityRepository.delete(saved1.id());

        List<Activity> afterDelete = activityRepository.findAll();
        assertEquals(1, afterDelete.size());
        assertEquals(saved2.id(), afterDelete.get(0).id());
    }

    @Test
    void testDeleteAllActivities() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity1 = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 1")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(start.plusHours(1))
                .endTime(end.plusHours(1))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 2")
                .build();

        Activity saved1 = activityRepository.save(activity1);
        Activity saved2 = activityRepository.save(activity2);

        activityRepository.delete(saved1.id());
        activityRepository.delete(saved2.id());

        List<Activity> allActivities = activityRepository.findAll();
        assertTrue(allActivities.isEmpty());
    }
}
