package com.stetits.core.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class CommandLogsRepository {

    private final JdbcTemplate jdbc;

    public CommandLogsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(long commandId, String level, String message) {
        jdbc.update(
                "INSERT INTO command_logs(command_id, ts, level, message) VALUES(?,?,?,?)",
                commandId,
                Instant.now().toString(),
                level,
                message
        );
    }
}
