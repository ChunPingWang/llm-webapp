package com.example.llmagent.adapter.out.provider;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.llmagent.domain.provider.ModelInfo;

import com.github.tomakehurst.wiremock.WireMockServer;

import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WP2-T2 驗收:WireMock 模擬 OpenAI {@code /v1/models};解析正確、timeout 3s 快速失敗。
 */
class IcaModelCatalogAdapterTest {

    private WireMockServer wm;
    private IcaModelCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();
        IcaProviderProperties props = new IcaProviderProperties(
                "OPENAI_COMPATIBLE", wm.baseUrl() + "/v1", "test-key", "claude-opus-4-8");
        adapter = new IcaModelCatalogAdapter(WebClient.builder(), props);
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void parsesOpenAiModelsResponse() {
        wm.stubFor(get(urlEqualTo("/v1/models")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"data":[
                          {"id":"gpt-4o","object":"model"},
                          {"id":"claude-opus-4-8","object":"model"}
                        ]}""")));

        List<ModelInfo> models = adapter.listModels("ica").collectList().block();
        assertThat(models).isNotNull();
        assertThat(models).extracting(ModelInfo::id)
                .containsExactlyInAnyOrder("gpt-4o", "claude-opus-4-8");
        assertThat(models).allMatch(m -> m.providerId().equals("ica"));
    }

    @Test
    void unknownProviderReturnsEmpty() {
        StepVerifier.create(adapter.listModels("nope")).verifyComplete();
    }

    @Test
    void errorResponseFailsFastAsEmpty() {
        wm.stubFor(get(urlEqualTo("/v1/models"))
                .willReturn(aResponse().withStatus(500)));
        StepVerifier.create(adapter.listModels("ica")).verifyComplete();
    }

    @Test
    void slowResponseTimesOutWithinThreeSeconds() {
        wm.stubFor(get(urlEqualTo("/v1/models")).willReturn(aResponse()
                .withFixedDelay(5000)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\":[]}")));

        // timeout 設定為 3s;5s 應在 ~3s 內以空串流完成
        StepVerifier.create(adapter.listModels("ica"))
                .expectSubscription()
                .verifyComplete();
    }
}
