package com.example.llmagent.domain.thinking;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.llmagent.domain.sse.SseEventType;

import static org.assertj.core.api.Assertions.assertThat;

class ThinkingParserTest {

    private String joined(ThinkingParser p, SseEventType kind, List<String> deltas) {
        List<Segment> segs = new ArrayList<>();
        for (String d : deltas) {
            segs.addAll(p.accept(d));
        }
        segs.addAll(p.flush());
        StringBuilder sb = new StringBuilder();
        segs.stream().filter(s -> s.kind() == kind).forEach(s -> sb.append(s.text()));
        return sb.toString();
    }

    @Test
    void plainContentPassesThrough() {
        ThinkingParser p = new ThinkingParser();
        assertThat(joined(p, SseEventType.CONTENT, List.of("Hello ", "world")))
                .isEqualTo("Hello world");
    }

    @Test
    void separatesThinkingFromContent() {
        ThinkingParser p = new ThinkingParser();
        List<String> deltas = List.of("<think>reasoning here</think>answer");
        assertThat(joined(new ThinkingParser(), SseEventType.THINKING, deltas)).isEqualTo("reasoning here");
        assertThat(joined(p, SseEventType.CONTENT, deltas)).isEqualTo("answer");
    }

    @Test
    void handlesTagSplitAcrossChunks() {
        List<String> deltas = List.of("<thi", "nk>deep", " thought</thi", "nk>final");
        assertThat(joined(new ThinkingParser(), SseEventType.THINKING, deltas)).isEqualTo("deep thought");
        assertThat(joined(new ThinkingParser(), SseEventType.CONTENT, deltas)).isEqualTo("final");
    }

    @Test
    void unclosedThinkTagFlushedAsThinking() {
        List<String> deltas = List.of("before<think>still thinking");
        assertThat(joined(new ThinkingParser(), SseEventType.CONTENT, deltas)).isEqualTo("before");
        assertThat(joined(new ThinkingParser(), SseEventType.THINKING, deltas)).isEqualTo("still thinking");
    }

    @Test
    void danglingLessThanIsNotSwallowed() {
        // "1 < 2" 不是標籤,結束時應完整輸出
        List<String> deltas = List.of("1 < 2 comparison");
        assertThat(joined(new ThinkingParser(), SseEventType.CONTENT, deltas)).isEqualTo("1 < 2 comparison");
    }
}
