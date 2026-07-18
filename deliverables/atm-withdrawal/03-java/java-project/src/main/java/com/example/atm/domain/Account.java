package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root），維護密碼驗證與餘額變更的一致性邊界。
 */
public class Account {

    private final AccountId id;
    private final Pin pin;
    private Money balance;

    public Account(AccountId id, Pin pin, Money balance) {
        this.id = Objects.requireNonNull(id, "id 不可為 null");
        this.pin = Objects.requireNonNull(pin, "pin 不可為 null");
        this.balance = Objects.requireNonNull(balance, "balance 不可為 null");
    }

    public boolean verifyPin(Pin input) {
        Objects.requireNonNull(input, "input 不可為 null");
        return this.pin.matches(input);
    }

    /**
     * 提款：金額必須為正，且不得超過餘額（BR-03、BR-04）。
     *
     * @throws IllegalArgumentException      金額 ≤ 0
     * @throws InsufficientBalanceException  餘額不足
     */
    public void withdraw(Money requested) {
        Objects.requireNonNull(requested, "requested 不可為 null");
        if (!requested.isPositive()) {
            throw new IllegalArgumentException("提款金額必須大於 0");
        }
        if (!this.balance.isGreaterThanOrEqual(requested)) {
            throw new InsufficientBalanceException("餘額不足");
        }
        this.balance = this.balance.subtract(requested);
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }
}
