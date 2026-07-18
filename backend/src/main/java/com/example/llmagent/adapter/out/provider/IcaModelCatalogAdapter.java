package com.example.llmagent.adapter.out.provider;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.llmagent.application.port.out.ModelCatalogPort;
import com.example.llmagent.domain.provider.ModelInfo;

import reactor.core.publisher.Flux;

/**
 * ICA 模型清單 adapter(WP2-T2)。呼叫 OpenAI-Compatible {@code /models} 管理端點,
 * 3 秒逾時快速失敗(規劃書 §4「model list 拉取需設短 timeout」)。
 */
@Component
public class IcaModelCatalogAdapter implements ModelCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(IcaModelCatalogAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public IcaModelCatalogAdapter(WebClient.Builder builder, IcaProviderProperties props) {
        this.webClient = builder
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .build();
    }

    @Override
    public Flux<ModelInfo> listModels(String providerId) {
        if (!IcaProviderProperties.PROVIDER_ID.equals(providerId)) {
            return Flux.empty();
        }
        return webClient.get()
                .uri("/models")
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
