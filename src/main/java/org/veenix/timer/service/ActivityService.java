package org.veenix.timer.service;

import org.veenix.timer.model.Activity;
import org.veenix.timer.model.ActivityStatus;
import org.veenix.timer.model.ActivityType;
import org.veenix.timer.persistence.ActivityRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ConfigurationService configurationService;

    public ActivityService(ActivityRepository activityRepository, ConfigurationService configurationService) {
        this.activityRepository = activityRepository;
        this.configurationService = configurationService;
    }

    public Activity startActivity(ActivityType type, String description) {
        return startActivity(type, description, LocalDateTime.now());
    }

    public Activity startActivity(ActivityType type, String description, LocalDateTime startTime) {
        // Complete all currently active activities
        completeActiveActivities();

        // Create new activity with ACTIVE status
        Activity newActivity = Activity.builder()
                .startTime(startTime)
                .activityType(type)
                .status(ActivityStatus.ACTIVE)
                .description(description)
                .build();

        return activityRepository.save(newActivity);
    }

    public Activity stopActivity() {
        // Find the currently active activity
        var activeActivities = activityRepository.findByStatus(ActivityStatus.ACTIVE);

        if (activeActivities.isEmpty()) {
            return null;
        }

        // Get the first active activity (should only be one)
        Activity activeActivity = activeActivities.get(0);

        // Calculate end time with rounding
        LocalDateTime endTime = LocalDateTime.now();
        int roundingInterval = configurationService.getRoundingMinutes();
        if (roundingInterval > 1) {
            endTime = roundUpToInterval(endTime, roundingInterval);
        }

        // Update it to completed with end time
        Activity completedActivity = Activity.builder()
                .id(activeActivity.id())
                .startTime(activeActivity.startTime())
                .endTime(endTime)
                .activityType(activeActivity.activityType())
                .status(ActivityStatus.COMPLETED)
                .description(activeActivity.description())
                .build();

        return activityRepository.update(completedActivity);
    }

    public Activity restartActivity(Long sourceActivityId) {
        // Find source activity
        var optionalSource = activityRepository.findById(sourceActivityId);
        if (optionalSource.isEmpty()) {
            return null;
        }

        Activity sourceActivity = optionalSource.get();

        // Stop any active activities
        Activity stoppedActivity = stopActivity();

        // Create new activity from source template
        Activity newActivity = Activity.builder()
                .startTime(LocalDateTime.now())
                .activityType(sourceActivity.activityType())
                .status(ActivityStatus.ACTIVE)
                .description(sourceActivity.description())
                .build();

        return activityRepository.save(newActivity);
    }

    private void completeActiveActivities() {
        LocalDateTime now = LocalDateTime.now();
        activityRepository.updateStatusByStatus(ActivityStatus.ACTIVE, ActivityStatus.COMPLETED, now);
    }

    private LocalDateTime roundUpToInterval(LocalDateTime time, int intervalMinutes) {
        // Truncate to minute (remove seconds and nanoseconds)
        LocalDateTime truncated = time.truncatedTo(ChronoUnit.MINUTES);

        int currentMinute = truncated.getMinute();
        int remainder = currentMinute % intervalMinutes;

        // If already at interval boundary, return as-is
        if (remainder == 0) {
            return truncated;
        }

        // Round up to next interval
        int minutesToAdd = intervalMinutes - remainder;
        return truncated.plusMinutes(minutesToAdd);
    }
}
