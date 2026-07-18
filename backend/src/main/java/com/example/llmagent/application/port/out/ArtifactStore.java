package com.example.llmagent.application.port.out;

import java.util.List;
import java.util.Optional;

import com.example.llmagent.domain.artifact.Artifact;

/** 產出物儲存 port(ADR-005)。版本以「對話 × 型別」為範圍遞增。 */
public interface ArtifactStore {

    void save(String conversationId, Artifact artifact);

    List<Artifact> findByMessageId(String messageId);

    Optional<Artifact> findById(String artifactId);

    /** 該對話中指定型別的下一個版本號(從 1 起)。 */
    int nextVersion(String conversationId, Artifact.ArtifactType type);

    /** 該對話中指定型別的全部版本(舊→新),供 diff。 */
    List<Artifact> findByConversationAndType(String conversationId, Artifact.ArtifactType type);
}
