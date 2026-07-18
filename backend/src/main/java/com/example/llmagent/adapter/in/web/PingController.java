package com.example.llmagent.adapter.in.web;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.domain.sse.SseEventType;

import reactor.core.publisher.Flux;

/**
 * SSE 骨架端點(WP1-T4)。驗證 Reactor {@code Flux<ServerSentEvent>} 串流管線可用,
 * 為後續 chat 串流(ADR-002/003)奠基。
 */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    /**
     * 心跳串流:每秒送出一筆 {@code log} 事件,共五筆後以 {@code done} 收尾。
     *
     * @return text/event-stream
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream() {
        Flux<ServerSentEvent<String>> heartbeats = Flux.interval(Duration.ofSeconds(1))
                .take(5)
                .map(i -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(i))
                        .event(SseEventType.LOG.wireName())
                        .data("{\"level\":\"INFO\",\"source\":\"ping\",\"msg\":\"heartbeat " + i + "\"}")
                        .build());

        Flux<ServerSentEvent<String>> done = Flux.just(ServerSentEvent.<String>builder()
                .event(SseEventType.DONE.wireName())
                .data("{\"elapsedMs\":5000}")
                .build());

        return Flux.concat(heartbeats, done);
    }
}
