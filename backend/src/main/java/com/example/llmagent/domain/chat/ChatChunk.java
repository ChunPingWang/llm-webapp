package com.example.llmagent.domain.chat;

import org.springframework.lang.Nullable;

/**
 * Provider 串流輸出的單一片段(domain 層,與 Provider 技術無關)。
 *
 * @param textDelta 本片段的文字增量(可能為空字串)
 * @param usage     僅最終片段帶有 token 用量;其餘為 {@code null}
 */
public record ChatChunk(String textDelta, @Nullable Usage usage) {

    public static ChatChunk text(String delta) {
        return new ChatChunk(delta, null);
    }

    public static ChatChunk finalUsage(Usage usage) {
        return new ChatChunk("", usage);
    }
}
