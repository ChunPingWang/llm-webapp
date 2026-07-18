package com.example.llmagent.application.port.out;

/** 物件儲存 port(WP6-T1,MinIO/S3)。 */
public interface FileStorage {

    /** 寫入物件。 */
    void put(String storageKey, byte[] content, String contentType);

    /** 讀取物件內容。 */
    byte[] get(String storageKey);

    /** 取得限時 pre-signed 下載 URL。 */
    String presignedGetUrl(String storageKey, int expirySeconds);
}
