package org.veenix.timer.service;

import org.veenix.timer.model.ActivityType;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class ConfigurationService {

    private static final String CONFIG_FILE = "timer.properties";
    private static final String DEFAULT_ACTIVITY_TYPE_KEY = "default.activity.type";
    private static final String CSV_DELIMITER_KEY = "csv.delimiter";
    private static final String DEFAULT_DURATION_MINUTES_KEY = "default.duration.minutes";
    private static final String ROUNDING_MINUTES_KEY = "rounding.minutes";
    private static final String DEFAULT_START_TIME_KEY = "default.start.time";
    private final Properties properties;

    public ConfigurationService() {
        properties = new Properties();
        loadConfiguration();
    }

    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load configuration file", e);
            }
        } else {
            // Create default configuration
            properties.setProperty(DEFAULT_ACTIVITY_TYPE_KEY, ActivityType.DEVELOP.name());
            properties.setProperty(CSV_DELIMITER_KEY, ",");
            properties.setProperty(DEFAULT_DURATION_MINUTES_KEY, "60");
            properties.setProperty(ROUNDING_MINUTES_KEY, "5");
            properties.setProperty(DEFAULT_START_TIME_KEY, "09:00");
            saveConfiguration();
        }
    }

    private void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Timer Application Configuration");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration file", e);
        }
    }

    public ActivityType getDefaultActivityType() {
        String typeStr = properties.getProperty(DEFAULT_ACTIVITY_TYPE_KEY, ActivityType.DEVELOP.name());
        try {
            return ActivityType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ActivityType.DEVELOP;
        }
    }

    public void setDefaultActivityType(ActivityType type) {
        properties.setProperty(DEFAULT_ACTIVITY_TYPE_KEY, type.name());
        saveConfiguration();
    }

    public String getCsvDelimiter() {
        return properties.getProperty(CSV_DELIMITER_KEY, ",");
    }

    public void setCsvDelimiter(String delimiter) {
        properties.setProperty(CSV_DELIMITER_KEY, delimiter);
        saveConfiguration();
    }

    public int getDefaultDurationMinutes() {
        String durationStr = properties.getProperty(DEFAULT_DURATION_MINUTES_KEY, "60");
        try {
            int duration = Integer.parseInt(durationStr);
            return duration > 0 ? duration : 60;
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    public void setDefaultDurationMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0");
        }
        properties.setProperty(DEFAULT_DURATION_MINUTES_KEY, String.valueOf(minutes));
        saveConfiguration();
    }

    public int getRoundingMinutes() {
        String roundingStr = properties.getProperty(ROUNDING_MINUTES_KEY, "5");
        try {
            int rounding = Integer.parseInt(roundingStr);
            // Validate: must be 0, 1, 5, 10, 15, 30, or 60
            if (rounding == 0 || rounding == 1 || rounding == 5 ||
                rounding == 10 || rounding == 15 || rounding == 30 || rounding == 60) {
                return rounding;
            }
            return 0; // Invalid value, return 0 (no rounding)
        } catch (NumberFormatException e) {
            return 0; // Invalid value, return 0 (no rounding)
        }
    }

    public void setRoundingMinutes(int minutes) {
        // Validate: must be 0, 1, 5, 10, 15, 30, or 60
        if (minutes != 0 && minutes != 1 && minutes != 5 &&
            minutes != 10 && minutes != 15 && minutes != 30 && minutes != 60) {
            throw new IllegalArgumentException("Rounding must be 0, 1, 5, 10, 15, 30, or 60 minutes");
        }
        properties.setProperty(ROUNDING_MINUTES_KEY, String.valueOf(minutes));
        saveConfiguration();
    }

    public String getDefaultStartTime() {
        String timeStr = properties.getProperty(DEFAULT_START_TIME_KEY, "09:00");
        // Validate format HH:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalTime.parse(timeStr, formatter);
            return timeStr;
        } catch (DateTimeParseException e) {
            return "09:00"; // Invalid value, return default
        }
    }

    public void setDefaultStartTime(String time) {
        // Validate format HH:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalTime.parse(time, formatter);
            properties.setProperty(DEFAULT_START_TIME_KEY, time);
            saveConfiguration();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Start time must be in HH:mm format (e.g., 09:00)");
        }
    }
}
