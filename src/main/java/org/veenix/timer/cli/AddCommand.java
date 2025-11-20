package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import org.veenix.timer.service.ConfigurationService;
import picocli.CommandLine.Command;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

@Command(
    name = "add",
    description = "Add a completed activity manually",
    mixinStandardHelpOptions = true
)
public class AddCommand implements Runnable {

    @Override
    public void run() {
        ConfigurationService configService = new ConfigurationService();
        ActivityRepositoryImpl activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        // Prompt for activity type
        ActivityType activityType = promptForActivityType(configService);

        // Prompt for description
        String description = promptForDescription();

        // Prompt for start date
        LocalDate startDate = promptForDate();

        // Prompt for start time
        LocalTime startTime = promptForStartTime();

        // Prompt for end time (optional)
        LocalTime endTime = promptForEndTime(startTime, configService);

        // Combine date and times
        LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(startDate, endTime);

        // Create and save activity
        Activity activity = Activity.builder()
                .startTime(startDateTime)
                .endTime(endDateTime)
                .activityType(activityType)
                .status(ActivityStatus.COMPLETED)
                .description(description)
                .build();

        Activity savedActivity = activityRepository.save(activity);

        System.out.println("Activity " + savedActivity.id() + " added: " +
            savedActivity.description() + " (" +
            savedActivity.startTime() + " to " +
            savedActivity.endTime() + ")");
    }

    private ActivityType promptForActivityType(ConfigurationService configService) {
        Scanner scanner = new Scanner(System.in);
        ActivityType defaultType = configService.getDefaultActivityType();
        ActivityType[] types = ActivityType.values();

        // Display numbered list
        System.out.println("\nSelect activity type:");
        for (int i = 0; i < types.length; i++) {
            String marker = types[i] == defaultType ? " (default)" : "";
            System.out.println((i + 1) + ". " + types[i] + marker);
        }

        while (true) {
            System.out.print("Enter choice (1-" + types.length + " or name) [" + defaultType + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use default
            if (input.isEmpty()) {
                return defaultType;
            }

            // Try to parse as number
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= types.length) {
                    return types[choice - 1];
                } else {
                    System.out.println("Invalid number. Please enter a number between 1 and " + types.length + ".");
                    continue;
                }
            } catch (NumberFormatException e) {
                // Not a number, try as name
                try {
                    return ActivityType.valueOf(input.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid activity type. Please try again.");
                }
            }
        }
    }

    private String promptForDescription() {
        Scanner scanner = new Scanner(System.in);
        String input = "";

        while (input.isBlank()) {
            System.out.print("\nEnter activity description: ");
            input = scanner.nextLine().trim();

            if (input.isBlank()) {
                System.out.println("Description cannot be blank. Please try again.");
            }
        }

        return input;
    }

    private LocalDate promptForDate() {
        Scanner scanner = new Scanner(System.in);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        while (true) {
            System.out.print("\nEnter start date (yyyyMMdd) [" + today.format(formatter) + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use today
            if (input.isEmpty()) {
                return today;
            }

            // Try to parse the date
            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyyMMdd (e.g., 20251027).");
            }
        }
    }

    private LocalTime promptForStartTime() {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        while (true) {
            System.out.print("\nEnter start time (hh:mm): ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Start time is required. Please try again.");
                continue;
            }

            // Try to parse the time
            try {
                return LocalTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use hh:mm (e.g., 09:30).");
            }
        }
    }

    private LocalTime promptForEndTime(LocalTime startTime, ConfigurationService configService) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        int defaultDuration = configService.getDefaultDurationMinutes();
        LocalTime defaultEndTime = startTime.plusMinutes(defaultDuration);

        while (true) {
            System.out.print("\nEnter end time (hh:mm) [" + defaultEndTime.format(formatter) + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use default (startTime + default duration)
            if (input.isEmpty()) {
                return defaultEndTime;
            }

            // Try to parse the time
            try {
                LocalTime endTime = LocalTime.parse(input, formatter);

                // Validate endTime is after startTime
                if (!endTime.isAfter(startTime)) {
                    System.out.println("End time must be after start time (" + startTime.format(formatter) + "). Please try again.");
                    continue;
                }

                return endTime;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use hh:mm (e.g., 11:00).");
            }
        }
    }
}
