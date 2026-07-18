package com.example.llmagent.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * WP7-T1 驗收:oidc profile 下 /api/** 未帶 JWT → 401;帶(mock)JWT → 通過;health 開放。
 */
@SpringBootTest(properties = {
        "spring.ai.openai.base-url=http://localhost:9999",
        "spring.ai.openai.api-key=test-key",
})
@AutoConfigureWebTestClient
@ActiveProfiles("oidc")
class OidcSecurityTest {

    @Autowired
    WebTestClient client;

    /** 測試中不連真 IdP;mock decoder 使 context 可啟動。 */
    @MockBean
    ReactiveJwtDecoder jwtDecoder;

    @Test
    void apiWithoutTokenIsUnauthorized() {
        client.get().uri("/api/agent-profiles").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void apiWithJwtIsAllowed() {
        client.mutateWith(mockJwt())
                .get().uri("/api/agent-profiles")
                .exchange().expectStatus().isOk();
    }

    @Test
    void healthIsPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
