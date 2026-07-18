package com.example.llmagent.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.application.ArtifactService;
import com.example.llmagent.application.port.out.AuditLogStore;
import com.example.llmagent.domain.artifact.Artifact;

/**
 * 產出物與稽核端點(WP5-T1/T3、WP4-T3;openapi:/api/messages/{id}/artifacts、/api/artifacts/{id}/download)。
 */
@RestController
@RequestMapping("/api")
public class ArtifactController {

    private final ArtifactService artifacts;
    private final AuditLogStore auditLog;
    private final com.example.llmagent.application.port.out.ConversationStore conversations;

    public ArtifactController(ArtifactService artifacts, AuditLogStore auditLog,
                              com.example.llmagent.application.port.out.ConversationStore conversations) {
        this.artifacts = artifacts;
        this.auditLog = auditLog;
        this.conversations = conversations;
    }

    /**
     * 該訊息之產出物。若給定的是 user 訊息 id,回傳其後緊接之 assistant 回覆的產出物
     * (前端只知道觸發串流的 user messageId)。
     */
    @GetMapping("/messages/{messageId}/artifacts")
    public List<Artifact> byMessage(@PathVariable String messageId) {
        List<Artifact> direct = artifacts.byMessage(messageId);
        if (!direct.isEmpty()) {
            return direct;
        }
        return conversations.findByMessageId(messageId)
                .map(c -> {
                    var msgs = c.messages();
                    for (int i = 0; i < msgs.size(); i++) {
                        if (msgs.get(i).id().equals(messageId)) {
                            for (int j = i + 1; j < msgs.size(); j++) {
                                if (msgs.get(j).role() == com.example.llmagent.domain.chat.Role.ASSISTANT) {
                                    return artifacts.byMessage(msgs.get(j).id());
                                }
                            }
                        }
                    }
                    return List.<Artifact>of();
                })
                .orElse(List.of());
    }

    /** 該對話中某型別的全部版本(舊→新),供版本 diff(WP5-T3)。 */
    @GetMapping("/conversations/{conversationId}/artifacts")
    public List<Artifact> versions(@PathVariable String conversationId,
                                   @RequestParam Artifact.ArtifactType type) {
        return artifacts.versions(conversationId, type);
    }

    @GetMapping("/artifacts/{artifactId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String artifactId,
                                           @RequestParam(required = false) String format) {
        Artifact a = artifacts.byId(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("artifact not found: " + artifactId));
        String ext = format != null && !format.isBlank() ? format : a.type().extension();
        String filename = URLEncoder.encode(
                "artifact-" + a.type().name().toLowerCase() + "-v" + a.version() + "." + ext,
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(a.content().getBytes(StandardCharsets.UTF_8));
    }

    /** 稽核查詢(WP4-T3):依對話回傳全部事件。 */
    @GetMapping("/conversations/{conversationId}/audit-logs")
    public List<AuditLogStore.AuditEntry> auditLogs(@PathVariable String conversationId) {
        return auditLog.findByConversation(conversationId);
    }
}
