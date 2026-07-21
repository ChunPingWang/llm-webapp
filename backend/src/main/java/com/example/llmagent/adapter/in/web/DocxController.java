package com.example.llmagent.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import com.example.llmagent.application.port.out.DocxRenderer;
import com.example.llmagent.application.port.out.DocxTemplateFiller;
import com.example.llmagent.application.port.out.FileMetadataStore;
import com.example.llmagent.application.port.out.FileStorage;
import com.example.llmagent.domain.file.StoredFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Word 產生端點(WP6-T3)。前端(docx-preview)以此取得 .docx 位元組並內嵌渲染(ADR-004)。
 * 採無狀態設計:前端傳入 assistant 回覆的原始 Markdown,後端即時轉檔。
 */
@RestController
@RequestMapping("/api/docx")
public class DocxController {

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocxRenderer renderer;
    private final DocxTemplateFiller templateFiller;
    private final FileMetadataStore fileMetadata;
    private final FileStorage fileStorage;

    public DocxController(DocxRenderer renderer, DocxTemplateFiller templateFiller,
                          FileMetadataStore fileMetadata, FileStorage fileStorage) {
        this.renderer = renderer;
        this.templateFiller = templateFiller;
        this.fileMetadata = fileMetadata;
        this.fileStorage = fileStorage;
    }

    /** templateFileId:先經 /api/files 上傳的 Word 範本;設定時以範本套版({{title}} / {{content}} 佔位)。 */
    public record DocxRequest(@NotBlank String markdown, String title, String templateFileId) {
    }

    @PostMapping
    public ResponseEntity<byte[]> generate(@Valid @RequestBody DocxRequest req) {
        String title = req.title() == null || req.title().isBlank() ? "文件" : req.title();
        byte[] bytes;
        if (req.templateFileId() == null || req.templateFileId().isBlank()) {
            bytes = renderer.render(title, req.markdown());
        } else {
            StoredFile tpl = fileMetadata.findById(req.templateFileId())
                    .orElseThrow(() -> new IllegalArgumentException("template not found: " + req.templateFileId()));
            bytes = renderer.renderWithTemplate(title, req.markdown(), fileStorage.get(tpl.storageKey()));
        }
        String filename = URLEncoder.encode(title + ".docx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(bytes);
    }

    /** BRD 模板套版填寫請求(openapi:/api/docx/fill)。values 鍵不含大括號;scenarios 對應「需求場景」重複區塊。 */
    public record FillRequest(
            @NotNull Map<String, String> values,
            List<Map<String, String>> scenarios,
            String templateFileId) {
    }

    @PostMapping("/fill")
    public ResponseEntity<byte[]> fill(@Valid @RequestBody FillRequest req) {
        byte[] template;
        if (req.templateFileId() == null || req.templateFileId().isBlank()) {
            template = builtinBrdTemplate();
        } else {
            StoredFile tpl = fileMetadata.findById(req.templateFileId())
                    .orElseThrow(() -> new IllegalArgumentException("template not found: " + req.templateFileId()));
            template = fileStorage.get(tpl.storageKey());
        }
        byte[] bytes = templateFiller.fill(template, req.values(),
                Map.of("需求場景", req.scenarios() == null ? List.of() : req.scenarios()));
        String title = req.values().getOrDefault("DOC_TITLE",
                req.values().getOrDefault("PROJECT_NAME", "業務需求文件"));
        String filename = URLEncoder.encode(title + ".docx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(bytes);
    }

    private byte[] builtinBrdTemplate() {
        try (InputStream in = DocxController.class.getResourceAsStream("/seed/brd-template.docx")) {
            if (in == null) {
                throw new IllegalStateException("找不到內建 BRD 模板資源");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("讀取內建 BRD 模板失敗", e);
        }
    }
}
