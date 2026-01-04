package com.stetits.core.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcStackVersionsRepository implements StackVersionsRepository {

    private final JdbcTemplate jdbc;

    public JdbcStackVersionsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> getBodyJson(String stackId, String version) {
        var rows = jdbc.query(
                "SELECT body_json FROM stack_versions WHERE stack_id=? AND version=?",
                (rs, i) -> rs.getString("body_json"),
                stackId, version
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<String> listVersions(String stackId) {
        return jdbc.query(
                "SELECT version FROM stack_versions WHERE stack_id=? ORDER BY created_at DESC",
                (rs, i) -> rs.getString("version"),
                stackId
        );
    }

    @Override
    public void insert(StackVersionRow row) {
        jdbc.update("""
      INSERT INTO stack_versions(stack_id, version, parent_version, body_json, body_sha256, created_at, created_by, comment)
      VALUES(?,?,?,?,?,?,?,?)
      """,
                row.stackId(), row.version(), row.parentVersion(), row.bodyJson(), row.bodySha256(), Instant.now().toString(), row.createdBy(), row.comment()
        );
    }

    public Optional<String> findLatestVersion(String stackId) {
        var rows = jdbc.query(
                "SELECT version FROM stack_versions WHERE stack_id=? ORDER BY created_at DESC LIMIT 1",
                (rs, i) -> rs.getString("version"),
                stackId
        );
        return rows.stream().findFirst();
    }

    public Optional<String> findLatestBodySha(String stackId) {
        var rows = jdbc.query(
                "SELECT body_sha256 FROM stack_versions WHERE stack_id=? ORDER BY created_at DESC LIMIT 1",
                (rs, i) -> rs.getString("body_sha256"),
                stackId
        );
        return rows.stream().findFirst();
    }

    public Optional<String> findLatestParentVersion(String stackId) {
        var rows = jdbc.query(
                "SELECT parent_version FROM stack_versions WHERE stack_id=? ORDER BY created_at DESC LIMIT 1",
                (rs, i) -> rs.getString("parent_version"),
                stackId
        );
        return rows.stream().findFirst();
    }
}
