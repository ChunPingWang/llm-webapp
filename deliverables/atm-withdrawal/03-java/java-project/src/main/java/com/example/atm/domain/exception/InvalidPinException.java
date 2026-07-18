package com.example.atm.domain.exception;

/**
 * 領域例外：密碼錯誤。
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }
}
