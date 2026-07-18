package com.example.atm.domain;

/**
 * 餘額不足領域例外。
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
