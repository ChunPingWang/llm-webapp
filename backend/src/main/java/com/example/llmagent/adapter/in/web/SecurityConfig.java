package com.example.llmagent.adapter.in.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 安全設定(WP7-T1)。
 *
 * <p>{@code oidc} profile:啟用 OIDC resource server —— 所有 /api/** 需 Bearer JWT
 * (issuer 由 {@code SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI} 指定,
 * 相容 Keycloak / Auth0 / Entra ID 等企業 IdP);health 端點開放供探針。
 * 預設(無 profile):全開放,便於本機開發。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Profile("oidc")
    SecurityWebFilterChain oidcChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }))
                .build();
    }

    @Bean
    @Profile("!oidc")
    SecurityWebFilterChain openChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
