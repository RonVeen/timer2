package org.veenix.timer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "activity",
    aliases = {"a", "act"},
    description = "Activity management commands",
    mixinStandardHelpOptions = true,
    version = "1.0",
    subcommands = {
        ActivityListCommand.class,
        EditCommand.class,
        DeleteCommand.class,
        ExportCommand.class,
        AddCommand.class
    }
)
public class ActivityCommand implements Runnable {

    @Override
    public void run() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
    }
}
