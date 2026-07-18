package com.example.llmagent.domain.agent;

import java.time.Instant;
import java.util.List;

/**
 * Agent Profile(ADR-006):定義一種任務型 Agent 的 system prompt、預設模型與參數。
 * Prompt 修改採 append-only 版本化 —— 每次更新產生新 {@code version},舊版永遠可查,
 * 產出物可追溯至生成當下的 prompt 版本。
 */
public record AgentProfile(
        String id,
        int version,
        String name,
        String description,
        String systemPrompt,
        String defaultModelId,
        Double temperature,
        List<String> tools,
        boolean enabled,
        Instant createdAt
) {

    /** 以本 Profile 為基礎產生下一版(欄位取自最新內容)。 */
    public AgentProfile nextVersion(String name, String description, String systemPrompt,
                                    String defaultModelId, Double temperature,
                                    List<String> tools, Instant at) {
        return new AgentProfile(id, version + 1, name, description, systemPrompt,
                defaultModelId, temperature, tools, enabled, at);
    }
}
