package com.example.atm.domain.model;

import com.example.atm.domain.exception.InvalidPinFormatException;

import java.util.Objects;

/**
 * 密碼值物件（Value Object）。
 * 保證為 4 位數字格式。
 */
public record Pin(String value) {

    public Pin {
        Objects.requireNonNull(value, "密碼不可為 null");
        if (!value.matches("\\d{4}")) {
            throw new InvalidPinFormatException("密碼必須為 4 位數字, 收到: " + value);
        }
    }

    public static Pin of(String value) {
        return new Pin(value);
    }

    public boolean matches(Pin input) {
        return this.equals(input);
    }
}
