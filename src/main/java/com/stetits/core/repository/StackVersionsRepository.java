package com.stetits.core.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StackVersionsRepository {
    private final JdbcTemplate jdbc;

    public StackVersionsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> listVersions(String stackId) {
        return jdbc.query(
                "SELECT version FROM stack_versions WHERE stack_id=? ORDER BY id ASC",
                (rs, i) -> rs.getString("version"),
                stackId
        );
    }

    public Optional<String> getBodyJson(String stackId, String version) {
        var rows = jdbc.query(
                "SELECT body_json FROM stack_versions WHERE stack_id=? AND version=?",
                (rs, i) -> rs.getString("body_json"),
                stackId, version
        );
        return rows.stream().findFirst();
    }
}
