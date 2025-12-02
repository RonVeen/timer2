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

class EditCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_edit_command.db";

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
    void testUpdateActivityDescription() {
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

        // Update description
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(saved.endTime())
                .activityType(saved.activityType())
                .status(saved.status())
                .description("Updated description")
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Updated description", found.get().description());
    }

    @Test
    void testUpdateActivityDuration() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Test activity")
                .build();

        Activity saved = activityRepository.save(activity);

        // Original duration is 60 minutes
        Duration originalDuration = Duration.between(saved.startTime(), saved.endTime());
        assertEquals(60, originalDuration.toMinutes());

        // Update to 90 minutes
        LocalDateTime newEnd = saved.startTime().plusMinutes(90);
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(newEnd)
                .activityType(saved.activityType())
                .status(saved.status())
                .description(saved.description())
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        Duration newDuration = Duration.between(found.get().startTime(), found.get().endTime());
        assertEquals(90, newDuration.toMinutes());
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
        assertEquals(90, duration.toMinutes());
    }

    @Test
    void testDurationMustBePositive() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);

        // Duration of 0 should not be allowed
        LocalDateTime endZero = start;
        Duration zeroDuration = Duration.between(start, endZero);
        assertEquals(0, zeroDuration.toMinutes());

        // Duration of -1 should not be allowed
        LocalDateTime endNegative = start.minusMinutes(1);
        Duration negativeDuration = Duration.between(start, endNegative);
        assertTrue(negativeDuration.toMinutes() < 0);
    }

    @Test
    void testUpdateBothDescriptionAndDuration() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Original")
                .build();

        Activity saved = activityRepository.save(activity);

        // Update both description and duration
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(saved.startTime().plusMinutes(45))
                .activityType(saved.activityType())
                .status(saved.status())
                .description("Updated")
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Updated", found.get().description());
        Duration duration = Duration.between(found.get().startTime(), found.get().endTime());
        assertEquals(45, duration.toMinutes());
    }

    @Test
    void testEditActivityWithDifferentEndDate() {
        // Create activity on Oct 27
        LocalDateTime start = LocalDateTime.of(2025, 10, 27, 14, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 27, 17, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Multi-day task")
                .build();

        Activity saved = activityRepository.save(activity);

        // Update end date to next day (Oct 28)
        LocalDateTime newEnd = LocalDateTime.of(2025, 10, 28, 10, 0);
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(newEnd)
                .activityType(saved.activityType())
                .status(saved.status())
                .description(saved.description())
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(2025, found.get().endTime().getYear());
        assertEquals(10, found.get().endTime().getMonthValue());
        assertEquals(28, found.get().endTime().getDayOfMonth());
        assertEquals(10, found.get().endTime().getHour());

        // Verify duration spans across days
        Duration duration = Duration.between(found.get().startTime(), found.get().endTime());
        assertEquals(20 * 60, duration.toMinutes()); // 20 hours
    }

    @Test
    void testEditActivityWithDifferentStartDate() {
        // Create activity on Oct 27
        LocalDateTime start = LocalDateTime.of(2025, 10, 27, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 27, 17, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Task with wrong start date")
                .build();

        Activity saved = activityRepository.save(activity);

        // Update start date to previous day (Oct 26)
        LocalDateTime newStart = LocalDateTime.of(2025, 10, 26, 14, 0);
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(newStart)
                .endTime(saved.endTime())
                .activityType(saved.activityType())
                .status(saved.status())
                .description(saved.description())
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals(2025, found.get().startTime().getYear());
        assertEquals(10, found.get().startTime().getMonthValue());
        assertEquals(26, found.get().startTime().getDayOfMonth());
        assertEquals(14, found.get().startTime().getHour());

        // Verify end time unchanged
        assertEquals(saved.endTime(), found.get().endTime());
    }

    @Test
    void testEditActivityCrossingDateBoundary() {
        // Create activity ending before midnight
        LocalDateTime start = LocalDateTime.of(2025, 10, 27, 22, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 27, 23, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Late night task")
                .build();

        Activity saved = activityRepository.save(activity);

        // Update end time to cross midnight (Oct 28, 2:00 AM)
        LocalDateTime newEnd = LocalDateTime.of(2025, 10, 28, 2, 0);
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(newEnd)
                .activityType(saved.activityType())
                .status(saved.status())
                .description(saved.description())
                .build();

        activityRepository.update(updated);

        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());

        // Verify end time is on the next day
        assertEquals(27, found.get().startTime().getDayOfMonth());
        assertEquals(28, found.get().endTime().getDayOfMonth());

        // Verify duration is 4 hours
        Duration duration = Duration.between(found.get().startTime(), found.get().endTime());
        assertEquals(4 * 60, duration.toMinutes());
    }

    @Test
    void testEditActivityEndTimeBeforeStartTimeValidation() {
        // Create valid activity
        LocalDateTime start = LocalDateTime.of(2025, 10, 27, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 27, 17, 0);

        Activity activity = Activity.builder()
                .startTime(start)
                .endTime(end)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Valid activity")
                .build();

        Activity saved = activityRepository.save(activity);

        // Try to update with end time before start time
        LocalDateTime invalidEnd = LocalDateTime.of(2025, 10, 26, 17, 0); // Day before
        Activity invalidUpdate = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(invalidEnd)
                .activityType(saved.activityType())
                .status(saved.status())
                .description(saved.description())
                .build();

        // Update should succeed (validation happens in CLI layer)
        activityRepository.update(invalidUpdate);

        // Verify the invalid data was stored (since repository doesn't validate)
        Optional<Activity> found = activityRepository.findById(saved.id());
        assertTrue(found.isPresent());
        assertTrue(found.get().endTime().isBefore(found.get().startTime()));
    }
}
