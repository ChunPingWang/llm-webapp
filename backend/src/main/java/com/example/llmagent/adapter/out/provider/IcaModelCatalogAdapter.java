package com.example.llmagent.adapter.out.provider;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.llmagent.application.RuntimeSettingsService;
import com.example.llmagent.application.port.out.ModelCatalogPort;
import com.example.llmagent.domain.provider.ModelInfo;

import reactor.core.publisher.Flux;

/**
 * ICA 模型清單 adapter(WP2-T2)。呼叫 OpenAI-Compatible {@code /v1/models} 管理端點,
 * 3 秒逾時快速失敗(規劃書 §4)。連線參數來自 {@link RuntimeSettingsService},
 * 執行期修改 URL/token 後,下次呼叫即用新值。
 */
@Component
public class IcaModelCatalogAdapter implements ModelCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(IcaModelCatalogAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final String PROVIDER_ID = "ica";

    private final RuntimeSettingsService settings;
    private final WebClient.Builder builder;

    private volatile WebClient webClient;
    private volatile long builtVersion = -1;

    public IcaModelCatalogAdapter(WebClient.Builder builder, RuntimeSettingsService settings) {
        this.builder = builder;
        this.settings = settings;
    }

    private WebClient client() {
        long v = settings.version();
        WebClient c = webClient;
        if (c == null || builtVersion != v) {
            synchronized (this) {
                if (webClient == null || builtVersion != settings.version()) {
                    webClient = builder.clone()
                            .baseUrl(settings.baseUrl())
                            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey())
                            .build();
                    builtVersion = settings.version();
                }
                c = webClient;
            }
        }
        return c;
    }

    @Override
    public Flux<ModelInfo> listModels(String providerId) {
        if (!PROVIDER_ID.equals(providerId)) {
            return Flux.empty();
        }
        return client().get()
                .uri("/v1/models")
                .retrieve()
                .bodyToMono(OpenAiModelsResponse.class)
                .timeout(TIMEOUT)
                .flatMapMany(resp -> Flux.fromIterable(resp.data() == null ? List.of() : resp.data()))
                .map(m -> new ModelInfo(m.id(), providerId, null))
                .onErrorResume(err -> {
                    log.warn("ICA 模型清單拉取失敗:{}", err.toString());
                    return Flux.empty();
                });
    }

    /** OpenAI /v1/models 回應片段。 */
    record OpenAiModelsResponse(List<OpenAiModel> data) {
    }

    record OpenAiModel(String id) {
    }
}
