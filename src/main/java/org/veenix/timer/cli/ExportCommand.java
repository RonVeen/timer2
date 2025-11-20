package org.veenix.timer.cli;

import org.veenix.timer.model.Activity;
import org.veenix.timer.persistence.ActivityRepository;
import org.veenix.timer.persistence.ActivityRepositoryImpl;
import org.veenix.timer.persistence.DatabaseConnection;
import org.veenix.timer.service.ConfigurationService;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Command(
    name = "export",
    description = "Export activities to CSV",
    mixinStandardHelpOptions = true
)
public class ExportCommand implements Runnable {

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    DateOptions dateOptions;

    static class DateOptions {
        @Option(names = {"--date"}, description = "Date in format yyyyMMdd")
        String date;

        @ArgGroup(exclusive = false)
        FromToOptions fromTo;
    }

    static class FromToOptions {
        @Option(names = {"--from"}, required = true, description = "From date in format yyyyMMdd")
        String from;

        @Option(names = {"--to"}, description = "To date in format yyyyMMdd (requires --from)")
        String to;
    }

    @Override
    public void run() {
        ActivityRepository activityRepository = new ActivityRepositoryImpl(
            DatabaseConnection.getInstance()
        );
        ConfigurationService configService = new ConfigurationService();

        // Determine date range
        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;

        if (dateOptions != null && dateOptions.date != null) {
            // Single date
            LocalDate date = parseDate(dateOptions.date);
            if (date == null) {
                System.out.println("Invalid date format. Use yyyyMMdd (e.g., 20251027)");
                return;
            }
            fromDateTime = date.atStartOfDay();
            toDateTime = date.atTime(LocalTime.MAX);
        } else if (dateOptions != null && dateOptions.fromTo != null) {
            // Date range with --from and optional --to
            LocalDate fromDate = parseDate(dateOptions.fromTo.from);
            if (fromDate == null) {
                System.out.println("Invalid date format for --from. Use yyyyMMdd (e.g., 20251027)");
                return;
            }
            fromDateTime = fromDate.atStartOfDay();

            if (dateOptions.fromTo.to != null) {
                // --to is specified
                LocalDate toDate = parseDate(dateOptions.fromTo.to);
                if (toDate == null) {
                    System.out.println("Invalid date format for --to. Use yyyyMMdd (e.g., 20251027)");
                    return;
                }

                // Validate that --to is not before --from
                if (toDate.isBefore(fromDate)) {
                    System.err.println("Error: --to date (" + dateOptions.fromTo.to + ") cannot be before --from date (" + dateOptions.fromTo.from + ")");
                    return;
                }

                toDateTime = toDate.atTime(LocalTime.MAX);
            } else {
                // --to not specified, use current date
                toDateTime = LocalDate.now().atTime(LocalTime.MAX);
            }
        } else {
            // Default: today
            LocalDate today = LocalDate.now();
            fromDateTime = today.atStartOfDay();
            toDateTime = today.atTime(LocalTime.MAX);
        }

        // Get activities
        List<Activity> activities;
        if (dateOptions != null && dateOptions.date != null) {
            activities = activityRepository.findByStartTime(fromDateTime);
        } else {
            activities = activityRepository.findByDateRange(fromDateTime, toDateTime);
        }

        if (activities.isEmpty()) {
            System.out.println("No activities found for the specified date range.");
            return;
        }

        // Generate filename
        String filename = generateFilename();

        // Export to CSV
        String delimiter = configService.getCsvDelimiter();
        try {
            exportToCsv(activities, filename, delimiter);
            System.out.println("Data has been exported to file " + filename);
        } catch (IOException e) {
            System.out.println("Error exporting data: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String generateFilename() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        String randomPart = UUID.randomUUID().toString().substring(0, 8);
        return "activities_" + timestamp + "_" + randomPart + ".csv";
    }

    private void exportToCsv(List<Activity> activities, String filename, String delimiter) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("id")
                  .append(delimiter)
                  .append("start_time")
                  .append(delimiter)
                  .append("end_time")
                  .append(delimiter)
                  .append("activity_type")
                  .append(delimiter)
                  .append("status")
                  .append(delimiter)
                  .append("description")
                  .append("\n");

            // Write data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Activity activity : activities) {
                writer.append(String.valueOf(activity.id()))
                      .append(delimiter)
                      .append(activity.startTime().format(formatter))
                      .append(delimiter)
                      .append(activity.endTime() != null ? activity.endTime().format(formatter) : "")
                      .append(delimiter)
                      .append(activity.activityType().name())
                      .append(delimiter)
                      .append(activity.status().name())
                      .append(delimiter)
                      .append(escapeCsvField(activity.description(), delimiter))
                      .append("\n");
            }
        }
    }

    private String escapeCsvField(String field, String delimiter) {
        if (field == null) {
            return "";
        }

        // If field contains delimiter, quotes, or newlines, wrap in quotes and escape quotes
        if (field.contains(delimiter) || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }
}
