package com.example.llmagent.adapter.in.web;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.application.port.out.FileMetadataStore;
import com.example.llmagent.application.port.out.FileStorage;
import com.example.llmagent.domain.file.StoredFile;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 檔案上傳與預覽端點(WP6-T1/T2,openapi:/api/files)。
 * 實體檔存 MinIO;preview 提供 pre-signed URL;content 由後端代理串流
 * (瀏覽器端 docx-preview 免 CORS 直接消費)。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final int PRESIGN_EXPIRY_SECONDS = 600;

    private final FileStorage storage;
    private final FileMetadataStore metadata;

    public FileController(FileStorage storage, FileMetadataStore metadata) {
        this.storage = storage;
        this.metadata = metadata;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Map<String, String>> upload(@RequestPart("file") FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return bytes;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(bytes -> {
                    String id = UUID.randomUUID().toString();
                    String key = "uploads/" + id + "/" + file.filename();
                    String contentType = file.headers().getContentType() == null
                            ? "application/octet-stream"
                            : file.headers().getContentType().toString();
                    storage.put(key, bytes, contentType);
                    metadata.save(new StoredFile(id, file.filename(), contentType,
                            bytes.length, key, Instant.now()));
                    return Map.of(
                            "fileId", id,
                            "filename", file.filename(),
                            "previewUrl", "/api/files/" + id + "/content");
                });
    }

    /** pre-signed URL(ADR-004/openapi);另提供 /content 代理供瀏覽器免 CORS 消費。 */
    @GetMapping("/{fileId}/preview")
    public Mono<Map<String, String>> preview(@PathVariable String fileId) {
        return Mono.fromCallable(() -> {
            StoredFile f = metadata.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("file not found: " + fileId));
            return Map.of(
                    "fileId", f.id(),
                    "filename", f.filename(),
                    "presignedUrl", storage.presignedGetUrl(f.storageKey(), PRESIGN_EXPIRY_SECONDS));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{fileId}/content")
    public Mono<ResponseEntity<byte[]>> content(@PathVariable String fileId) {
        return Mono.fromCallable(() -> {
            StoredFile f = metadata.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("file not found: " + fileId));
            return ResponseEntity.ok()
                    .contentType(f.contentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.parseMediaType(f.contentType()))
                    .body(storage.get(f.storageKey()));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
