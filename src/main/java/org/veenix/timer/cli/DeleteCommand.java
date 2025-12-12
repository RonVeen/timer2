package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Optional;

@Command(
    name = "delete",
    aliases = {"rm"},
    description = "Delete an activity",
    mixinStandardHelpOptions = true
)
public class DeleteCommand implements Runnable {

    @Parameters(index = "0", description = "Activity ID (must be > 0)")
    private Long activityId;

    @Override
    public void run() {
        if (activityId <= 0) {
            System.out.println("Activity ID must be greater than 0.");
            return;
        }

        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        // Check if activity exists
        Optional<Activity> optionalActivity = activityRepository.findById(activityId);

        if (optionalActivity.isEmpty()) {
            System.out.println("Activity with ID " + activityId + " not found.");
            return;
        }

        // Delete the activity
        activityRepository.delete(activityId);

        System.out.println("Activity " + activityId + " has been deleted.");
    }
}
