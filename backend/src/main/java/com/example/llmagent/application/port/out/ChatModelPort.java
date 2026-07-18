package com.example.llmagent.application.port.out;

import com.example.llmagent.domain.chat.ChatChunk;

import reactor.core.publisher.Flux;

/**
 * 對外 Provider 串流對話 port(hexagonal out)。實作以 Spring AI {@code ChatModel} 為之(ADR-001),
 * 上層 {@code application} 不感知具體 Provider。
 */
public interface ChatModelPort {

    /** 以串流方式取得模型回應片段。 */
    Flux<ChatChunk> stream(ChatCall call);
}
