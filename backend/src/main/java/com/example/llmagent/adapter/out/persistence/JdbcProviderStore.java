package com.example.llmagent.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.ProviderStore;
import com.example.llmagent.domain.provider.Provider;

/** PostgreSQL 版 Provider 儲存(providers 表;api_key_ref 僅存環境變數名稱)。 */
@Repository
@Profile("postgres")
@Primary
public class JdbcProviderStore implements ProviderStore {

    private static final RowMapper<Provider> MAPPER = (rs, i) -> new Provider(
            rs.getString("id"),
            Provider.ProviderType.valueOf(rs.getString("type")),
            rs.getString("base_url"),
            rs.getString("api_key_ref"),
            rs.getBoolean("enabled"));

    private final JdbcTemplate jdbc;

    public JdbcProviderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Provider p) {
        jdbc.update("""
                INSERT INTO providers (id, type, base_url, api_key_ref, enabled)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    type = EXCLUDED.type,
                    base_url = EXCLUDED.base_url,
                    api_key_ref = EXCLUDED.api_key_ref,
                    enabled = EXCLUDED.enabled
                """,
                p.id(), p.type().name(), p.baseUrl(), p.apiKeyRef(), p.enabled());
    }

    @Override
    public List<Provider> findAll() {
        return jdbc.query("SELECT * FROM providers ORDER BY id", MAPPER);
    }

    @Override
    public Optional<Provider> findById(String id) {
        return jdbc.query("SELECT * FROM providers WHERE id = ?", MAPPER, id).stream().findFirst();
    }
}
