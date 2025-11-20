package org.veenix.timer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "timer",
    aliases = {"t", "tmr"},
    description = "Timer application for tracking activities",
    mixinStandardHelpOptions = true,
    version = "1.0",
    subcommands = {
        StartCommand.class,
        StopCommand.class,
        ListCommand.class,
        RestartCommand.class
    }
)
public class TimerCommand implements Runnable {

    @Override
    public void run() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
    }
}
