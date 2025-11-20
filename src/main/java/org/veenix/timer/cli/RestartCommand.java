package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import org.veenix.timer.service.ActivityService;
import org.veenix.timer.service.ConfigurationService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;

@Command(
    name = "restart",
    description = "Restart an activity by copying an existing one",
    mixinStandardHelpOptions = true
)
public class RestartCommand implements Runnable {

    @Parameters(index = "0", description = "Activity ID to restart (must be > 0)")
    private Long activityId;

    @Override
    public void run() {
        if (activityId <= 0) {
            System.out.println("Activity ID must be greater than 0.");
            return;
        }

        ConfigurationService configurationService = new ConfigurationService();
        ActivityRepositoryImpl activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );
        ActivityService activityService = new ActivityService(
            activityRepository,
            configurationService
        );

        // Check if source activity exists
        var optionalSource = activityRepository.findById(activityId);
        if (optionalSource.isEmpty()) {
            System.out.println("Activity with ID " + activityId + " not found.");
            return;
        }

        Activity sourceActivity = optionalSource.get();

        // Check for active activities
        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);

        if (!activeActivities.isEmpty()) {
            Activity activeActivity = activeActivities.get(0);

            // Prompt user to stop active activity
            System.out.println("Activity " + activeActivity.id() + " - " +
                activeActivity.activityType() + " - " +
                activeActivity.description() + " is currently active. Stop it? (Y/N): ");

            Scanner scanner = new Scanner(System.in);
            String response = "";

            while (true) {
                response = scanner.nextLine().trim().toUpperCase();
                if (response.equals("Y") || response.equals("N")) {
                    break;
                }
                System.out.print("Please enter Y or N: ");
            }

            if (response.equals("N")) {
                System.out.println("Restart cancelled");
                return;
            }

            // Stop the active activity
            Activity stoppedActivity = activityService.stopActivity();
            if (stoppedActivity != null) {
                long minutes = Duration.between(
                    stoppedActivity.startTime(),
                    stoppedActivity.endTime()
                ).toMinutes();

                System.out.println(stoppedActivity.id() + " - " +
                    stoppedActivity.description() +
                    " has been completed after being working on for " +
                    minutes + " minutes");
            }
        }

        // Create new activity from source
        Activity newActivity = activityService.restartActivity(activityId);

        if (newActivity != null) {
            System.out.println("New activity " + newActivity.id() + " - " +
                newActivity.description() + " has started");
        }
    }
}
