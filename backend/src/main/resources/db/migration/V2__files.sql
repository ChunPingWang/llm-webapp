-- WP6-T1:上傳檔案中繼資料(實體檔存 MinIO,storage_key 為物件鍵)。
CREATE TABLE files (
    id           UUID PRIMARY KEY,
    filename     TEXT        NOT NULL,
    content_type TEXT,
    size_bytes   BIGINT      NOT NULL,
    storage_key  TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
