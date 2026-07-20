package com.example.llmagent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.llmagent.application.port.out.ArtifactStore;
import com.example.llmagent.domain.artifact.Artifact;
import com.example.llmagent.domain.artifact.ArtifactExtractor;

/**
 * 產出物應用服務(WP5-T1,ADR-005)。串流完成後抽取 code fence,
 * 版本以「對話 × 型別」遞增;格式偏差整段降級為 MARKDOWN。
 */
@Service
public class ArtifactService {

    private final ArtifactStore store;

    public ArtifactService(ArtifactStore store) {
        this.store = store;
    }

    /** 抽取結果摘要(供 ChatService 產生 log 事件)。 */
    public record ExtractResult(List<Artifact> artifacts, boolean degraded) {
    }

    public ExtractResult extractAndStore(String conversationId, String messageId, String content) {
        ArtifactExtractor.Extraction extraction = ArtifactExtractor.extract(content);
        List<Artifact> created = new ArrayList<>();
        for (ArtifactExtractor.Extraction.Piece piece : extraction.pieces()) {
            Artifact a = new Artifact(
                    UUID.randomUUID().toString(), messageId, piece.type(), piece.language(),
                    piece.content(), store.nextVersion(conversationId, piece.type()), Instant.now());
            store.save(conversationId, a);
            created.add(a);
        }
        return new ExtractResult(created, extraction.degraded());
    }

    public List<Artifact> byMessage(String messageId) {
        return store.findByMessageId(messageId);
    }

    public Optional<Artifact> byId(String artifactId) {
        return store.findById(artifactId);
    }

    public List<Artifact> versions(String conversationId, Artifact.ArtifactType type) {
        return store.findByConversationAndType(conversationId, type);
    }

    public void deleteByConversation(String conversationId) {
        store.deleteByConversationId(conversationId);
    }
}
