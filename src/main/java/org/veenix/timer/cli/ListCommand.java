package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.Command;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Command(
    name = "list",
    aliases = {"ls"},
    description = "List all active activities",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Runnable {

    @Override
    public void run() {
        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        List<Activity> activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);

        if (activeActivities.isEmpty()) {
            System.out.println("No active activities found.");
        } else {
            System.out.println("Active Activities:");
            System.out.println("==================");

            for (Activity activity : activeActivities) {
                System.out.println();
                System.out.println("ID: " + activity.id());
                System.out.println("Type: " + activity.activityType());
                System.out.println("Description: " + activity.description());
                System.out.println("Started at: " + activity.startTime());
                System.out.println("Running for: " + formatRunningDuration(activity));
                System.out.println("------------------");
            }

            System.out.println("\nTotal active activities: " + activeActivities.size());
        }
    }

    private String formatRunningDuration(Activity activity) {
        if (activity.startTime() == null) {
            return "N/A";
        }

        Duration duration = Duration.between(activity.startTime(), LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }
}
