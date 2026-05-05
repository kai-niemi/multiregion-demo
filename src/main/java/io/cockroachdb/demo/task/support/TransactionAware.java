package io.cockroachdb.demo.task.support;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * Interface to be implemented by any task that wishes to be supplied
 * with a {@link PlatformTransactionManager} to use.
 *
 * @author Kai Niemi
 */
public interface TransactionAware {
    void setTransactionManager(PlatformTransactionManager transactionManager);
}
