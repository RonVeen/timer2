package org.veenix.timer;

import org.veenix.timer.cli.ActivityCommand;
import org.veenix.timer.cli.TimerCommand;
import picocli.CommandLine;

@CommandLine.Command(
    name = "timer-app",
    mixinStandardHelpOptions = true,
    subcommands = {
        TimerCommand.class,
        ActivityCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class Main implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
