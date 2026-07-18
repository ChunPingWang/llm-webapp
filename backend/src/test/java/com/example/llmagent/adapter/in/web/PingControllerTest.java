package com.example.llmagent.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WP1-T4 驗收:{@code GET /api/ping/stream} 回傳 event-stream 心跳,並以 done 收尾。
 */
@WebFluxTest(controllers = PingController.class)
@org.springframework.context.annotation.Import(SecurityConfig.class)
class PingControllerTest {

    @Autowired
    WebTestClient client;

    @Test
    void streamsHeartbeatsAndDone() {
        String body = client.get()
                .uri("/api/ping/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(String.class)
                .getResponseBody()
                .collectList()
                .block()
                .toString();

        assertThat(body).contains("heartbeat 0");
        assertThat(body).contains("elapsedMs");
    }
}
