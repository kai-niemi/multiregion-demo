package io.cockroachdb.demo.task.support;

/**
 * An exception thrown by a task to signal a recoverable, transient exception candidate
 * for a retry.
 *
 * @author Kai Niemi
 */
public class RecoverableException extends RuntimeException {
    public RecoverableException(String message) {
        super(message);
    }
}
