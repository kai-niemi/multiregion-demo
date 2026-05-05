package io.cockroachdb.demo.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import io.cockroachdb.demo.task.support.DataSourceAware;
import io.cockroachdb.demo.task.support.Name;
import io.cockroachdb.demo.task.support.Task;

/**
 * A basic singleton update using a narrow selection of keys.
 *
 * @author Kai Niemi
 */
@Name(value = "update-key", alias = "uk",
        options = {
                "--param limit=2",
                "--param region=<any>"
        })
public class UpdateKey implements Task, DataSourceAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private JdbcTemplate jdbcTemplate;

    private final List<UUID> ids = new ArrayList<>();

    @Override
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void prepare(Map<String, String> params) {
        String gateway = jdbcTemplate.queryForObject("select gateway_region()", String.class);

        String region = params.getOrDefault("region", gateway);

        int limit = Integer.parseInt(params.getOrDefault("limit", "2"));

        ids.addAll(jdbcTemplate.queryForList(
                "select id from account where crdb_region=? "
                + "order by random() limit ?",
                UUID.class, region, limit));
    }

    @Override
    public final void run() {
        UUID id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));

        int rows = jdbcTemplate.update("update account set "
                                       + "closed = ?, "
                                       + "last_modified_time = ? "
                                       + "where id=?",
                false,
                LocalDateTime.now(),
                id);
        if (rows != 1) {
            logger.warn("Rows affected was not 1 but " + rows);
        }
    }
}



