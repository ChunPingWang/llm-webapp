package com.example.atm.domain.exception;

/**
 * 領域例外：ATM 現金庫存不足。
 */
public class InsufficientCashException extends RuntimeException {
    public InsufficientCashException(String message) {
        super(message);
    }
}
