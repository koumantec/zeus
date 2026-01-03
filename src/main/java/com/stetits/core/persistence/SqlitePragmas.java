package com.stetits.core.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SqlitePragmas {

    private final JdbcTemplate jdbc;

    @Value("${core.docker.wal:true}")
    boolean wal;

    @Value("${core.docker.busyTimeoutMs:5000}")
    int busyTimeoutMs;

    public SqlitePragmas(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void init() {
        try {
            jdbc.execute("PRAGMA foreign_keys=ON");
            jdbc.execute("PRAGMA busy_timeout=" + busyTimeoutMs);
            if (wal) jdbc.execute("PRAGMA journal_mode=WAL");
        } catch (Exception ignored) {}
    }
}
