package com.example.llmagent.adapter.out.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.FileMetadataStore;
import com.example.llmagent.domain.file.StoredFile;

/** 記憶體版檔案中繼資料。 */
@Repository
public class InMemoryFileMetadataStore implements FileMetadataStore {

    private final Map<String, StoredFile> byId = new ConcurrentHashMap<>();

    @Override
    public void save(StoredFile file) {
        byId.put(file.id(), file);
    }

    @Override
    public Optional<StoredFile> findById(String fileId) {
        return Optional.ofNullable(byId.get(fileId));
    }
}
