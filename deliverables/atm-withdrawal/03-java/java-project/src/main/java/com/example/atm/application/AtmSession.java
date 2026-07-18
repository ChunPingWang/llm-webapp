package com.example.atm.application;

import com.example.atm.domain.model.CardNumber;

import java.util.Objects;

/**
 * ATM 交易會話（Application 層狀態物件）。
 * 表達卡片插入與驗證狀態，符合 SRP。
 */
public class AtmSession {

    private final CardNumber cardNumber;
    private boolean authenticated;
    private boolean active;

    private AtmSession(CardNumber cardNumber) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不可為 null");
        this.authenticated = false;
        this.active = true;
    }

    public static AtmSession start(CardNumber cardNumber) {
        return new AtmSession(cardNumber);
    }

    public void markAuthenticated() {
        this.authenticated = true;
    }

    public boolean canSelectService() {
        return active && authenticated;
    }

    public void end() {
        this.active = false;
        this.authenticated = false;
    }

    public boolean isActive() {
        return active;
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }
}
