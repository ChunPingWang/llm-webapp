package com.example.atm.domain.exception;

/**
 * 密碼格式錯誤領域例外。
 */
public class InvalidPinFormatException extends RuntimeException {

    public InvalidPinFormatException(String message) {
        super(message);
    }
}
