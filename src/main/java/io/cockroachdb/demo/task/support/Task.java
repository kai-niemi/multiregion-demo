package io.cockroachdb.demo.task.support;

import java.util.Map;

/**
 * Functional interface for a background worker task. These tasks are executed
 * concurrently for a specified duration of time. Thus, it's important
 * to use proper concurrency controls if sharing any instance state
 * (thread locals, synchronized collections, mutexes, etc) between
 * tasks.
 *
 * @author Kai Niemi
 * @see DataSourceAware
 * @see TransactionAware
 * @see org.springframework.context.ApplicationContextAware
 */
@FunctionalInterface
public interface Task extends Runnable {
    /**
     * Invoked once prior to task execution providing an opportunity to initialize the task,
     * such as creating tables, indexes, populating tables etc.
     *
     * @param params command-line parameters
     */
    default void prepare(Map<String, String> params) {

    }

    /**
     * Invoked once post execution providing an opportunity to teardown any side effects of the task.
     */
    default void teardown() {

    }
}
