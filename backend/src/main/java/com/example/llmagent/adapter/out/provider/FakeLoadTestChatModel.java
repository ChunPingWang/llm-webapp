package com.example.llmagent.adapter.out.provider;

import java.time.Duration;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.llmagent.application.port.out.ChatCall;
import com.example.llmagent.application.port.out.ChatModelPort;
import com.example.llmagent.domain.chat.ChatChunk;
import com.example.llmagent.domain.chat.Usage;

import reactor.core.publisher.Flux;

/**
 * 負載測試用 fake provider(WP8-T3,{@code loadtest} profile)。
 * 每 20ms 一個 chunk、共 50 chunk(約 1 秒串流),不打真實 LLM —— 專測 SSE 管線併發。
 */
@Component
@Profile("loadtest")
@Primary
public class FakeLoadTestChatModel implements ChatModelPort {

    @Override
    public Flux<ChatChunk> stream(ChatCall call) {
        return Flux.interval(Duration.ofMillis(20))
                .take(50)
                .map(i -> ChatChunk.text("chunk-" + i + " "))
                .concatWith(Flux.just(ChatChunk.finalUsage(new Usage(10, 50))));
    }
}
