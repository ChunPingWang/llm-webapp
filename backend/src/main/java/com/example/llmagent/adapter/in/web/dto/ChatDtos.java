package com.example.llmagent.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Chat 相關 REST DTO(對映 specs/openapi.yaml)。 */
public final class ChatDtos {

    private ChatDtos() {
    }

    /** POST /api/conversations 請求。 */
    public record CreateConversationRequest(
            String title,
            String agentProfileId,
            String modelId,
            Double temperature,
            String systemPrompt) {
    }

    /** POST /api/conversations 回應。 */
    public record CreateConversationResponse(
            String conversationId,
            String modelId,
            String title) {
    }

    /** POST /api/conversations/{id}/messages 請求。 */
    public record PostMessageRequest(
            @NotBlank String content,
            String modelId,
            String agentProfileId) {
    }

    /** POST /api/conversations/{id}/messages 回應。 */
    public record PostMessageResponse(String messageId) {
    }
}
