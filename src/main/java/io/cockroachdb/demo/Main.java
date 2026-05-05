package io.cockroachdb.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import io.cockroachdb.demo.task.support.Name;
import io.cockroachdb.demo.util.AnsiConsole;

public class Main {
    public static void printUsageAndQuit(String message) {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            new AnsiConsole(terminal)
                    .header("Usage: java -jar demo.jar [options] <task, ...>")
                    .header("Options include:")
                    .info("--url <url>                  Connection URL (jdbc:postgresql://localhost:26257/demo)")
                    .info("--user <user>                Login user name (root)")
                    .info("--password <secret>          Login password (n/a)")
                    .info("--pool-size <size>           Max connection pool size (500)")
                    .info("--concurrency <level>        Number of threads per task (1)")
                    .info("--duration <time>            Task execution duration (15m)")
                    .info("--trace                      Enable SQL trace logging (false)")
                    .info("--rc                         Enable READ_COMMITTED isolation (SERIALIZABLE)")
                    .info("--param <key=value>          Task parameter")
                    .info("--profiles [profile,..]      Override spring profiles to activate")
                    .header("Available tasks include (name|alias|params):")
                    .info(Application.AVAILABLE_TASKS
                            .stream().map(task -> {
                                Name name = AnnotationUtils.findAnnotation(task.getClass(), Name.class);
                                Objects.requireNonNull(name);
                                return name.value()
                                       + " | " + name.alias()
                                       + " | " + String.join(" ",
                                        name.options());
                            })
                            .collect(Collectors.joining("\n")))
                    .warn(message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.exit(1);
    }

    public static void main(String[] args) {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThrough = new LinkedList<>(Arrays.asList(args));
        Set<String> profiles = new HashSet<>();

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected profile name(s) after: " + arg);
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else if (arg.equals("--url")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected URL after: " + arg);
                } else {
                    System.setProperty("spring.datasource.url", argsList.pop());
                }
                passThrough.remove(arg);
            } else if (arg.equals("--user")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected username after: " + arg);
                } else {
                    System.setProperty("spring.datasource.user", argsList.pop());
                }
                passThrough.remove(arg);
            } else if (arg.equals("--password")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected password after: " + arg);
                } else {
                    System.setProperty("spring.datasource.password", argsList.pop());
                }
                passThrough.remove(arg);
            } else if (arg.equals("--pool-size")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected value after: " + arg);
                } else {
                    String v = argsList.pop();
                    System.setProperty("spring.datasource.hikari.maximum-pool-size", v);
                    System.setProperty("spring.datasource.hikari.minimum-idle", v);
                }
                passThrough.remove(arg);
            } else if (arg.equals("--rc")) {
                System.setProperty("spring.datasource.hikari.transaction-isolation", "TRANSACTION_READ_COMMITTED");
                passThrough.remove(arg);
            } else if (arg.equals("--trace")) {
                profiles.add("verbose");
                passThrough.remove(arg);
            } else if (arg.equals("--help")) {
                printUsageAndQuit("");
            }
        }

        if (!profiles.isEmpty()) {
            System.setProperty("spring.profiles.active", String.join(",", profiles));
        }

        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(true)
                .bannerMode(Banner.Mode.OFF)
                .profiles(profiles.toArray(new String[0]))
                .run(passThrough.toArray(new String[] {}));
    }
}
