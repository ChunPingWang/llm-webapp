package com.example.llmagent.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.llmagent.application.port.out.ConversationStore;
import com.example.llmagent.domain.chat.Conversation;
import com.example.llmagent.domain.chat.Message;
import com.example.llmagent.domain.chat.Role;

/**
 * PostgreSQL 版對話儲存(WP1-T3)。{@code postgres} profile 啟用並取代 in-memory;
 * 訊息 append-only,以 {@code (conversation_id, seq)} 保序。
 */
@Repository
@Profile("postgres")
@Primary
public class JdbcConversationStore implements ConversationStore {

    private final JdbcTemplate jdbc;

    public JdbcConversationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void save(Conversation c) {
        jdbc.update("""
                INSERT INTO conversations (id, title, system_prompt, default_model_id, temperature, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    system_prompt = EXCLUDED.system_prompt,
                    default_model_id = EXCLUDED.default_model_id
                """,
                UUID.fromString(c.id()), c.title(), c.systemPrompt(), c.defaultModelId(),
                c.temperature(), Timestamp.from(c.createdAt()));

        List<Message> messages = c.messages();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            jdbc.update("""
                    INSERT INTO messages
                        (id, conversation_id, seq, role, model_id, agent_profile_id, agent_profile_version, content, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                    """,
                    UUID.fromString(m.id()), UUID.fromString(c.id()), i,
                    m.role().name(), m.modelId(),
                    m.agentProfileId() == null ? null : UUID.fromString(m.agentProfileId()),
                    m.agentProfileVersion(), m.content(), Timestamp.from(m.createdAt()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(String conversationId) {
        List<Conversation> found = jdbc.query("""
                SELECT id, title, system_prompt, default_model_id, temperature, created_at
                FROM conversations WHERE id = ?
                """,
                (rs, i) -> new Conversation(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("system_prompt"),
                        rs.getString("default_model_id"),
                        rs.getObject("temperature", Double.class),
                        rs.getTimestamp("created_at").toInstant()),
                UUID.fromString(conversationId));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Conversation c = found.get(0);
        jdbc.query("""
                SELECT id, role, model_id, agent_profile_id, agent_profile_version, content, created_at
                FROM messages WHERE conversation_id = ? ORDER BY seq
                """,
                rs -> {
                    Instant at = rs.getTimestamp("created_at").toInstant();
                    c.addMessage(new Message(
                            rs.getString("id"),
                            Role.valueOf(rs.getString("role")),
                            rs.getString("model_id"),
                            rs.getString("agent_profile_id"),
                            rs.getObject("agent_profile_version", Integer.class),
                            rs.getString("content"),
                            at));
                },
                UUID.fromString(conversationId));
        return Optional.of(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findByMessageId(String messageId) {
        List<String> convIds = jdbc.query(
                "SELECT conversation_id FROM messages WHERE id = ?",
                (rs, i) -> rs.getString(1),
                UUID.fromString(messageId));
        return convIds.isEmpty() ? Optional.empty() : findById(convIds.get(0));
    }

    @Override
    @Transactional
    public void deleteById(String conversationId) {
        // messages 與 artifacts 由 FK ON DELETE CASCADE 一併刪除(V1__init.sql)
        jdbc.update("DELETE FROM conversations WHERE id = ?", UUID.fromString(conversationId));
    }
}
