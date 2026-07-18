package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientBalanceException;

import java.util.Objects;

/**
 * 聚合根：帳戶。封裝密碼驗證與扣款業務規則。
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

    /** BR-01：驗證密碼是否相符。 */
    public boolean verifyPin(Pin input) {
        return this.pin.matches(input);
    }

    /**
     * BR-04：扣款。金額須為正數且不得超過餘額，否則擲出例外。
     */
    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "金額不可為 null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("提款金額須大於 0");
        }
        if (!balance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientBalanceException("帳戶餘額不足");
        }
        this.balance = this.balance.subtract(amount);
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }

    public Money balance() {
        return balance;
    }
}
