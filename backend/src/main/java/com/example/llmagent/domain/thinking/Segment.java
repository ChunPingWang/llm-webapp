package com.example.llmagent.domain.thinking;

import com.example.llmagent.domain.sse.SseEventType;

/**
 * ThinkingParser 輸出的片段:{@link SseEventType#THINKING} 或 {@link SseEventType#CONTENT}。
 */
public record Segment(SseEventType kind, String text) {

    public static Segment thinking(String text) {
        return new Segment(SseEventType.THINKING, text);
    }

    public static Segment content(String text) {
        return new Segment(SseEventType.CONTENT, text);
    }
}
