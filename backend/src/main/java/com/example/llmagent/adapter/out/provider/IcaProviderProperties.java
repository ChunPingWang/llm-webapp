package com.example.llmagent.adapter.out.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ICA(OpenAI-Compatible)Provider 連線設定,綁定 {@code llmagent.providers.ica}。
 * {@code baseUrl}/{@code apiKey} 均來自環境變數,禁止明碼(CLAUDE.md #8)。
 */
@ConfigurationProperties(prefix = "llmagent.providers.ica")
public record IcaProviderProperties(
        String type,
        String baseUrl,
        String apiKey,
        String defaultModel) {

    public static final String PROVIDER_ID = "ica";
}
