package io.cockroachdb.demo.task.core;

/**
 * Enumeration of worker status codes.
 *
 * @author Kai Niemi
 */
public enum WorkerStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}
