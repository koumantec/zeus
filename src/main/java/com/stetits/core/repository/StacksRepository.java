package com.stetits.core.repository;

import com.stetits.core.domain.dto.StackDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class StacksRepository {
    private final JdbcTemplate jdbc;

    public StacksRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StackDto> list() {
        return jdbc.query(
                "SELECT stack_id, name, current_version FROM stacks ORDER BY stack_id ASC",
                (rs, i) -> new StackDto(rs.getString("stack_id"), rs.getString("name"), rs.getString("current_version"))
        );
    }

    public Optional<StackDto> get(String stackId) {
        var rows = jdbc.query(
                "SELECT stack_id, name, current_version FROM stacks WHERE stack_id=?",
                (rs, i) -> new StackDto(rs.getString("stack_id"), rs.getString("name"), rs.getString("current_version")),
                stackId
        );
        return rows.stream().findFirst();
    }

    public void insert(String stackId, String name) {
        jdbc.update(
                "INSERT INTO stacks(stack_id, name, current_version, created_at) VALUES(?,?,?,?)",
                stackId, name, null, Instant.now().toString()
        );
    }

    public void setCurrentVersion(String stackId, String version) {
        jdbc.update("UPDATE stacks SET current_version=? WHERE stack_id=?", version, stackId);
    }
}
