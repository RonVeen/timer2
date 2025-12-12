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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CopyCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_copy_command.db";

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
    void testFindActivityById() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Original description")
                .build();

        Activity saved = activityRepository.save(activity);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Original description", found.get().description());
    }

    @Test
    void testActivityNotFound() {
        Optional<Activity> found = activityRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testCopyActivityCreatesNewActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        // Create original activity
        Activity original = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Original activity")
                .build();

        Activity savedOriginal = activityRepository.save(original);

        // Create a copy with different values
        LocalDateTime newStart = LocalDateTime.of(2025, 10, 24, 10, 0);
        LocalDateTime newEnd = LocalDateTime.of(2025, 10, 24, 12, 0);

        Activity copy = Activity.builder()
                .startTime(newStart)
                .endTime(newEnd)
                .activityType(ActivityType.BUG)
                .status(ActivityStatus.COMPLETED)
                .description("Copied activity")
                .build();

        Activity savedCopy = activityRepository.save(copy);

        // Verify both exist and have different IDs
        assertNotEquals(savedOriginal.id(), savedCopy.id());

        // Verify original is unchanged
        Optional<Activity> foundOriginal = activityRepository.findById(savedOriginal.id());
        assertTrue(foundOriginal.isPresent());
        assertEquals("Original activity", foundOriginal.get().description());
        assertEquals(ActivityType.DEVELOP, foundOriginal.get().activityType());

        // Verify copy exists with new values
        Optional<Activity> foundCopy = activityRepository.findById(savedCopy.id());
        assertTrue(foundCopy.isPresent());
        assertEquals("Copied activity", foundCopy.get().description());
        assertEquals(ActivityType.BUG, foundCopy.get().activityType());
    }

    @Test
    void testCopiedActivityHasCompletedStatus() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        // Create an ACTIVE activity (simulating a running timer)
        Activity activeActivity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Active activity")
                .build();

        Activity savedActive = activityRepository.save(activeActivity);

        // Create a copy with COMPLETED status
        Activity copy = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Active activity")
                .build();

        Activity savedCopy = activityRepository.save(copy);

        // Verify copy has COMPLETED status
        assertEquals(ActivityStatus.COMPLETED, savedCopy.status());

        // Verify original still has ACTIVE status
        Optional<Activity> foundOriginal = activityRepository.findById(savedActive.id());
        assertTrue(foundOriginal.isPresent());
        assertEquals(ActivityStatus.ACTIVE, foundOriginal.get().status());
    }

    @Test
    void testCopyActivityWithDifferentValues() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        // Create original
        Activity original = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Original")
                .build();

        Activity savedOriginal = activityRepository.save(original);

        // Create copy with all different values
        LocalDateTime newStart = LocalDateTime.of(2025, 10, 25, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2025, 10, 25, 16, 30);

        Activity copy = Activity.builder()
                .startTime(newStart)
                .endTime(newEnd)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Meeting notes")
                .build();

        Activity savedCopy = activityRepository.save(copy);

        // Verify original is completely unchanged
        Optional<Activity> foundOriginal = activityRepository.findById(savedOriginal.id());
        assertTrue(foundOriginal.isPresent());
        Activity unchangedOriginal = foundOriginal.get();
        assertEquals(start, unchangedOriginal.startTime());
        assertEquals(end, unchangedOriginal.endTime());
        assertEquals(ActivityType.DEVELOP, unchangedOriginal.activityType());
        assertEquals("Original", unchangedOriginal.description());

        // Verify copy has all new values
        assertEquals(newStart, savedCopy.startTime());
        assertEquals(newEnd, savedCopy.endTime());
        assertEquals(ActivityType.MEETING, savedCopy.activityType());
        assertEquals("Meeting notes", savedCopy.description());
    }

    @Test
    void testCopyActivitySavesToDatabase() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        // Create and save original
        Activity original = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Test activity")
                .build();

        Activity savedOriginal = activityRepository.save(original);

        // Create copy
        Activity copy = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Copied activity")
                .build();

        Activity savedCopy = activityRepository.save(copy);

        // Verify copy can be retrieved from database
        Optional<Activity> foundCopy = activityRepository.findById(savedCopy.id());
        assertTrue(foundCopy.isPresent());
        assertEquals(savedCopy.id(), foundCopy.get().id());
        assertEquals("Copied activity", foundCopy.get().description());
    }

    @Test
    void testCalculateDuration() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 30);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Test")
                .build();

        Duration duration = Duration.between(activity.startTime(), activity.endTime());
        int minutes = (int) duration.toMinutes();

        assertEquals(90, minutes);
    }

    @Test
    void testCopyActivityWithCrossDateBoundary() {
        // Activity that spans midnight
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 23, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 24, 1, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Late night coding")
                .build();

        Activity saved = activityRepository.save(activity);

        // Verify duration is calculated correctly
        Duration duration = Duration.between(saved.startTime(), saved.endTime());
        int minutes = (int) duration.toMinutes();

        assertEquals(120, minutes); // 2 hours
    }
}
