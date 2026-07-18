package com.example.llmagent.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.AgentProfileStore;
import com.example.llmagent.domain.agent.AgentProfile;

/**
 * PostgreSQL 版 Agent Profile 儲存(ADR-006)。主鍵 (id, version),append-only;
 * tools 以逗號分隔存 TEXT。
 */
@Repository
@Profile("postgres")
@Primary
public class JdbcAgentProfileStore implements AgentProfileStore {

    private static final RowMapper<AgentProfile> MAPPER = (ResultSet rs, int i) -> map(rs);

    private final JdbcTemplate jdbc;

    public JdbcAgentProfileStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static AgentProfile map(ResultSet rs) throws SQLException {
        String tools = rs.getString("tools");
        return new AgentProfile(
                rs.getString("id"),
                rs.getInt("version"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("system_prompt"),
                rs.getString("default_model_id"),
                rs.getObject("temperature", Double.class),
                tools == null || tools.isBlank() ? List.of() : Arrays.asList(tools.split(",")),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant());
    }

    @Override
    public void save(AgentProfile p) {
        jdbc.update("""
                INSERT INTO agent_profiles
                  (id, version, name, description, system_prompt, default_model_id, temperature, tools, enabled, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.fromString(p.id()), p.version(), p.name(), p.description(), p.systemPrompt(),
                p.defaultModelId(), p.temperature(),
                p.tools() == null ? "" : String.join(",", p.tools()),
                p.enabled(), Timestamp.from(p.createdAt()));
    }

    @Override
    public List<AgentProfile> findAllLatest() {
        return jdbc.query("""
                SELECT a.* FROM agent_profiles a
                JOIN (SELECT id, MAX(version) AS v FROM agent_profiles GROUP BY id) m
                  ON a.id = m.id AND a.version = m.v
                ORDER BY a.name
                """, MAPPER);
    }

    @Override
    public Optional<AgentProfile> findLatest(String profileId) {
        List<AgentProfile> found = jdbc.query(
                "SELECT * FROM agent_profiles WHERE id = ? ORDER BY version DESC LIMIT 1",
                MAPPER, UUID.fromString(profileId));
        return found.stream().findFirst();
    }

    @Override
    public Optional<AgentProfile> findVersion(String profileId, int version) {
        return jdbc.query("SELECT * FROM agent_profiles WHERE id = ? AND version = ?",
                MAPPER, UUID.fromString(profileId), version).stream().findFirst();
    }

    @Override
    public List<AgentProfile> findVersions(String profileId) {
        return jdbc.query("SELECT * FROM agent_profiles WHERE id = ? ORDER BY version DESC",
                MAPPER, UUID.fromString(profileId));
    }
}
