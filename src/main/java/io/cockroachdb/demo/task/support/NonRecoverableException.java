package io.cockroachdb.demo.task.support;

/**
 * An exception thrown by a task to signal an unrecoverable, non-transient exception.
 *
 * @author Kai Niemi
 */
public class NonRecoverableException extends RuntimeException {
    public NonRecoverableException(String message) {
        super(message);
    }
}

