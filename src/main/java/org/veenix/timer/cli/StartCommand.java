package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import org.veenix.timer.service.ActivityService;
import org.veenix.timer.service.ConfigurationService;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

@Command(
    name = "start",
    aliases = {"st"},
    description = "Start a new activity timer",
    mixinStandardHelpOptions = true
)
public class StartCommand implements Runnable {

    @Option(names = {"-d", "--description"}, description = "Activity description")
    private String description;

    @Option(names = {"-t", "--type"}, description = "Activity type (BUG, DEVELOP, GENERAL, INFRA, MEETING, OUT_OF_OFFICE, PROBLEM, SUPPORT)")
    private String type;

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    StartTimeOptions startTimeOptions;

    static class StartTimeOptions {
        @Option(names = {"-c", "--connect"}, description = "Connect to the last activity of today (start time = last activity's end time + 1 minute)")
        boolean connect;

        @Option(names = {"-s", "--start-time"}, description = "Start time in HH:mm format")
        String startTime;
    }

    @Override
    public void run() {
        ConfigurationService configService = new ConfigurationService();
        ActivityRepository activityRepository = new ActivityRepositoryImpl(DatabaseConnection.getInstance());
        ActivityService activityService = new ActivityService(activityRepository, configService);

        // Determine activity type
        ActivityType activityType;
        if (type == null || type.isBlank()) {
            activityType = promptForActivityType(configService);
        } else {
            try {
                activityType = ActivityType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid activity type: " + type);
                System.err.println("Valid types: BUG, DEVELOP, GENERAL, INFRA, MEETING, OUT_OF_OFFICE, PROBLEM, SUPPORT");
                System.exit(1);
                return;
            }
        }

        // Determine description
        String activityDescription = description;
        if (activityDescription == null || activityDescription.isBlank()) {
            activityDescription = promptForDescription();
        }

        // Determine start time
        LocalDateTime startTime;

        if (startTimeOptions != null && startTimeOptions.connect) {
            // --connect option: use last activity's end time + 1 minute
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            List<Activity> todaysActivities = activityRepository.findByStartTime(startOfDay);

            if (todaysActivities.isEmpty()) {
                // No activities for today - prompt for start time
                System.out.println("No activities found for today.");
                LocalTime time = promptForStartTime(configService);
                startTime = LocalDate.now().atTime(time);
            } else {
                // Get the latest activity (last in the list, ordered by start_time ASC)
                Activity latestActivity = todaysActivities.get(todaysActivities.size() - 1);

                if (latestActivity.endTime() == null) {
                    System.err.println("The latest activity for today has no end time. Cannot use --connect option.");
                    System.exit(1);
                    return;
                }

                // Calculate start time as end time + 1 minute
                startTime = latestActivity.endTime().plusMinutes(1);
            }
        } else if (startTimeOptions != null && startTimeOptions.startTime != null) {
            // --start-time option provided
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            try {
                LocalTime time = LocalTime.parse(startTimeOptions.startTime, formatter);
                startTime = LocalDate.now().atTime(time);
            } catch (DateTimeParseException e) {
                System.err.println("Invalid time format for --start-time. Please use HH:mm (e.g., 09:30).");
                System.exit(1);
                return;
            }
        } else {
            // No option provided - prompt user for start time
            LocalTime time = promptForStartTimeWithCurrentDefault();
            startTime = LocalDate.now().atTime(time);
        }

        // Start the activity
        Activity activity = activityService.startActivity(activityType, activityDescription, startTime);
        System.out.println("Activity started:");
        System.out.println("  ID: " + activity.id());
        System.out.println("  Type: " + activity.activityType());
        System.out.println("  Description: " + activity.description());
        System.out.println("  Started at: " + activity.startTime());
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
            System.out.print("Enter activity description: ");
            input = scanner.nextLine().trim();

            if (input.isBlank()) {
                System.out.println("Description cannot be blank. Please try again.");
            }
        }

        return input;
    }

    private LocalTime promptForStartTime(ConfigurationService configService) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String defaultTimeStr = configService.getDefaultStartTime();

        while (true) {
            System.out.print("Enter start time (hh:mm) [" + defaultTimeStr + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use default
            if (input.isEmpty()) {
                try {
                    return LocalTime.parse(defaultTimeStr, formatter);
                } catch (DateTimeParseException e) {
                    // Should not happen as config validates, but handle gracefully
                    return LocalTime.of(9, 0);
                }
            }

            // Try to parse the time
            try {
                return LocalTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use hh:mm (e.g., 09:30).");
            }
        }
    }

    private LocalTime promptForStartTimeWithCurrentDefault() {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime currentTime = LocalTime.now();
        String currentTimeStr = currentTime.format(formatter);

        while (true) {
            System.out.print("Enter start time (hh:mm) [current time: " + currentTimeStr + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use current time
            if (input.isEmpty()) {
                return currentTime;
            }

            // Try to parse the time
            try {
                return LocalTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use hh:mm (e.g., 09:30).");
            }
        }
    }
}
