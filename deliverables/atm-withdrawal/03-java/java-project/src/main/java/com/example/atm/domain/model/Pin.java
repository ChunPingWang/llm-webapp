package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 值物件：密碼（4 位數字）。
 */
public record Pin(String value) {

    private static final String PIN_PATTERN = "\\d{4}";

    public Pin {
        Objects.requireNonNull(value, "密碼不可為 null");
        if (!value.matches(PIN_PATTERN)) {
            throw new IllegalArgumentException("密碼須為 4 位數字");
        }
    }

    public static Pin of(String value) {
        return new Pin(value);
    }

    public boolean matches(Pin other) {
        return other != null && this.value.equals(other.value);
    }
}
