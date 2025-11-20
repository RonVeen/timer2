package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import org.veenix.timer.service.ActivityService;
import org.veenix.timer.service.ConfigurationService;
import picocli.CommandLine.Command;

import java.time.Duration;

@Command(
    name = "stop",
    aliases = {"sp"},
    description = "Stop the current timer and mark activity as completed",
    mixinStandardHelpOptions = true
)
public class StopCommand implements Runnable {

    @Override
    public void run() {
        ConfigurationService configurationService = new ConfigurationService();
        ActivityService activityService = new ActivityService(
            new ActivityRepositoryImpl(DatabaseConnection.getInstance()),
            configurationService
        );

        Activity stoppedActivity = activityService.stopActivity();

        if (stoppedActivity == null) {
            System.out.println("No active activity found.");
        } else {
            System.out.println("Activity stopped:");
            System.out.println("  ID: " + stoppedActivity.id());
            System.out.println("  Type: " + stoppedActivity.activityType());
            System.out.println("  Description: " + stoppedActivity.description());
            System.out.println("  Started at: " + stoppedActivity.startTime());
            System.out.println("  Stopped at: " + stoppedActivity.endTime());
            System.out.println("  Duration: " + formatDuration(stoppedActivity));
        }
    }

    private String formatDuration(Activity activity) {
        if (activity.startTime() == null || activity.endTime() == null) {
            return "N/A";
        }

        Duration duration = Duration.between(activity.startTime(), activity.endTime());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }
}
