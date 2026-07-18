package com.example.llmagent.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSettingsServiceTest {

    private RuntimeSettingsService newSettings() {
        return new RuntimeSettingsService(
                new ChatProperties("claude-opus-4-8", "original prompt"),
                "https://api.example.com/ica", "secret-token-12345");
    }

    @Test
    void masksApiKey() {
        RuntimeSettingsService s = newSettings();
        assertThat(s.apiKeyMasked()).startsWith("secr");
        assertThat(s.apiKeyMasked()).doesNotContain("token");
    }

    @Test
    void blankFieldsKeepExistingValues() {
        RuntimeSettingsService s = newSettings();
        long v = s.version();
        s.update("", "  ", null);
        assertThat(s.systemPrompt()).isEqualTo("original prompt");
        assertThat(s.baseUrl()).isEqualTo("https://api.example.com/ica");
        assertThat(s.version()).isEqualTo(v); // 連線未變,不 bump
    }

    @Test
    void updatingPromptDoesNotBumpConnectionVersion() {
        RuntimeSettingsService s = newSettings();
        long v = s.version();
        s.update("new prompt", null, null);
        assertThat(s.systemPrompt()).isEqualTo("new prompt");
        assertThat(s.version()).isEqualTo(v);
    }

    @Test
    void updatingConnectionBumpsVersion() {
        RuntimeSettingsService s = newSettings();
        long v = s.version();
        s.update(null, "https://other.example.com", "new-key");
        assertThat(s.baseUrl()).isEqualTo("https://other.example.com");
        assertThat(s.apiKey()).isEqualTo("new-key");
        assertThat(s.version()).isGreaterThan(v);
    }
}
