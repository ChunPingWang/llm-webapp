package com.example.llmagent.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.ArtifactStore;
import com.example.llmagent.domain.artifact.Artifact;

/** PostgreSQL 版產出物儲存(ADR-005)。版本範圍 = 對話 × 型別(經 messages join)。 */
@Repository
@Profile("postgres")
@Primary
public class JdbcArtifactStore implements ArtifactStore {

    private static final RowMapper<Artifact> MAPPER = (ResultSet rs, int i) -> map(rs);

    private final JdbcTemplate jdbc;

    public JdbcArtifactStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Artifact map(ResultSet rs) throws SQLException {
        return new Artifact(
                rs.getString("id"),
                rs.getString("message_id"),
                Artifact.ArtifactType.valueOf(rs.getString("type")),
                rs.getString("language"),
                rs.getString("content"),
                rs.getInt("version"),
                rs.getTimestamp("created_at").toInstant());
    }

    @Override
    public void save(String conversationId, Artifact a) {
        jdbc.update("""
                INSERT INTO artifacts (id, message_id, type, language, content, version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.fromString(a.id()), UUID.fromString(a.messageId()), a.type().name(),
                a.language(), a.content(), a.version(), Timestamp.from(a.createdAt()));
    }

    @Override
    public List<Artifact> findByMessageId(String messageId) {
        return jdbc.query("SELECT * FROM artifacts WHERE message_id = ? ORDER BY version",
                MAPPER, UUID.fromString(messageId));
    }

    @Override
    public Optional<Artifact> findById(String artifactId) {
        return jdbc.query("SELECT * FROM artifacts WHERE id = ?",
                MAPPER, UUID.fromString(artifactId)).stream().findFirst();
    }

    @Override
    public int nextVersion(String conversationId, Artifact.ArtifactType type) {
        Integer max = jdbc.queryForObject("""
                SELECT COALESCE(MAX(a.version), 0) FROM artifacts a
                JOIN messages m ON a.message_id = m.id
                WHERE m.conversation_id = ? AND a.type = ?
                """, Integer.class, UUID.fromString(conversationId), type.name());
        return (max == null ? 0 : max) + 1;
    }

    @Override
    public List<Artifact> findByConversationAndType(String conversationId, Artifact.ArtifactType type) {
        return jdbc.query("""
                SELECT a.* FROM artifacts a
                JOIN messages m ON a.message_id = m.id
                WHERE m.conversation_id = ? AND a.type = ?
                ORDER BY a.version
                """, MAPPER, UUID.fromString(conversationId), type.name());
    }
}
