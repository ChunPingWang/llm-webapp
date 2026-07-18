package com.example.atm.domain.exception;

/**
 * 餘額不足領域例外。
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
