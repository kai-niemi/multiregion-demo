package io.cockroachdb.demo.task;

import java.sql.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.cockroachdb.demo.task.support.DataSourceAware;
import io.cockroachdb.demo.task.support.Name;
import io.cockroachdb.demo.task.support.Task;
import io.cockroachdb.demo.task.support.TransactionAware;

/**
 * A multi-value update using a narrow selection of keys.
 * Optionally transactional.
 *
 * @author Kai Niemi
 */
@Name(value = "update-set", alias = "us",
        options = {
                "--limit 10000",
                "--size 10",
                "--transactional false",
                "--region <any>"
        })
public class UpdateSet implements Task, DataSourceAware, TransactionAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;

    private final List<UUID> ids = new ArrayList<>();

    private boolean transactional;

    private int size;

    private String region;

    @Override
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
    }

    @Override
    public void prepare(Map<String, String> params) {
        String gateway = jdbcTemplate.queryForObject("select gateway_region()", String.class);

        this.region = params.getOrDefault("region", gateway);

        this.transactional = Boolean.parseBoolean(params.getOrDefault("transactional", "false"));
        this.size = Integer.parseInt(params.getOrDefault("size", "10"));

        int limit = Integer.parseInt(params.getOrDefault("limit", "10000"));

        ids.addAll(jdbcTemplate.queryForList(
                "select id from account where crdb_region=? "
                + "order by random() limit ?",
                UUID.class, region, limit));
    }

    @Override
    public final void run() {
        Set<UUID> keys = IntStream.rangeClosed(1, size)
                .mapToObj(value -> ids.get(ThreadLocalRandom.current().nextInt(ids.size())))
                .collect(Collectors.toSet());

        int rowsAffected;
        if (transactional) {
            rowsAffected = transactionTemplate.execute(status ->
                    jdbcTemplate.update("update account set "
                                        + "closed = ?, "
                                        + "last_modified_time = ? "
                                        + "where id = ANY(?)", ps -> {
                        Array keysArray = ps.getConnection().createArrayOf("varchar", keys.toArray());
                        ps.setBoolean(1, false);
                        ps.setObject(2, LocalDateTime.now());
                        ps.setArray(3, keysArray);
                    }));
        } else {
            rowsAffected = jdbcTemplate.update("update account set "
                                               + "closed = ?, "
                                               + "last_modified_time = ? "
                                               + "where id = ANY(?)", ps -> {
                Array keysArray = ps.getConnection().createArrayOf("varchar", keys.toArray());
                ps.setBoolean(1, false);
                ps.setObject(2, LocalDateTime.now());
                ps.setArray(3, keysArray);
            });
        }

        if (rowsAffected != keys.size()) {
            logger.warn("Rows affected was %d expected %d".formatted(rowsAffected, keys.size()));
        }
    }
}

