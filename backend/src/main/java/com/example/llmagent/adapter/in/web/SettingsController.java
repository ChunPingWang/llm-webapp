package com.example.llmagent.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.application.RuntimeSettingsService;

/**
 * 執行期設定端點:System Prompt 與 LLM API 連線(base URL / token)。
 *
 * <p>GET 回傳目前值(token 遮罩);PUT 更新,空欄位表示維持不變。
 * 設定僅存記憶體,重啟還原為環境變數值(金鑰不落地)。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final RuntimeSettingsService settings;

    public SettingsController(RuntimeSettingsService settings) {
        this.settings = settings;
    }

    public record SettingsView(String systemPrompt, String baseUrl, String apiKeyMasked, String defaultModelId) {
    }

    public record SettingsUpdate(String systemPrompt, String baseUrl, String apiKey) {
    }

    @GetMapping
    public SettingsView get() {
        return new SettingsView(
                settings.systemPrompt(), settings.baseUrl(),
                settings.apiKeyMasked(), settings.defaultModelId());
    }

    @PutMapping
    public SettingsView update(@RequestBody SettingsUpdate req) {
        settings.update(req.systemPrompt(), req.baseUrl(), req.apiKey());
        return get();
    }
}
