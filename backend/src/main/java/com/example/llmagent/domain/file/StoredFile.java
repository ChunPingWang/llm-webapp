package com.example.llmagent.domain.file;

import java.time.Instant;

/** 上傳檔案中繼資料(WP6-T1);實體內容存物件儲存,{@code storageKey} 為物件鍵。 */
public record StoredFile(
        String id,
        String filename,
        String contentType,
        long sizeBytes,
        String storageKey,
        Instant createdAt
) {
}
