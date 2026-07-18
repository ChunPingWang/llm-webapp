package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 卡號值物件（Value Object）。
 */
public record CardNumber(String value) {

    public CardNumber {
        Objects.requireNonNull(value, "卡號不可為 null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("卡號不可為空白");
        }
    }

    public static CardNumber of(String value) {
        return new CardNumber(value);
    }
}
