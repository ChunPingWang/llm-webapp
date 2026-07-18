package com.example.llmagent.domain.provider;

import org.springframework.lang.Nullable;

/**
 * 一個可用模型(對映 openapi.yaml ModelInfo)。
 *
 * @param id            模型 id(如 {@code claude-opus-4-8})
 * @param providerId    所屬 Provider(如 {@code ica})
 * @param contextLength 上下文長度(Provider 未提供時為 {@code null})
 */
public record ModelInfo(String id, String providerId, @Nullable Integer contextLength) {
}
