package com.stetits.core.persistence;

import com.stetits.core.domain.dto.CommandLogRow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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

    public List<CommandLogRow> list(long commandId, int limit) {
        return jdbc.query(
                "SELECT ts, level, message FROM command_logs WHERE command_id=? ORDER BY id DESC LIMIT ?",
                (rs, i) -> new CommandLogRow(rs.getString("ts"), rs.getString("level"), rs.getString("message")),
                commandId, limit
        );
    }
}
