package io.cockroachdb.demo.task.support;

import java.sql.SQLException;
import java.util.List;

/**
 * A SQL exception classifier inspecting state codes.
 *
 * @author Kai Niemi
 */
public interface ExceptionClassifier {
    /**
     * Only 40001 is safe to retry in terms of non-idempotent side effects (like INSERT:s)
     */
    List<String> TRANSIENT_CODES = List.of(
            "40001", "08001", "08003", "08004", "08006", "08007", "08S01", "57P01"
    );

    List<String> TRANSIENT_RETRYABLE_CODES = List.of(
            "40001"
    );

    default boolean isTransient(SQLException ex) {
        String sqlState = ex.getSQLState();
        return sqlState != null && TRANSIENT_CODES.contains(sqlState);
    }

    default boolean isTransientRetryable(SQLException ex) {
        String sqlState = ex.getSQLState();
        return sqlState != null && TRANSIENT_RETRYABLE_CODES.contains(sqlState);
    }
}
