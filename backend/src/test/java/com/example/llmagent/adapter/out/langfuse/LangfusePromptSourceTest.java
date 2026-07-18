package com.example.llmagent.adapter.out.langfuse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/** WP4-T5 驗收:Langfuse 取用(Basic auth、版本記錄)+ 失敗 fallback(回空)。 */
class LangfusePromptSourceTest {

    private WireMockServer wm;
    private LangfusePromptSource source;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();
        source = new LangfusePromptSource(WebClient.builder(), wm.baseUrl(), "pk-test", "sk-test");
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void fetchesPromptWithVersionUsingBasicAuth() {
        wm.stubFor(get(urlEqualTo("/api/public/v2/prompts/BDD%20Agent"))
                .withHeader("Authorization", containing("Basic "))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"prompt\":\"remote prompt {x}\",\"version\":7}")));

        var result = source.fetch("BDD Agent");
        assertThat(result).isPresent();
        assertThat(result.get().prompt()).isEqualTo("remote prompt {x}");
        assertThat(result.get().version()).isEqualTo(7);
    }

    @Test
    void notFoundFallsBackToEmpty() {
        wm.stubFor(get(urlEqualTo("/api/public/v2/prompts/missing"))
                .willReturn(aResponse().withStatus(404)));
        assertThat(source.fetch("missing")).isEmpty();
    }

    @Test
    void disabledWhenNoKeysConfigured() {
        LangfusePromptSource off = new LangfusePromptSource(WebClient.builder(), "", "", "");
        assertThat(off.enabled()).isFalse();
        assertThat(off.fetch("any")).isEmpty();
    }
}
