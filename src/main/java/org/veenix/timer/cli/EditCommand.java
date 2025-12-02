package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;

@Command(
    name = "edit",
    description = "Edit an activity",
    mixinStandardHelpOptions = true
)
public class EditCommand implements Runnable {

    @Parameters(index = "0", description = "Activity ID")
    private Long activityId;

    @Option(names = {"--duration"}, description = "Edit duration in minutes instead of end time")
    private boolean useDuration;

    @Override
    public void run() {
        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        // Find the activity by ID
        Optional<Activity> optionalActivity = activityRepository.findById(activityId);

        if (optionalActivity.isEmpty()) {
            System.out.println("Activity with ID " + activityId + " not found.");
            return;
        }

        Activity activity = optionalActivity.get();

        // Check if activity is active
        if (activity.status() == ActivityStatus.ACTIVE) {
            System.err.println("Cannot edit an active activity. Please stop the activity first using 'timer stop'.");
            return;
        }

        // Prompt for description
        String newDescription = promptForDescription(activity.description());

        // Prompt for start time
        LocalDateTime newStartTime = promptForStartTime(activity);

        // Prompt for end time or duration based on flag
        LocalDateTime newEndTime;
        if (useDuration) {
            // Duration mode: prompt for minutes
            int currentDuration = calculateDuration(activity);
            int newDuration = promptForDuration(currentDuration);
            newEndTime = newStartTime.plusMinutes(newDuration);
        } else {
            // End time mode: prompt for HH:mm
            newEndTime = promptForEndTime(activity, newStartTime);
        }

        // Prompt for activity type
        ActivityType newActivityType = promptForActivityType(activity.activityType());

        // Update the activity
        Activity updatedActivity = Activity.builder()
                .id(activity.id())
                .startTime(newStartTime)
                .endTime(newEndTime)
                .activityType(newActivityType)
                .status(activity.status())
                .description(newDescription)
                .build();

        activityRepository.update(updatedActivity);

        // Calculate final duration for display
        int finalDuration = calculateDuration(updatedActivity);

        System.out.println("\nActivity updated:");
        System.out.println("  ID: " + updatedActivity.id());
        System.out.println("  Type: " + updatedActivity.activityType());
        System.out.println("  Description: " + updatedActivity.description());
        System.out.println("  Start Time: " + updatedActivity.startTime());
        System.out.println("  End Time: " + updatedActivity.endTime());
        System.out.println("  Duration: " + finalDuration + " minutes");
    }

    private String promptForDescription(String currentDescription) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter description [current: " + currentDescription + "]: ");
        String input = scanner.nextLine().trim();

        // If user pressed enter without input, keep current description
        if (input.isEmpty()) {
            return currentDescription;
        }

        // If user entered something, validate it's not blank
        while (input.isBlank()) {
            System.out.println("Description cannot be blank. Please try again.");
            System.out.print("Enter description [current: " + currentDescription + "]: ");
            input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return currentDescription;
            }
        }

        return input;
    }

    private int promptForDuration(int currentMinutes) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter duration in minutes [current: " + currentMinutes + "]: ");
            String input = scanner.nextLine().trim();

            // If user pressed enter without input, keep current duration
            if (input.isEmpty()) {
                return currentMinutes;
            }

            // Try to parse the input
            try {
                int duration = Integer.parseInt(input);

                if (duration <= 0) {
                    System.out.println("Duration must be greater than 0. Please try again.");
                    continue;
                }

                return duration;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid integer.");
            }
        }
    }

    private ActivityType promptForActivityType(ActivityType currentType) {
        Scanner scanner = new Scanner(System.in);
        ActivityType[] types = ActivityType.values();

        // Display numbered list
        System.out.println("\nSelect activity type:");
        for (int i = 0; i < types.length; i++) {
            String marker = types[i] == currentType ? " (current)" : "";
            System.out.println((i + 1) + ". " + types[i] + marker);
        }

        while (true) {
            System.out.print("Enter choice (1-" + types.length + " or name) [" + currentType + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use current type
            if (input.isEmpty()) {
                return currentType;
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

    private int calculateDuration(Activity activity) {
        if (activity.startTime() == null || activity.endTime() == null) {
            return 0;
        }

        Duration duration = Duration.between(activity.startTime(), activity.endTime());
        return (int) duration.toMinutes();
    }

    private LocalDate promptForDate(String label, LocalDate defaultDate) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        while (true) {
            System.out.print("Enter " + label + " date (yyyyMMdd) [" + defaultDate.format(formatter) + "]: ");
            String input = scanner.nextLine().trim();

            // Empty input - use default date
            if (input.isEmpty()) {
                return defaultDate;
            }

            // Try to parse the date
            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyyMMdd (e.g., 20251027).");
            }
        }
    }

    private LocalDateTime promptForStartTime(Activity activity) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // Prompt for start date first
        LocalDate newDate = promptForDate("start", activity.startTime().toLocalDate());

        // Format current start time
        String currentStartTimeStr = activity.startTime() != null
            ? activity.startTime().format(timeFormatter)
            : "N/A";

        // Then prompt for start time
        while (true) {
            System.out.print("Enter start time (hh:mm) [current: " + currentStartTimeStr + "]: ");
            String input = scanner.nextLine().trim();

            // If user pressed enter without input, check if date changed
            if (input.isEmpty()) {
                // If date didn't change, return original start time
                if (newDate.isEqual(activity.startTime().toLocalDate())) {
                    return activity.startTime();
                } else {
                    // If date changed, combine new date with original time
                    return newDate.atTime(activity.startTime().toLocalTime());
                }
            }

            // Try to parse the input as HH:mm
            try {
                LocalTime newTime = LocalTime.parse(input, timeFormatter);

                // Combine the new date with the new time
                LocalDateTime newStartTime = newDate.atTime(newTime);

                return newStartTime;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time format. Please use hh:mm (e.g., 09:30).");
            }
        }
    }

    private LocalDateTime promptForEndTime(Activity activity, LocalDateTime newStartTime) {
        Scanner scanner = new Scanner(System.in);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Prompt for end date first
        LocalDate newDate = promptForDate("end", activity.endTime().toLocalDate());

        // Format current end time
        String currentEndTimeStr = activity.endTime() != null
            ? activity.endTime().format(timeFormatter)
            : "N/A";

        // Then prompt for end time
        while (true) {
            System.out.print("Enter end time (hh:mm) [current: " + currentEndTimeStr + "]: ");
            String input = scanner.nextLine().trim();

            LocalDateTime newEndTime;

            // If user pressed enter without input, check if date changed
            if (input.isEmpty()) {
                // If date didn't change, return original end time
                if (newDate.isEqual(activity.endTime().toLocalDate())) {
                    newEndTime = activity.endTime();
                } else {
                    // If date changed, combine new date with original time
                    newEndTime = newDate.atTime(activity.endTime().toLocalTime());
                }
            } else {
                // Try to parse the input as HH:mm
                try {
                    LocalTime newTime = LocalTime.parse(input, timeFormatter);

                    // Combine the new date with the new time
                    newEndTime = newDate.atTime(newTime);
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid time format. Please use hh:mm (e.g., 14:30).");
                    continue;
                }
            }

            // Validate that end time is after start time
            if (newEndTime.isBefore(newStartTime) || newEndTime.isEqual(newStartTime)) {
                System.out.println("End time must be after start time (" +
                    newStartTime.format(dateTimeFormatter) + "). Please try again.");
                continue;
            }

            return newEndTime;
        }
    }
}
