package com.example.llmagent.domain.chat;

import java.time.Instant;

/**
 * 對話中的單則訊息。{@code modelId} 記錄該則(assistant)訊息實際使用的模型,
 * 支援對話中切換模型(規劃書 §4)。
 */
public record Message(
        String id,
        Role role,
        String modelId,
        String content,
        Instant createdAt
) {
    public static Message user(String id, String content, Instant at) {
        return new Message(id, Role.USER, null, content, at);
    }

    public static Message assistant(String id, String modelId, String content, Instant at) {
        return new Message(id, Role.ASSISTANT, modelId, content, at);
    }
}
