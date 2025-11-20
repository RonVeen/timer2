package org.veenix.timer;

import org.veenix.timer.cli.ActivityCommand;
import org.veenix.timer.cli.TimerCommand;
import picocli.CommandLine;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@CommandLine.Command(
    name = "timer-app",
    mixinStandardHelpOptions = true,
    versionProvider = Main.VersionProvider.class,
    subcommands = {
        TimerCommand.class,
        ActivityCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class Main implements Runnable {

    static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() {
            try (InputStream input = Main.class.getClassLoader().getResourceAsStream("version.properties")) {
                if (input == null) {
                    return new String[]{"timer-app unknown"};
                }
                Properties prop = new Properties();
                prop.load(input);
                String version = prop.getProperty("version", "unknown");
                return new String[]{"timer-app " + version};
            } catch (IOException e) {
                return new String[]{"timer-app unknown"};
            }
        }
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
