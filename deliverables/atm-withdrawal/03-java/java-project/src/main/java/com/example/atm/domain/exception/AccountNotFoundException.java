package com.example.atm.domain.exception;

/**
 * 領域例外：查無帳戶。
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
