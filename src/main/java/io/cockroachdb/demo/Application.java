package io.cockroachdb.demo;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.demo.task.Fake;
import io.cockroachdb.demo.task.ReadKey;
import io.cockroachdb.demo.task.ReadKeyFollower;
import io.cockroachdb.demo.task.UpdateKey;
import io.cockroachdb.demo.task.UpdateSet;
import io.cockroachdb.demo.task.core.WorkerManager;
import io.cockroachdb.demo.task.support.DataSourceAware;
import io.cockroachdb.demo.task.support.Name;
import io.cockroachdb.demo.task.support.Task;
import io.cockroachdb.demo.task.support.TransactionAware;
import io.cockroachdb.demo.util.DurationUtils;
import static io.cockroachdb.demo.Main.printUsageAndQuit;

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataJdbcRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
})
public class Application implements CommandLineRunner, ApplicationContextAware {
    static final List<Task> AVAILABLE_TASKS = List.of(
            new Fake(),
            new UpdateKey(),
            new UpdateSet(),
            new ReadKey(),
            new ReadKeyFollower()
    );

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private WorkerManager workerManager;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        Set<String> taskNames = new HashSet<>();
        Map<String, String> params = new HashMap<>();

        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--concurrency")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected value after: " + arg);
                } else {
                    params.put("concurrency", argsList.pop());
                }
            } else if (arg.equals("--duration")) {
                if (argsList.isEmpty()) {
                    printUsageAndQuit("Expected duration after: " + arg);
                } else {
                    params.put("duration", argsList.pop());
                }
            } else if (arg.startsWith("--")) {
                String k = arg.substring(2);
                if (argsList.isEmpty()) {
                    params.put(k, null);
                } else {
                    params.put(k, argsList.pop());
                }
            } else {
                taskNames.add(arg);
            }
        }

        if (taskNames.isEmpty()) {
            printUsageAndQuit("No task name specified");
        }

        List<Task> taskList =
                AVAILABLE_TASKS.stream().filter(t -> {
                    Name name = AnnotationUtils.findAnnotation(t.getClass(), Name.class);
                    Objects.requireNonNull(name, "Missing @Name annotation for " + t.getClass().getSimpleName());
                    return taskNames.contains(name.alias())
                           || taskNames.contains(name.value());
                }).toList();

        if (taskList.isEmpty()) {
            printUsageAndQuit("No such tasks: " + String.join(",", taskNames));
        }

        runAndWait(taskList, params);

        SpringApplication.exit(applicationContext, () -> 0);
    }

    public void runAndWait(List<Task> taskList, Map<String, String> params) {
        final int concurrency = Integer.parseInt(params.getOrDefault("concurrency", "1"));
        final Duration duration = DurationUtils.parseDuration(params.getOrDefault("duration", "15m"));
        final Instant stopTime = Instant.now().plus(duration);

        printPoolStats();

        logger.info("Concurrency level: %s".formatted(concurrency));
        logger.info("Duration: %s".formatted(DurationFormatterUtils.print(duration, DurationFormat.Style.SIMPLE)));

        // Initialize tasks sequentially
        taskList.forEach((task) -> {
            if (task instanceof ApplicationContextAware) {
                ((ApplicationContextAware) task).setApplicationContext(applicationContext);
            }
            if (task instanceof DataSourceAware) {
                ((DataSourceAware) task).setDataSource(dataSource);
            }
            if (task instanceof TransactionAware) {
                ((TransactionAware) task).setTransactionManager(transactionManager);
            }
            task.prepare(params);
        });

        // Queue tasks to run in parallel
        taskList.forEach((task) -> {
            Name name = AnnotationUtils.findAnnotation(task.getClass(), Name.class);
            Objects.requireNonNull(name);

            logger.info("Scheduling task '%s' to run with %d threads for %s"
                    .formatted(name.value(), concurrency, duration));

            IntStream.rangeClosed(1, concurrency).forEach(
                    value -> workerManager.submitTask(task,
                            x -> Instant.now().isBefore(stopTime),
                            name.value() + " #" + value));
        });

        logger.info("All %d tasks scheduled, awaiting completion - ಠ益ಠ".formatted(taskList.size()));

        workerManager.awaitCompletion();

        logger.info("All %d tasks completed, thanks for watching this film - ¯\\_(ツ)_/¯".formatted(taskList.size()));
    }

    private void printPoolStats() {
        HikariDataSource hikariDataSource;
        try {
            hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        logger.info(" Connected to: %s".formatted(new JdbcTemplate(hikariDataSource)
                .queryForObject("select version()", String.class)));

        HikariConfigMXBean cb = hikariDataSource.getHikariConfigMXBean();
        if (cb != null) {
            logger.info(">> Connection Pool Config");
            logger.info(" PoolName: %s".formatted(cb.getPoolName()));
            logger.info(" MaximumPoolSize: %s".formatted(cb.getMaximumPoolSize()));
            logger.info(" MinimumIdle: %s".formatted(cb.getMinimumIdle()));
            logger.info(" ConnectionTimeout: %s".formatted(cb.getConnectionTimeout()));
            logger.info(" IdleTimeout: %s".formatted(cb.getIdleTimeout()));
            logger.info(" LeakDetectionThreshold: %s".formatted(cb.getLeakDetectionThreshold()));
            logger.info(" ValidationTimeout: %s".formatted(cb.getValidationTimeout()));
        }

        logger.info(">> Connection Pool Status");
        logger.info(" PoolName: %s".formatted(hikariDataSource.getPoolName()));
        logger.info(" TransactionIsolation: %s".formatted(hikariDataSource.getTransactionIsolation()));
        logger.info(" KeepaliveTime: %s".formatted(hikariDataSource.getKeepaliveTime()));
        try {
            logger.info(" LoginTimeout: %s".formatted(hikariDataSource.getLoginTimeout()));
        } catch (SQLException e) {
            logger.info(" LoginTimeout (ERROR): %s".formatted(e.toString()));
        }
    }
}
