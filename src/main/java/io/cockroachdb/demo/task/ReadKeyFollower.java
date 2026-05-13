package io.cockroachdb.demo.task;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import io.cockroachdb.demo.task.support.Name;

/**
 * A basic follower read point lookup.
 *
 * @author Kai Niemi
 */
@Name(value = "read-key-follower", alias = "rkf",
        options = {
                "--limit 10",
                "--region <any>"
        })
public class ReadKeyFollower extends AbstractReadKey {
    @Override
    protected void runQuery(JdbcTemplate jdbcTemplate, UUID id, String region) {
        jdbcTemplate.queryForMap("select * from account "
                                 + "as of system time follower_read_timestamp() "
                                 + "where id=? and crdb_region=?",
                id, region);
    }
}
