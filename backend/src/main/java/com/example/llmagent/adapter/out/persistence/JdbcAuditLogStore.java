package com.example.llmagent.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.AuditLogStore;

/** PostgreSQL 版稽核日誌(WP4-T3):audit_logs 表落地,可依 conversation 查詢。 */
@Repository
@Profile("postgres")
@Primary
public class JdbcAuditLogStore implements AuditLogStore {

    private final JdbcTemplate jdbc;

    public JdbcAuditLogStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(String conversationId, String level, String source, String payload) {
        jdbc.update("""
                INSERT INTO audit_logs (conversation_id, level, source, payload)
                VALUES (?, ?, ?, ?)
                """,
                conversationId == null ? null : UUID.fromString(conversationId),
                level, source, payload);
    }

    @Override
    public List<AuditEntry> findByConversation(String conversationId) {
        return jdbc.query("""
                SELECT id, conversation_id, level, source, payload, created_at
                FROM audit_logs WHERE conversation_id = ? ORDER BY id
                """,
                (rs, i) -> new AuditEntry(
                        rs.getLong("id"),
                        rs.getString("conversation_id"),
                        rs.getString("level"),
                        rs.getString("source"),
                        rs.getString("payload"),
                        rs.getTimestamp("created_at").toInstant()),
                UUID.fromString(conversationId));
    }
}
