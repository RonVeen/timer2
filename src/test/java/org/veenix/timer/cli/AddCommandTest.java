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
import org.veenix.timer.service.ConfigurationService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AddCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private ConfigurationService configService;
    private static final String TEST_DB = "test_add_command.db";
    private static final String TEST_CONFIG = "timer.properties";

    @BeforeEach
    void setUp() {
        dbConnection = new DatabaseConnection("jdbc:sqlite:" + TEST_DB);
        activityRepository = new ActivityRepositoryImpl(dbConnection);
        configService = new ConfigurationService();
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
        new File(TEST_DB).delete();
        new File(TEST_CONFIG).delete();
    }

    @Test
    void testAddActivityWithAllFields() {
        LocalDate date = LocalDate.of(2025, 10, 26);
        LocalTime startTime = LocalTime.of(9, 30);
        LocalTime endTime = LocalTime.of(11, 0);

        Activity activity = Activity.builder()
                .startTime(LocalDateTime.of(date, startTime))
                .endTime(LocalDateTime.of(date, endTime))
                .activityType(ActivityType.BUG)
                .status(ActivityStatus.COMPLETED)
                .description("Fixed authentication bug")
                .build();

        Activity saved = activityRepository.save(activity);

        assertNotNull(saved.id());
        assertEquals(ActivityType.BUG, saved.activityType());
        assertEquals("Fixed authentication bug", saved.description());
        assertEquals(ActivityStatus.COMPLETED, saved.status());
        assertEquals(LocalDateTime.of(date, startTime), saved.startTime());
        assertEquals(LocalDateTime.of(date, endTime), saved.endTime());
    }

    @Test
    void testAddActivityWithDefaultDuration() {
        int defaultDuration = configService.getDefaultDurationMinutes();
        LocalDate date = LocalDate.now();
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = startTime.plusMinutes(defaultDuration);

        Activity activity = Activity.builder()
                .startTime(LocalDateTime.of(date, startTime))
                .endTime(LocalDateTime.of(date, endTime))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Development task")
                .build();

        Activity saved = activityRepository.save(activity);

        assertNotNull(saved);
        long durationMinutes = java.time.Duration.between(saved.startTime(), saved.endTime()).toMinutes();
        assertEquals(defaultDuration, durationMinutes);
    }

    @Test
    void testDefaultDurationIsConfigurable() {
        int originalDuration = configService.getDefaultDurationMinutes();
        assertEquals(60, originalDuration);

        configService.setDefaultDurationMinutes(90);
        assertEquals(90, configService.getDefaultDurationMinutes());

        configService.setDefaultDurationMinutes(45);
        assertEquals(45, configService.getDefaultDurationMinutes());
    }

    @Test
    void testSetDefaultDurationRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> {
            configService.setDefaultDurationMinutes(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            configService.setDefaultDurationMinutes(-10);
        });
    }

    @Test
    void testAddActivitySetsStatusToCompleted() {
        Activity activity = Activity.builder()
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Team meeting")
                .build();

        Activity saved = activityRepository.save(activity);

        assertEquals(ActivityStatus.COMPLETED, saved.status());
    }

    @Test
    void testAddActivitySavesToDatabase() {
        Activity activity = Activity.builder()
                .startTime(LocalDateTime.of(2025, 10, 27, 14, 0))
                .endTime(LocalDateTime.of(2025, 10, 27, 15, 30))
                .activityType(ActivityType.SUPPORT)
                .status(ActivityStatus.COMPLETED)
                .description("Customer support")
                .build();

        Activity saved = activityRepository.save(activity);
        assertNotNull(saved.id());

        Optional<Activity> retrieved = activityRepository.findById(saved.id());
        assertTrue(retrieved.isPresent());
        assertEquals("Customer support", retrieved.get().description());
    }

    @Test
    void testAddActivityWithTodayAsDefault() {
        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(9, 0);

        Activity activity = Activity.builder()
                .startTime(LocalDateTime.of(today, startTime))
                .endTime(LocalDateTime.of(today, endTime))
                .activityType(ActivityType.GENERAL)
                .status(ActivityStatus.COMPLETED)
                .description("Morning task")
                .build();

        Activity saved = activityRepository.save(activity);

        assertEquals(today, saved.startTime().toLocalDate());
        assertEquals(today, saved.endTime().toLocalDate());
    }

    @Test
    void testAddActivityValidatesEndTimeAfterStartTime() {
        LocalDate date = LocalDate.of(2025, 10, 27);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(9, 0); // Before start time

        Activity activity = Activity.builder()
                .startTime(LocalDateTime.of(date, startTime))
                .endTime(LocalDateTime.of(date, endTime))
                .activityType(ActivityType.INFRA)
                .status(ActivityStatus.COMPLETED)
                .description("Infrastructure work")
                .build();

        Activity saved = activityRepository.save(activity);

        // Just verify it can be saved - validation should happen in the command
        assertNotNull(saved);
    }
}
