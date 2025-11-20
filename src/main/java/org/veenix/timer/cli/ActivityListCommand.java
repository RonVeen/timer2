package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Command(
    name = "list",
    aliases = {"ls"},
    description = "List activities",
    mixinStandardHelpOptions = true
)
public class ActivityListCommand implements Runnable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    DateOptions dateOptions;

    static class DateOptions {
        @Option(names = {"--date"}, description = "Show activities for specific date (format: yyyyMMdd)")
        String date;

        @Option(names = {"--from"}, description = "Show activities from date to today (format: yyyyMMdd)")
        String from;

        @Option(names = {"--all"}, description = "Show all activities in chronological order")
        boolean all;
    }

    @Override
    public void run() {
        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );

        // Get activities
        List<Activity> activities;

        if (dateOptions != null && dateOptions.all) {
            // Show all activities in chronological order
            activities = activityRepository.findAll();
        } else {
            LocalDate targetDate;
            LocalDate endDate;

            try {
                if (dateOptions == null || (dateOptions.date == null && dateOptions.from == null)) {
                    // Default: show today's activities
                    targetDate = LocalDate.now();
                    endDate = targetDate;
                } else if (dateOptions.date != null) {
                    // Show activities for specific date
                    targetDate = LocalDate.parse(dateOptions.date, DATE_FORMATTER);
                    endDate = targetDate;
                } else {
                    // Show activities from date to today
                    targetDate = LocalDate.parse(dateOptions.from, DATE_FORMATTER);
                    endDate = LocalDate.now();
                }
            } catch (DateTimeParseException e) {
                System.err.println("Invalid date format. Please use yyyyMMdd format.");
                System.exit(1);
                return;
            }

            if (targetDate.equals(endDate)) {
                LocalDateTime startOfDay = targetDate.atStartOfDay();
                activities = activityRepository.findByStartTime(startOfDay);
            } else {
                LocalDateTime fromDateTime = targetDate.atStartOfDay();
                LocalDateTime toDateTime = endDate.atTime(LocalTime.MAX);
                activities = activityRepository.findByDateRange(fromDateTime, toDateTime);
            }
        }

        if (activities.isEmpty()) {
            System.out.println("No activities found for the specified date range.");
        } else {
            // Print header
            System.out.printf("%-5s | %-10s | %-5s | %-5s | %-8s | %-10s | %s%n",
                "ID", "Date", "Start", "End", "Duration", "Status", "Description");
            System.out.println("------+------------+-------+-------+----------+------------+------------------");

            // Print activities and calculate total duration
            long totalMinutes = 0;
            for (Activity activity : activities) {
                String id = String.valueOf(activity.id());
                String date = activity.startTime() != null
                    ? activity.startTime().format(DATE_DISPLAY_FORMATTER)
                    : "-";
                String startTime = activity.startTime() != null
                    ? activity.startTime().format(TIME_FORMATTER)
                    : "-";
                String endTime = activity.endTime() != null
                    ? activity.endTime().format(TIME_FORMATTER)
                    : "-";
                String duration = formatDuration(activity);
                String status = activity.status() != null ? activity.status().toString() : "-";
                String description = activity.description() != null ? activity.description() : "";

                System.out.printf("%-5s | %-10s | %-5s | %-5s | %-8s | %-10s | %s%n",
                    id, date, startTime, endTime, duration, status, description);

                // Add to total if both start and end times exist
                if (activity.startTime() != null && activity.endTime() != null) {
                    Duration activityDuration = Duration.between(activity.startTime(), activity.endTime());
                    totalMinutes += activityDuration.toMinutes();
                }
            }

            // Print total line
            System.out.println("------+------------+-------+-------+----------+------------+------------------");
            long hours = totalMinutes / 60;
            long remainingMinutes = totalMinutes % 60;
            System.out.printf("%-5s | %-10s | %-5s | %-5s | %-8s%n",
                "", "", "", "TOTAL:", totalMinutes + " min (" + hours + "h " + remainingMinutes + "m)");

            System.out.println("\nTotal activities: " + activities.size());
        }
    }

    private String formatDuration(Activity activity) {
        if (activity.startTime() == null || activity.endTime() == null) {
            return "-";
        }

        Duration duration = Duration.between(activity.startTime(), activity.endTime());
        long minutes = duration.toMinutes();

        return minutes + " min";
    }
}
