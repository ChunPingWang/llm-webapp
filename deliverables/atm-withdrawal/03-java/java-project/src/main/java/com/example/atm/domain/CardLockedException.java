package com.example.atm.domain;

/**
 * 提款卡已鎖定領域例外。
 */
public class CardLockedException extends RuntimeException {
    public CardLockedException(String message) {
        super(message);
    }
}
