package com.example.llmagent.application;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.llmagent.application.port.out.ProviderStore;
import com.example.llmagent.domain.provider.Provider;

import reactor.core.publisher.Mono;

/**
 * Provider 註冊與連線測試(WP2-T1/T5,ADR-001)。
 * apiKeyRef 僅為環境變數「名稱」;實際金鑰於測試/呼叫當下自環境解析,不落地。
 */
@Service
public class ProviderService {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final ProviderStore store;
    private final WebClient.Builder webClientBuilder;

    public ProviderService(ProviderStore store, WebClient.Builder webClientBuilder) {
        this.store = store;
        this.webClientBuilder = webClientBuilder;
    }

    public List<Provider> list() {
        return store.findAll();
    }

    public Provider register(Provider provider) {
        if (provider.apiKeyRef() != null && provider.apiKeyRef().startsWith("sk-")) {
            throw new IllegalStateException("apiKeyRef 僅接受環境變數名稱參照,禁止明碼金鑰(ADR-001)");
        }
        store.save(provider);
        return provider;
    }

    /** 連線測試結果。 */
    public record TestResult(boolean ok, long latencyMs, int models, String error) {
    }

    /** 依 Provider 型別打對應管理端點(3s 逾時快速失敗);reactive,不阻塞 event loop。 */
    public Mono<TestResult> testConnection(String providerId) {
        Provider p = store.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("provider not found: " + providerId));
        String apiKey = p.apiKeyRef() == null ? null : System.getenv(p.apiKeyRef());

        String path;
        WebClient.Builder b = webClientBuilder.clone().baseUrl(p.baseUrl());
        switch (p.type()) {
            case OLLAMA -> path = "/api/tags";
            case ANTHROPIC -> {
                path = "/v1/models";
                if (apiKey != null) {
                    b.defaultHeader("x-api-key", apiKey).defaultHeader("anthropic-version", "2023-06-01");
                }
            }
            default -> {
                path = "/v1/models";
                if (apiKey != null) {
                    b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
                }
            }
        }

        long start = System.currentTimeMillis();
        return b.build().get().uri(path)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .map(body -> {
                    long ms = System.currentTimeMillis() - start;
                    int models = 0;
                    Object data = body.containsKey("data") ? body.get("data") : body.get("models");
                    if (data instanceof List<?> l) {
                        models = l.size();
                    }
                    return new TestResult(true, ms, models, null);
                })
                .onErrorResume(e -> Mono.just(new TestResult(
                        false, System.currentTimeMillis() - start, 0,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
    }

    /** 預設 ICA Provider 種子(冪等)。 */
    @Configuration
    static class Seeder {
        @Bean
        ApplicationRunner seedProviders(ProviderStore store, RuntimeSettingsService settings) {
            return args -> {
                if (store.findById("ica").isEmpty()) {
                    store.save(new Provider("ica", Provider.ProviderType.OPENAI_COMPATIBLE,
                            settings.baseUrl(), "ICA_CLAUDE_KEY", true));
                }
            };
        }
    }
}
