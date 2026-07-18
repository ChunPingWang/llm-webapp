package com.example.llmagent.adapter.out.langfuse;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Langfuse Prompt Management 對接(WP4-T5,ADR-006/007)。
 * 依名稱取用 prompt 主版本({@code GET /api/public/v2/prompts/{name}},Basic auth pk:sk);
 * 未設定 LANGFUSE_* 環境變數或取用失敗時回空 —— 呼叫端退回 DB 版本(fallback)。
 */
@Component
public class LangfusePromptSource {

    private static final Logger log = LoggerFactory.getLogger(LangfusePromptSource.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    /** 取得的 prompt 與其 Langfuse 版本(供追溯記錄)。 */
    public record LangfusePrompt(String prompt, int version) {
    }

    private final String host;
    private final String authHeader;
    private final WebClient.Builder builder;

    public LangfusePromptSource(WebClient.Builder builder,
                                @Value("${llmagent.langfuse.host:}") String host,
                                @Value("${llmagent.langfuse.public-key:}") String publicKey,
                                @Value("${llmagent.langfuse.secret-key:}") String secretKey) {
        this.builder = builder;
        this.host = host == null ? "" : host.strip();
        this.authHeader = publicKey == null || publicKey.isBlank() ? ""
                : "Basic " + Base64.getEncoder()
                        .encodeToString((publicKey + ":" + secretKey).getBytes());
    }

    public boolean enabled() {
        return !host.isBlank() && !authHeader.isBlank();
    }

    /** 取用 prompt;停用/逾時/錯誤皆回空(呼叫端 fallback 至 DB)。 */
    public Optional<LangfusePrompt> fetch(String promptName) {
        if (!enabled()) {
            return Optional.empty();
        }
        try {
            Map<?, ?> body = builder.clone().baseUrl(host)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                    .build()
                    .get().uri("/api/public/v2/prompts/{name}", promptName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block(TIMEOUT.plusSeconds(1));
            if (body == null || body.get("prompt") == null) {
                return Optional.empty();
            }
            int version = body.get("version") instanceof Number n ? n.intValue() : 0;
            return Optional.of(new LangfusePrompt(String.valueOf(body.get("prompt")), version));
        } catch (Exception e) {
            log.warn("Langfuse prompt 取用失敗({}),fallback 至 DB:{}", promptName, e.toString());
            return Optional.empty();
        }
    }
}
