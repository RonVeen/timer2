package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Duration;
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

        // Prompt for duration
        int currentDuration = calculateDuration(activity);
        int newDuration = promptForDuration(currentDuration);

        // Prompt for activity type
        ActivityType newActivityType = promptForActivityType(activity.activityType());

        // Update the activity
        Activity updatedActivity = Activity.builder()
                .id(activity.id())
                .startTime(activity.startTime())
                .endTime(activity.startTime().plusMinutes(newDuration))
                .activityType(newActivityType)
                .status(activity.status())
                .description(newDescription)
                .build();

        activityRepository.update(updatedActivity);

        System.out.println("\nActivity updated:");
        System.out.println("  ID: " + updatedActivity.id());
        System.out.println("  Type: " + updatedActivity.activityType());
        System.out.println("  Description: " + updatedActivity.description());
        System.out.println("  Start Time: " + updatedActivity.startTime());
        System.out.println("  End Time: " + updatedActivity.endTime());
        System.out.println("  Duration: " + newDuration + " minutes");
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
}
