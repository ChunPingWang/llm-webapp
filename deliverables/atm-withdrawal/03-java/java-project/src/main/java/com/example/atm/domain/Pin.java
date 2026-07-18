package com.example.atm.domain;

import java.util.Objects;

/**
 * 密碼值物件（Value Object），格式為 4 位數字。
 */
public record Pin(String value) {

    public Pin {
        Objects.requireNonNull(value, "密碼不可為 null");
        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException("密碼必須為 4 位數字");
        }
    }

    public static Pin of(String value) {
        return new Pin(value);
    }

    public boolean matches(Pin other) {
        return this.equals(other);
    }
}
