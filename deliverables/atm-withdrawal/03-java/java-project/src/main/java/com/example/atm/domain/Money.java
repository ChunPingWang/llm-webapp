package com.example.atm.domain;

import java.util.Objects;

/**
 * 金額值物件（Value Object），以「元」為單位，不可為負。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不可為負數: " + amount);
        }
    }

    public static final Money ZERO = new Money(0);

    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        if (this.amount < other.amount) {
            throw new IllegalArgumentException("餘額不足");
        }
        return new Money(this.amount - other.amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        return this.amount >= other.amount;
    }

    public boolean isPositive() {
        return this.amount > 0;
    }
}
