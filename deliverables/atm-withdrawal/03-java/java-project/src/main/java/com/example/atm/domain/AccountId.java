package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶識別碼值物件，用於卡片與帳戶對應。
 */
public record AccountId(String value) {

    public AccountId {
        Objects.requireNonNull(value, "帳戶識別碼不可為 null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("帳戶識別碼不可為空白");
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
    }
}
