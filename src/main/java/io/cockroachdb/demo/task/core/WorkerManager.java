package io.cockroachdb.demo.task.core;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.cockroachdb.demo.task.support.ExceptionClassifier;
import io.cockroachdb.demo.task.support.RecoverableException;
import io.cockroachdb.demo.task.support.Task;
import io.cockroachdb.demo.util.Metrics;

/**
 * A simple workload manager that submit tasks to an executor service
 * and collects time-series call metrics / stats with aggregation. It
 * also provides retries for transient exceptions.
 *
 * @author Kai Niemi
 */
@Component
public class WorkerManager {
    private static void backoffDelayWithJitter(int calls) {
        try {
            TimeUnit.MILLISECONDS.sleep(
                    Math.min((long) (Math.pow(2, calls) + Math.random() * 1000), 5000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicInteger monotonicId = new AtomicInteger();

    private final List<Worker> workers = new LinkedList<>();

    private ExceptionClassifier exceptionClassifier = new ExceptionClassifier() {
    };

    private final int samplePeriodSeconds = 60;

    @Autowired
    @Qualifier("asyncTaskExecutor")
    private AsyncTaskExecutor taskExecutor;

    public void setExceptionClassifier(ExceptionClassifier exceptionClassifier) {
        this.exceptionClassifier = exceptionClassifier;
    }

    /**
     * Submit a new task for execution.
     *
     * @param task       the task to submit
     * @param completion the task completion predicate receiving total number of task invocations
     * @param name       an identifying name for the task
     */
    public void submitTask(Task task, Predicate<Integer> completion, String name) {
        final Metrics metrics = Metrics.empty(samplePeriodSeconds);

        final Future<Task> future = taskExecutor.submit(() -> {
            AtomicInteger totalCalls = new AtomicInteger();
            AtomicInteger failedCalls = new AtomicInteger();

            while (completion.test(totalCalls.incrementAndGet())) {
                if (Thread.interrupted()) {
                    logger.warn("Thread interrupted - bailing out");
                    break;
                }

                final Instant invocationTime = Instant.now();

                try {
                    task.run();
                    metrics.markSuccess(Duration.between(invocationTime, Instant.now()));
                    failedCalls.set(0);
                } catch (Throwable ex) {
                    final Duration callTime = Duration.between(invocationTime, Instant.now());
                    Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
                    boolean isTransient = true;

                    if (cause instanceof SQLException) {
                        String sqlState = ((SQLException) cause).getSQLState();
                        if (exceptionClassifier.isTransient((SQLException) cause)) {
                            logger.warn("Transient SQL exception in %s: [%s]: [%s]"
                                    .formatted(name, sqlState, cause));
                        } else {
                            isTransient = false;
                            logger.error("Non-transient SQL exception in %s: [%s]"
                                    .formatted(name, sqlState), cause);
                        }
                    } else if (ex instanceof TransientDataAccessException) {
                        logger.warn("Recoverable SQL exception in %s: [%s]".formatted(name, ex));
                    } else if (ex instanceof RecoverableException) {
                        logger.warn("Recoverable exception in %s: [%s]".formatted(name, ex));
                    } else if (ex instanceof NonTransientDataAccessException) {
                        throw new FailedExecutionException(name, "Non-transient SQL exception", ex);
                    } else {
                        throw new FailedExecutionException(name, "Uncategorized exception", ex);
                    }

                    metrics.markFail(callTime, isTransient);

                    backoffDelayWithJitter(failedCalls.incrementAndGet());
                }
            }
            return task;
        });

        workers.add(new Worker(monotonicId.incrementAndGet(), name, metrics, future));
    }

    /**
     * Awaits completion of all submitted tasks.
     */
    public void awaitCompletion() {
        for (Worker worker : workers) {
            try {
                Task t = worker.getFuture().get();
                t.teardown();

                worker.setFailed(false);
                logger.info("Finished '%s' successfully".formatted(worker.getName()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                worker.setFailed(true);
                logger.warn("Finished '%s' prematurely due to interrupt".formatted(worker.getName()), e);
            } catch (ExecutionException e) {
                worker.setFailed(true);
                logger.warn("Finished '%s' prematurely".formatted(worker.getName()), e.getCause());
            }
        }
    }

    /**
     * Returns a list of all submitted task workers with a given status.
     *
     * @param status the set of statuses to inspect
     * @return an immutable list of workers
     */
    public List<Worker> listWorkers(EnumSet<WorkerStatus> status) {
        return new LinkedList<>(workers)
                .stream()
                .filter(worker -> status.contains(worker.getStatus()))
                .toList();
    }

    /**
     * Periodically prints the call metrics for all tasks to stdout.
     */
    @Scheduled(fixedRate = 5, initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void printMetrics() {
        AtomicInteger i = new AtomicInteger();

        EnumSet<WorkerStatus> status = EnumSet.allOf(WorkerStatus.class);

        listWorkers(status).forEach(worker -> {
            if (i.getAndIncrement() % 10 == 0) {
                System.out.printf("%4s %-25s %9s %9s %7s %7s | %5s %5s %5s %5s | %7s %7s %7s %s%n",
                        "id", "name", "op/s", "op/m", "time", "mean",
                        "p50", "p95", "p99", "p999",
                        "success", "retry", "error", "status"
                );
                System.out.println(new String(new char[125]).replace('\0', '-'));
            }

            Metrics m = worker.getMetrics();

            System.out.printf(
                    "%4d %-25s %9.1f %9.1f %7.1f %7.1f | %5.0f %5.0f %5.0f %5.0f | %7d %7d %7d %s%n",
                    worker.getId(),
                    worker.getName(),
                    m.getOpsPerSec(),
                    m.getOpsPerMin(),
                    m.getExecutionTimeSeconds(),
                    m.getMeanTimeMillis(),
                    m.getP50(),
                    m.getP95(),
                    m.getP99(),
                    m.getP999(),
                    m.getSuccess(),
                    m.getTransientFail(),
                    m.getNonTransientFail(),
                    worker.getStatus()
            );
        });

        Metrics m = metricsAggregate(status);

        System.out.printf("%4s %-25s %9.1f %9.1f %7.1f %7.1f | %5.0f %5.0f %5.0f %5.0f | %7d %7d %7d %n",
                "Σ",
                "",
                m.getOpsPerSec(),
                m.getOpsPerMin(),
                m.getExecutionTimeSeconds(),
                m.getMeanTimeMillis(),
                m.getP50(),
                m.getP95(),
                m.getP99(),
                m.getP999(),
                m.getSuccess(),
                m.getTransientFail(),
                m.getNonTransientFail());
    }

    private Metrics metricsAggregate(EnumSet<WorkerStatus> status) {
        List<Metrics> metrics = listWorkers(status).stream().map(Worker::getMetrics).toList();
        return Metrics.builder(samplePeriodSeconds)
                .withUpdateTime(Instant.now())
                .withMeanTimeMillis(metrics.stream()
                        .mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withOps(metrics.stream().mapToDouble(Metrics::getOpsPerSec).sum(),
                        metrics.stream().mapToDouble(Metrics::getOpsPerMin).sum())
                .withP50(metrics.stream().mapToDouble(Metrics::getP50).average().orElse(0))
                .withP90(metrics.stream().mapToDouble(Metrics::getP90).average().orElse(0))
                .withP95(metrics.stream().mapToDouble(Metrics::getP95).average().orElse(0))
                .withP99(metrics.stream().mapToDouble(Metrics::getP99).average().orElse(0))
                .withP999(metrics.stream().mapToDouble(Metrics::getP999).average().orElse(0))
                .withMeanTimeMillis(metrics.stream().mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withSuccessful(metrics.stream().mapToInt(Metrics::getSuccess).sum())
                .withFails(metrics.stream().mapToInt(Metrics::getTransientFail).sum(),
                        metrics.stream().mapToInt(Metrics::getNonTransientFail).sum())
                .build();
    }
}
