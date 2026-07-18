package com.example.llmagent.adapter.out.persistence;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.FileMetadataStore;
import com.example.llmagent.domain.file.StoredFile;

/** PostgreSQL 版檔案中繼資料(files 表,V2 migration)。 */
@Repository
@Profile("postgres")
@Primary
public class JdbcFileMetadataStore implements FileMetadataStore {

    private static final RowMapper<StoredFile> MAPPER = (rs, i) -> new StoredFile(
            rs.getString("id"),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("storage_key"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcFileMetadataStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(StoredFile f) {
        jdbc.update("""
                INSERT INTO files (id, filename, content_type, size_bytes, storage_key, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.fromString(f.id()), f.filename(), f.contentType(),
                f.sizeBytes(), f.storageKey(), Timestamp.from(f.createdAt()));
    }

    @Override
    public Optional<StoredFile> findById(String fileId) {
        return jdbc.query("SELECT * FROM files WHERE id = ?", MAPPER, UUID.fromString(fileId))
                .stream().findFirst();
    }
}
