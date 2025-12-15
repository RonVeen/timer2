package org.veenix.timer.cli;

import org.veenix.timer.cli.util.ActivityTypePrompt;
import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.Command;
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
    name = "copy",
    aliases = {"cp"},
    description = "Copy an activity with edited values",
    mixinStandardHelpOptions = true
)
public class CopyCommand implements Runnable {

    @Parameters(index = "0", description = "Activity ID to copy")
    private Long activityId;

    // Reusable scanner - do not close as it wraps System.in
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void run() {
        // Step 1: Validate input
        if (activityId == null || activityId <= 0) {
            System.out.println("Activity ID must be a positive number.");
            return;
        }

        // Step 2: Initialize repository
        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        // Step 3: Look up the activity by ID
        Optional<Activity> optionalActivity = activityRepository.findById(activityId);

        // Step 4: Check if activity exists
        if (optionalActivity.isEmpty()) {
            System.out.println("Activity with ID " + activityId + " not found.");
            return;
        }

        Activity originalActivity = optionalActivity.get();

        // Step 5a: Prompt for activity type
        ActivityType newActivityType = promptForActivityType(originalActivity.activityType());

        // Step 5b: Prompt for description
        String newDescription = promptForDescription(originalActivity.description());

        // Step 5c: Prompt for start time (includes date + time)
        LocalDateTime newStartTime = promptForStartTime(originalActivity);

        // Step 5d: Prompt for end time (includes date + time)
        LocalDateTime newEndTime = promptForEndTime(originalActivity, newStartTime);

        // Step 6: Create new COMPLETED activity (no ID set - repository will assign)
        Activity newActivity = Activity.builder()
                .startTime(newStartTime)
                .endTime(newEndTime)
                .activityType(newActivityType)
                .status(ActivityStatus.COMPLETED)
                .description(newDescription)
                .build();

        // Step 7: Save the new activity
        Activity savedActivity = activityRepository.save(newActivity);

        // Step 8: Calculate duration for display
        int duration = calculateDuration(savedActivity);

        // Step 9: Display success message
        System.out.println("\nActivity copied:");
        System.out.println("  Original ID: " + originalActivity.id());
        System.out.println("  New ID: " + savedActivity.id());
        System.out.println("  Type: " + savedActivity.activityType());
        System.out.println("  Description: " + savedActivity.description());
        System.out.println("  Start Time: " + savedActivity.startTime());
        System.out.println("  End Time: " + savedActivity.endTime());
        System.out.println("  Duration: " + duration + " minutes");
    }

    private String promptForDescription(String currentDescription) {
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

    private ActivityType promptForActivityType(ActivityType currentType) {
        return ActivityTypePrompt.prompt(currentType, "current");
    }

    private int calculateDuration(Activity activity) {
        if (activity.startTime() == null || activity.endTime() == null) {
            return 0;
        }

        Duration duration = Duration.between(activity.startTime(), activity.endTime());
        return (int) duration.toMinutes();
    }

    private LocalDate promptForDate(String label, LocalDate defaultDate) {
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
