package com.example.llmagent.domain.chat;

import java.time.Instant;

import org.springframework.lang.Nullable;

/**
 * 對話中的單則訊息。{@code modelId} 記錄該則訊息實際使用(或要求切換)的模型;
 * {@code agentProfileId/Version} 記錄產生當下的 Agent Profile,支援產出物追溯(規劃書 §6)
 * 與對話中切換(WP3-T3)。
 */
public record Message(
        String id,
        Role role,
        @Nullable String modelId,
        @Nullable String agentProfileId,
        @Nullable Integer agentProfileVersion,
        String content,
        Instant createdAt
) {
    public static Message user(String id, String content, Instant at) {
        return new Message(id, Role.USER, null, null, null, content, at);
    }

    public static Message user(String id, String content, @Nullable String modelId,
                               @Nullable String agentProfileId, @Nullable Integer agentProfileVersion,
                               Instant at) {
        return new Message(id, Role.USER, modelId, agentProfileId, agentProfileVersion, content, at);
    }

    public static Message assistant(String id, String modelId, String content, Instant at) {
        return new Message(id, Role.ASSISTANT, modelId, null, null, content, at);
    }
}
