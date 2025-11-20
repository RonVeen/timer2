package org.veenix.timer.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ActivityRepositoryTest {

    private ActivityRepository repository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_timer.db";

    @BeforeEach
    void setUp() {
        dbConnection = new DatabaseConnection("jdbc:sqlite:" + TEST_DB);
        repository = new ActivityRepositoryImpl(dbConnection);
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
        new File(TEST_DB).delete();
    }

    @Test
    void testSaveActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        Activity activity = Activity.builder()
                .startTime(start)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Test activity")
                .build();

        Activity saved = repository.save(activity);

        assertNotNull(saved.id());
        assertEquals(start, saved.startTime());
        assertEquals(ActivityType.DEVELOP, saved.activityType());
        assertEquals(ActivityStatus.ACTIVE, saved.status());
        assertEquals("Test activity", saved.description());
    }

    @Test
    void testFindById() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        Activity activity = Activity.builder()
                .startTime(start)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Meeting")
                .build();

        Activity saved = repository.save(activity);
        Optional<Activity> found = repository.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals(saved.id(), found.get().id());
        assertEquals(ActivityType.MEETING, found.get().activityType());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Activity> found = repository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdateActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        Activity activity = Activity.builder()
                .startTime(start)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Original")
                .build();

        Activity saved = repository.save(activity);

        LocalDateTime end = LocalDateTime.of(2025, 10, 23, 10, 0);
        Activity updated = Activity.builder()
                .id(saved.id())
                .startTime(saved.startTime())
                .endTime(end)
                .activityType(saved.activityType())
                .status(ActivityStatus.COMPLETED)
                .description("Updated")
                .build();

        Activity result = repository.update(updated);

        assertEquals(saved.id(), result.id());
        assertEquals(end, result.endTime());
        assertEquals(ActivityStatus.COMPLETED, result.status());
        assertEquals("Updated", result.description());
    }

    @Test
    void testUpdateWithoutId() {
        Activity activity = Activity.builder()
                .startTime(LocalDateTime.now())
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .build();

        assertThrows(IllegalArgumentException.class, () -> repository.update(activity));
    }

    @Test
    void testDeleteActivity() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);
        Activity activity = Activity.builder()
                .startTime(start)
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.ACTIVE)
                .description("To delete")
                .build();

        Activity saved = repository.save(activity);
        repository.delete(saved.id());

        Optional<Activity> found = repository.findById(saved.id());
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        LocalDateTime start1 = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime start2 = LocalDateTime.of(2025, 10, 23, 10, 0);

        repository.save(Activity.builder()
                .startTime(start1)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Activity 1")
                .build());

        repository.save(Activity.builder()
                .startTime(start2)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Activity 2")
                .build());

        List<Activity> activities = repository.findAll();
        assertEquals(2, activities.size());
    }

    @Test
    void testFindByStatus() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Active 1")
                .build());

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Active 2")
                .build());

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Completed")
                .build());

        List<Activity> activeActivities = repository.findByStatus(ActivityStatus.ACTIVE);
        assertEquals(2, activeActivities.size());
        assertTrue(activeActivities.stream().allMatch(a -> a.status() == ActivityStatus.ACTIVE));
    }

    @Test
    void testFindByType() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 23, 9, 0);

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Dev 1")
                .build());

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Dev 2")
                .build());

        repository.save(Activity.builder()
                .startTime(start)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.ACTIVE)
                .description("Meeting")
                .build());

        List<Activity> devActivities = repository.findByType(ActivityType.DEVELOP);
        assertEquals(2, devActivities.size());
        assertTrue(devActivities.stream().allMatch(a -> a.activityType() == ActivityType.DEVELOP));
    }

    @Test
    void testFindByStartTime() {
        LocalDateTime date1 = LocalDateTime.of(2025, 10, 23, 9, 0);
        LocalDateTime date2 = LocalDateTime.of(2025, 10, 23, 14, 30);
        LocalDateTime date3 = LocalDateTime.of(2025, 10, 24, 10, 0);

        repository.save(Activity.builder()
                .startTime(date1)
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.ACTIVE)
                .description("Morning task")
                .build());

        repository.save(Activity.builder()
                .startTime(date2)
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Afternoon meeting")
                .build());

        repository.save(Activity.builder()
                .startTime(date3)
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.ACTIVE)
                .description("Next day task")
                .build());

        List<Activity> oct23Activities = repository.findByStartTime(date1);
        assertEquals(2, oct23Activities.size());
        assertTrue(oct23Activities.stream()
                .allMatch(a -> a.startTime().toLocalDate().equals(date1.toLocalDate())));

        List<Activity> oct24Activities = repository.findByStartTime(date3);
        assertEquals(1, oct24Activities.size());
        assertEquals("Next day task", oct24Activities.get(0).description());
    }
}
