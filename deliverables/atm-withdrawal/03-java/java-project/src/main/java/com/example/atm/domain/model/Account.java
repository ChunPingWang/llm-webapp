package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root）。
 * 負責密碼驗證與提款扣款之核心業務規則，維護聚合內部一致性。
 */
public class Account {

    private final CardNumber cardNumber;
    private final Pin pin;
    private Money balance;

    public Account(CardNumber cardNumber, Pin pin, Money balance) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不可為 null");
        this.pin = Objects.requireNonNull(pin, "密碼不可為 null");
        this.balance = Objects.requireNonNull(balance, "餘額不可為 null");
    }

    public boolean verifyPin(Pin input) {
        Objects.requireNonNull(input, "輸入密碼不可為 null");
        return pin.matches(input);
    }

    public boolean canWithdraw(Money amount) {
        Objects.requireNonNull(amount, "提款金額不可為 null");
        return amount.isPositive() && balance.isGreaterThanOrEqualTo(amount);
    }

    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "提款金額不可為 null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("提款金額必須大於 0");
        }
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException("餘額不足, 目前餘額: " + balance.amount());
        }
        this.balance = balance.subtract(amount);
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }

    public Money balance() {
        return balance;
    }
}
