package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 金額值物件（Value Object）。
 * 不可變、非負，封裝金額運算業務規則。
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

    public boolean isGreaterThanOrEqualTo(Money other) {
        Objects.requireNonNull(other, "比較金額不可為 null");
        return this.amount >= other.amount;
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "扣除金額不可為 null");
        return new Money(this.amount - other.amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "增加金額不可為 null");
        return new Money(this.amount + other.amount);
    }
}
