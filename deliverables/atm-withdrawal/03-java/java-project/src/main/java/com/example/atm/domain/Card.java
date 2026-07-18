package com.example.atm.domain;

import java.util.Objects;

/**
 * 提款卡聚合根，負責密碼驗證與鎖卡邏輯。
 */
public class Card {

    public static final int MAX_PIN_ATTEMPTS = 3;

    private final String cardNumber;
    private final String correctPin;
    private final Account account;
    private int failedAttempts;
    private CardStatus status;

    public Card(String cardNumber, String correctPin, Account account) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不得為 null");
        this.correctPin = Objects.requireNonNull(correctPin, "密碼不得為 null");
        this.account = Objects.requireNonNull(account, "帳戶不得為 null");
        this.failedAttempts = 0;
        this.status = CardStatus.ACTIVE;
    }

    /**
     * 驗證密碼；正確則重置錯誤次數，錯誤累積達上限則鎖卡。
     *
     * @return true 表示密碼正確
     */
    public boolean verifyPin(String inputPin) {
        if (status == CardStatus.LOCKED) {
            throw new CardLockedException("提款卡已被鎖定，卡號: " + cardNumber);
        }
        if (correctPin.equals(inputPin)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_PIN_ATTEMPTS) {
            status = CardStatus.LOCKED;
        }
        return false;
    }

    public boolean isLocked() {
        return status == CardStatus.LOCKED;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Account getAccount() {
        return account;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public CardStatus getStatus() {
        return status;
    }
}
