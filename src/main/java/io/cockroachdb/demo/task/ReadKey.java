package io.cockroachdb.demo.task;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import io.cockroachdb.demo.task.support.Name;

/**
 * A basic authoritative read (point lookup) from the raft group lease holder.
 *
 * @author Kai Niemi
 */
@Name(value = "read-key", alias = "rk",
        options = {
                "--limit 10",
                "--region <any>"
        })
public class ReadKey extends AbstractReadKey {
    protected void runQuery(JdbcTemplate jdbcTemplate, UUID id, String region) {
        jdbcTemplate.queryForMap("select * from account where id=? and crdb_region=?", id, region);
    }
}

