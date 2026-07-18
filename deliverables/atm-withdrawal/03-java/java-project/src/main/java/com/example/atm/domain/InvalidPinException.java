package com.example.atm.domain;

/**
 * 密碼驗證失敗領域例外。
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }
}
