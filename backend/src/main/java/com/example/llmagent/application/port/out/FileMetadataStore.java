package com.example.llmagent.application.port.out;

import java.util.Optional;

import com.example.llmagent.domain.file.StoredFile;

/** 上傳檔案中繼資料儲存 port(WP6-T1)。 */
public interface FileMetadataStore {

    void save(StoredFile file);

    Optional<StoredFile> findById(String fileId);
}
