package io.cockroachdb.demo.task.core;

import java.util.Objects;
import java.util.concurrent.Future;

import io.cockroachdb.demo.task.support.Task;
import io.cockroachdb.demo.util.Metrics;

/**
 * Wrapper for a background future task pending completion.
 *
 * @author Kai Niemi
 */
public class Worker {
    private final Integer id;

    private final String name;

    private final Metrics metrics;

    private final Future<Task> future;

    private boolean failed;

    Worker(Integer id,
           String name,
           Metrics metrics,
           Future<Task> future) {
        this.id = id;
        this.name = name;
        this.metrics = metrics;
        this.future = future;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public WorkerStatus getStatus() {
        if (failed) {
            return WorkerStatus.FAILED;
        } else if (isRunning()) {
            return WorkerStatus.RUNNING;
        } else if (isCancelled()) {
            return WorkerStatus.CANCELLED;
        } else {
            return WorkerStatus.COMPLETED;
        }
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Metrics getMetrics() {
        return isRunning() ? metrics : Metrics.copy(metrics);
    }

    public boolean isRunning() {
        return !future.isDone();
    }

    public boolean isCancelled() {
        return future.isCancelled();
    }

    public Future<Task> getFuture() {
        return future;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Worker worker = (Worker) o;
        return Objects.equals(id, worker.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
