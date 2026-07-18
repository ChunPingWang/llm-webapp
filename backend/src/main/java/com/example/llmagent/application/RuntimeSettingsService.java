package com.example.llmagent.application;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 執行期可調設定(System Prompt / LLM API base URL / API Token / 預設模型)。
 *
 * <p>初始值來自環境變數(application.yaml),之後可經 {@code PUT /api/settings} 於執行期修改;
 * 僅存於記憶體,重啟即還原為環境值 —— 金鑰不落地,符合 CLAUDE.md #8。
 * {@link #version()} 遞增供 adapter 判斷是否需重建連線。
 */
@Service
public class RuntimeSettingsService {

    private final AtomicLong version = new AtomicLong(1);

    private volatile String systemPrompt;
    private volatile String baseUrl;
    private volatile String apiKey;
    private volatile String defaultModelId;

    public RuntimeSettingsService(ChatProperties chatProps,
                                  @Value("${spring.ai.openai.base-url}") String baseUrl,
                                  @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.systemPrompt = chatProps.defaultSystemPrompt();
        this.defaultModelId = chatProps.defaultModelId();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public long version() {
        return version.get();
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String defaultModelId() {
        return defaultModelId;
    }

    /** 遮罩後的 token(僅顯示前 4 碼),供設定畫面呈現。 */
    public String apiKeyMasked() {
        String k = apiKey;
        if (k == null || k.length() <= 4) {
            return "****";
        }
        return k.substring(0, 4) + "*".repeat(Math.min(k.length() - 4, 20));
    }

    /**
     * 更新設定;null/blank 欄位表示「維持不變」。
     * 連線相關(baseUrl / apiKey)有變更時 bump version,通知 adapter 重建。
     */
    public synchronized void update(String systemPrompt, String baseUrl, String apiKey) {
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            this.systemPrompt = systemPrompt;
        }
        boolean connectionChanged = false;
        if (baseUrl != null && !baseUrl.isBlank() && !baseUrl.equals(this.baseUrl)) {
            this.baseUrl = baseUrl.strip();
            connectionChanged = true;
        }
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals(this.apiKey)) {
            this.apiKey = apiKey.strip();
            connectionChanged = true;
        }
        if (connectionChanged) {
            version.incrementAndGet();
        }
    }
}
