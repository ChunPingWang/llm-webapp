package com.example.llmagent.adapter.out.storage;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.llmagent.application.port.out.FileStorage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

/**
 * MinIO(S3 API)物件儲存 adapter(WP6-T1)。bucket 於首次使用時自動建立。
 * 注意:MinIO SDK 為 blocking,呼叫端(FileController)以 boundedElastic 調度。
 */
@Component
public class MinioFileStorage implements FileStorage {

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketChecked;

    public MinioFileStorage(
            @Value("${llmagent.storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${llmagent.storage.access-key:minioadmin}") String accessKey,
            @Value("${llmagent.storage.secret-key:minioadmin}") String secretKey,
            @Value("${llmagent.storage.bucket:llm-webapp}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
    }

    private void ensureBucket() {
        if (bucketChecked) {
            return;
        }
        synchronized (this) {
            if (bucketChecked) {
                return;
            }
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
                bucketChecked = true;
            } catch (Exception e) {
                throw new IllegalStateException("MinIO bucket 檢查/建立失敗: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void put(String storageKey, byte[] content, String contentType) {
        ensureBucket();
        try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(storageKey)
                    .stream(in, content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 上傳失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] get(String storageKey) {
        ensureBucket();
        try (var in = client.getObject(GetObjectArgs.builder().bucket(bucket).object(storageKey).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 讀取失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public String presignedGetUrl(String storageKey, int expirySeconds) {
        ensureBucket();
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket).object(storageKey)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("MinIO presigned URL 失敗: " + e.getMessage(), e);
        }
    }
}
