package com.example.llmagent.domain.chat;

/** Token 用量。 */
public record Usage(int promptTokens, int completionTokens) {
    public static final Usage ZERO = new Usage(0, 0);
}
