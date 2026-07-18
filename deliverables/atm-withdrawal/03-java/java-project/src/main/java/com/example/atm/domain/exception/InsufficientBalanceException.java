package com.example.atm.domain.exception;

/**
 * 領域例外：帳戶餘額不足。
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
