package com.example.atm.domain.exception;

/**
 * 卡片查無帳戶領域例外。
 */
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String message) {
        super(message);
    }
}
