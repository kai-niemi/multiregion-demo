package io.cockroachdb.demo.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import io.cockroachdb.demo.task.support.DataSourceAware;
import io.cockroachdb.demo.task.support.Task;

public abstract class AbstractReadKey implements Task, DataSourceAware {
    private JdbcTemplate jdbcTemplate;

    private final List<UUID> ids = new ArrayList<>();

    private String region;

    @Override
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void prepare(Map<String, String> params) {
        String gateway = jdbcTemplate.queryForObject("select gateway_region()", String.class);

        this.region = params.getOrDefault("region", gateway);

        int limit = Integer.parseInt(params.getOrDefault("limit", "10"));

        ids.addAll(jdbcTemplate.queryForList(
                "select id from account where crdb_region=? "
                + "order by random() limit ?",
                UUID.class, region, limit));
    }

    @Override
    public final void run() {
        UUID id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));

        runQuery(jdbcTemplate, id, region);
    }

    protected abstract void runQuery(JdbcTemplate jdbcTemplate, UUID id, String region);
}


