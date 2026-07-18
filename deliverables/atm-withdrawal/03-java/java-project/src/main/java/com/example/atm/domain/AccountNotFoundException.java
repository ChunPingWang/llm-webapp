package com.example.atm.domain;

/**
 * 查無帳戶領域例外。
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
