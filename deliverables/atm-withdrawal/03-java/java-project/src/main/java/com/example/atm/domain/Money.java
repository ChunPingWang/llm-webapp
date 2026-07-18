package com.example.atm.domain;

import java.util.Objects;

/**
 * 金額值物件（Value Object），確保金額不為負且提供不可變運算。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不得為負數: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "比較對象不得為 null");
        return this.amount >= other.amount;
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "扣除金額不得為 null");
        return new Money(this.amount - other.amount);
    }

    @Override
    public String toString() {
        return amount + " 元";
    }
}
