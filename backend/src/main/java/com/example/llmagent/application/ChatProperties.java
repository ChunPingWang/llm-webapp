package com.example.llmagent.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 對話預設值。預設模型指向 ICA 最新 Claude(claude-opus-4-8)。
 */
@ConfigurationProperties(prefix = "llmagent.chat")
public record ChatProperties(String defaultModelId, String defaultSystemPrompt) {

    public ChatProperties {
        if (defaultModelId == null || defaultModelId.isBlank()) {
            defaultModelId = "claude-opus-4-8";
        }
        if (defaultSystemPrompt == null || defaultSystemPrompt.isBlank()) {
            defaultSystemPrompt = "你是一位專業的軟體工程助理,回答簡潔、正確,程式碼以 Markdown code fence 呈現。";
        }
    }
}
