package com.example.llmagent.domain.provider;

/**
 * LLM Provider 設定(ADR-001)。{@code apiKeyRef} 為環境變數「名稱」參照 —— 禁止存明碼金鑰,
 * 實際金鑰於呼叫時自環境解析(CLAUDE.md #8)。
 */
public record Provider(
        String id,
        ProviderType type,
        String baseUrl,
        String apiKeyRef,
        boolean enabled
) {

    public enum ProviderType {
        OLLAMA,
        OPENAI_COMPATIBLE,
        ANTHROPIC
    }
}
