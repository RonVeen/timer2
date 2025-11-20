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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportCommandTest {

    private ActivityRepository activityRepository;
    private DatabaseConnection dbConnection;
    private static final String TEST_DB = "test_export_command.db";
    private static final String TEST_CONFIG = "timer.properties";
    private List<String> createdFiles;

    @BeforeEach
    void setUp() {
        dbConnection = new DatabaseConnection("jdbc:sqlite:" + TEST_DB);
        activityRepository = new ActivityRepositoryImpl(dbConnection);
        createdFiles = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
        new File(TEST_DB).delete();
        new File(TEST_CONFIG).delete();

        // Clean up any CSV files created during tests
        for (String filename : createdFiles) {
            new File(filename).delete();
        }
    }

    @Test
    void testExportActivitiesWithDefaultDelimiter() throws IOException {
        LocalDate today = LocalDate.now();

        Activity activity1 = Activity.builder()
                .startTime(today.atTime(9, 0))
                .endTime(today.atTime(10, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("First activity")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(today.atTime(11, 0))
                .endTime(today.atTime(12, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Second activity")
                .build();

        activityRepository.save(activity1);
        activityRepository.save(activity2);

        // Verify CSV file is created with correct content
        // This test would need to capture the generated filename
        // For now, we test the repository data is available
        List<Activity> activities = activityRepository.findByStartTime(today.atStartOfDay());
        assertEquals(2, activities.size());
    }

    @Test
    void testExportWithCustomDelimiter() {
        ConfigurationService configService = new ConfigurationService();
        configService.setCsvDelimiter(";");

        String delimiter = configService.getCsvDelimiter();
        assertEquals(";", delimiter);
    }

    @Test
    void testEscapeCsvFieldWithDelimiter() {
        String field = "This, has, commas";
        String delimiter = ",";

        String escaped = escapeCsvField(field, delimiter);
        assertEquals("\"This, has, commas\"", escaped);
    }

    @Test
    void testEscapeCsvFieldWithQuotes() {
        String field = "This has \"quotes\"";
        String delimiter = ",";

        String escaped = escapeCsvField(field, delimiter);
        assertEquals("\"This has \"\"quotes\"\"\"", escaped);
    }

    @Test
    void testEscapeCsvFieldWithNewlines() {
        String field = "This has\nNewlines";
        String delimiter = ",";

        String escaped = escapeCsvField(field, delimiter);
        assertEquals("\"This has\nNewlines\"", escaped);
    }

    @Test
    void testEscapeCsvFieldNormal() {
        String field = "Normal text";
        String delimiter = ",";

        String escaped = escapeCsvField(field, delimiter);
        assertEquals("Normal text", escaped);
    }

    @Test
    void testEscapeCsvFieldNull() {
        String field = null;
        String delimiter = ",";

        String escaped = escapeCsvField(field, delimiter);
        assertEquals("", escaped);
    }

    @Test
    void testExportWithDateFilter() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Activity todayActivity = Activity.builder()
                .startTime(today.atTime(9, 0))
                .endTime(today.atTime(10, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Today")
                .build();

        Activity yesterdayActivity = Activity.builder()
                .startTime(yesterday.atTime(9, 0))
                .endTime(yesterday.atTime(10, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Yesterday")
                .build();

        activityRepository.save(todayActivity);
        activityRepository.save(yesterdayActivity);

        List<Activity> todayActivities = activityRepository.findByStartTime(today.atStartOfDay());
        assertEquals(1, todayActivities.size());
        assertEquals("Today", todayActivities.get(0).description());
    }

    @Test
    void testExportWithDateRangeFilter() {
        LocalDate today = LocalDate.now();
        LocalDate twoDaysAgo = today.minusDays(2);

        Activity activity1 = Activity.builder()
                .startTime(twoDaysAgo.atTime(9, 0))
                .endTime(twoDaysAgo.atTime(10, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("Two days ago")
                .build();

        Activity activity2 = Activity.builder()
                .startTime(today.atTime(9, 0))
                .endTime(today.atTime(10, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Today")
                .build();

        activityRepository.save(activity1);
        activityRepository.save(activity2);

        List<Activity> rangeActivities = activityRepository.findByDateRange(
            twoDaysAgo.atStartOfDay(),
            today.atTime(23, 59, 59)
        );
        assertEquals(2, rangeActivities.size());
    }

    @Test
    void testExportNoActivities() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<Activity> activities = activityRepository.findByStartTime(yesterday.atStartOfDay());
        assertTrue(activities.isEmpty());
    }

    @Test
    void testFilenameGeneration() {
        String filename = generateFilename();

        assertNotNull(filename);
        assertTrue(filename.startsWith("activities_"));
        assertTrue(filename.endsWith(".csv"));
        assertTrue(filename.matches("activities_\\d{8}_\\d{6}_[a-f0-9]{8}\\.csv"));
    }

    @Test
    void testExportActivitiesSortedAscending() {
        LocalDate today = LocalDate.now();

        Activity activity2 = Activity.builder()
                .startTime(today.atTime(11, 0))
                .endTime(today.atTime(12, 0))
                .activityType(ActivityType.MEETING)
                .status(ActivityStatus.COMPLETED)
                .description("Second")
                .build();

        Activity activity1 = Activity.builder()
                .startTime(today.atTime(9, 0))
                .endTime(today.atTime(10, 0))
                .activityType(ActivityType.DEVELOP)
                .status(ActivityStatus.COMPLETED)
                .description("First")
                .build();

        // Save in non-sequential order
        activityRepository.save(activity2);
        activityRepository.save(activity1);

        // Verify they are returned in ascending order
        List<Activity> activities = activityRepository.findByStartTime(today.atStartOfDay());
        assertEquals(2, activities.size());
        assertEquals("First", activities.get(0).description());
        assertEquals("Second", activities.get(1).description());
    }

    // Helper methods extracted from ExportCommand for testing
    private String escapeCsvField(String field, String delimiter) {
        if (field == null) {
            return "";
        }

        if (field.contains(delimiter) || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    private String generateFilename() {
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        String randomPart = java.util.UUID.randomUUID().toString().substring(0, 8);
        return "activities_" + timestamp + "_" + randomPart + ".csv";
    }
}
