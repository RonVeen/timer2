package org.veenix.timer.cli.util;

import org.veenix.timer.model.ActivityType;

import java.util.Scanner;

public class ActivityTypePrompt {

    public static ActivityType prompt(ActivityType defaultOrCurrent, String contextLabel) {
        Scanner scanner = new Scanner(System.in);
        ActivityType[] types = ActivityType.values();

        // Display numbered list with first letter highlighted
        System.out.println("\nSelect activity type:");
        for (int i = 0; i < types.length; i++) {
            String marker = types[i] == defaultOrCurrent ? " (" + contextLabel + ")" : "";
            System.out.println((i + 1) + ". " + formatFirstLetter(types[i].name()) + marker);
        }

        while (true) {
            System.out.print("Enter choice (1-" + types.length + ", first letter, or name) [" + defaultOrCurrent + "]: ");
            String input = scanner.nextLine().trim();

            ActivityType result = parseInput(input, types, defaultOrCurrent);
            if (result != null) {
                return result;
            }

            System.out.println("Invalid input. Please try again.");
        }
    }

    // Package-private for testing
    static String formatFirstLetter(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        return AnsiColors.BOLD_RED + typeName.charAt(0) + AnsiColors.RESET + typeName.substring(1);
    }

    // Package-private for testing
    static ActivityType parseInput(String input, ActivityType[] types, ActivityType defaultValue) {
        // Empty input - use default/current
        if (input.isEmpty()) {
            return defaultValue;
        }

        // Try to parse as number
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= types.length) {
                return types[choice - 1];
            }
        } catch (NumberFormatException e) {
            // Not a number, continue to next attempt
        }

        // Single letter matching (case-insensitive)
        if (input.length() == 1) {
            char inputChar = input.toUpperCase().charAt(0);
            for (ActivityType type : types) {
                if (type.name().charAt(0) == inputChar) {
                    return type;
                }
            }
        }

        // Full name matching (case-insensitive)
        try {
            return ActivityType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid input
        }

        // No match found
        return null;
    }
}
