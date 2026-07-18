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

import com.example.llmagent.application.port.out.DocxRenderer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

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

    public DocxController(DocxRenderer renderer) {
        this.renderer = renderer;
    }

    public record DocxRequest(@NotBlank String markdown, String title) {
    }

    @PostMapping
    public ResponseEntity<byte[]> generate(@Valid @RequestBody DocxRequest req) {
        String title = req.title() == null || req.title().isBlank() ? "文件" : req.title();
        byte[] bytes = renderer.render(title, req.markdown());
        String filename = URLEncoder.encode(title + ".docx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename).build().toString())
                .body(bytes);
    }
}
