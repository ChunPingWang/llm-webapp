package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 值物件：金額（新台幣，整數元）。不可為負數。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不可為負數: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(0);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "金額不可為 null");
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "金額不可為 null");
        return new Money(this.amount - other.amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "金額不可為 null");
        return this.amount >= other.amount;
    }

    public boolean isPositive() {
        return this.amount > 0;
    }
}
