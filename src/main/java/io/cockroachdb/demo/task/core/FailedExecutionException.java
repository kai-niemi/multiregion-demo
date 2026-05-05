package io.cockroachdb.demo.task.core;

/**
 * @author Kai Niemi
 */
public class FailedExecutionException extends RuntimeException {
    public FailedExecutionException(String name, String message, Throwable cause) {
        super("Task '" + name + "' failed due to: " + message, cause);
    }
}
