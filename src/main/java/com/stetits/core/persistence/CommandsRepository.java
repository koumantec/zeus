package com.stetits.core.persistence;

import com.stetits.core.domain.dto.CommandDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class CommandsRepository {

    private final JdbcTemplate jdbc;

    public CommandsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CommandDto> list(Optional<String> stackId, int limit) {
        if (stackId.isPresent()) {
            return jdbc.query(
                    "SELECT * FROM commands WHERE stack_id=? ORDER BY id DESC LIMIT ?",
                    (rs, i) -> map(rs),
                    stackId.get(), limit
            );
        }
        return jdbc.query(
                "SELECT * FROM commands ORDER BY id DESC LIMIT ?",
                (rs, i) -> map(rs),
                limit
        );
    }

    public Optional<CommandDto> get(long id) {
        var rows = jdbc.query("SELECT * FROM commands WHERE id=?", (rs, i) -> map(rs), id);
        return rows.stream().findFirst();
    }

    public long enqueue(String stackId, String type, String payloadJson) {
        jdbc.update(
                "INSERT INTO commands(stack_id, type, payload_json, status, created_at) VALUES(?,?,?,?,?)",
                stackId, type, payloadJson, "PENDING", Instant.now().toString()
        );
        // SQLite: récupérer last_insert_rowid() sur la même connexion n'est pas trivial via JdbcTemplate,
        // donc on fait une requête simple et sûre :
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id == null ? -1 : id;
    }

    public boolean cancelIfPending(long id) {
        int updated = jdbc.update(
                "UPDATE commands SET status=?, ended_at=? WHERE id=? AND status='PENDING'",
                "CANCELLED", Instant.now().toString(), id
        );
        return updated == 1;
    }

    public void markDone(long id) {
        jdbc.update(
                "UPDATE commands SET status=?, ended_at=? WHERE id=? AND status='RUNNING'",
                "DONE", Instant.now().toString(), id
        );
    }

    public void markFailed(long id, String errorMessage) {
        jdbc.update(
                "UPDATE commands SET status=?, ended_at=?, error_message=? WHERE id=? AND status='RUNNING'",
                "FAILED", Instant.now().toString(), truncate(errorMessage, 2000), id
        );
    }

    /**
     * Claim atomique de la prochaine commande PENDING.
     * IMPORTANT: @Transactional assure que tout se passe sur la même connexion.
     */
    @Transactional
    public Optional<Long> claimNextPending() {
        SqlRowSet next = jdbc.queryForRowSet("SELECT id FROM commands WHERE status='PENDING' ORDER BY id ASC LIMIT 1");
        if (!next.next()) {
            return Optional.empty();
        }
        long cmdId = next.getLong("id");

        SqlRowSet prev = jdbc.queryForRowSet(
                "SELECT 1 FROM commands WHERE id < ? AND status IN ('PENDING','RUNNING') LIMIT 1",
                cmdId
        );
        if (prev.next()) {
            return Optional.empty();
        }

        int updated = jdbc.update(
                "UPDATE commands SET status='RUNNING', started_at=? WHERE id=? AND status='PENDING'",
                Instant.now().toString(),
                cmdId
        );
        return updated == 1 ? Optional.of(cmdId) : Optional.empty();
    }

    private static CommandDto map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CommandDto(
                rs.getLong("id"),
                rs.getString("stack_id"),
                rs.getString("type"),
                rs.getString("payload_json"),
                rs.getString("status"),
                parseInstant(rs.getString("created_at")),
                parseInstant(rs.getString("started_at")),
                parseInstant(rs.getString("ended_at")),
                rs.getString("error_message")
        );
    }

    private static Instant parseInstant(String s) {
        return (s == null || s.isBlank()) ? null : Instant.parse(s);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
