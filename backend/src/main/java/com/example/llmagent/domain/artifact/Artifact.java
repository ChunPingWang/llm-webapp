package com.example.llmagent.domain.artifact;

import java.time.Instant;

/**
 * 產出物(ADR-005):自 assistant 回覆抽取的 Gherkin / Java / Markdown,
 * 以對話為範圍、每型別獨立遞增 {@code version},可追溯、可下載。
 */
public record Artifact(
        String id,
        String messageId,
        ArtifactType type,
        String language,
        String content,
        int version,
        Instant createdAt
) {

    public enum ArtifactType {
        GHERKIN("feature"),
        JAVA("java"),
        MARKDOWN("md"),
        DOCX("docx");

        private final String extension;

        ArtifactType(String extension) {
            this.extension = extension;
        }

        public String extension() {
            return extension;
        }
    }
}
