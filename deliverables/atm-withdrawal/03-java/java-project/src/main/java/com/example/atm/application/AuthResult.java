package com.example.atm.application;

/**
 * 密碼驗證結果。
 */
public record AuthResult(boolean authenticated, String message) {

    public static AuthResult success() {
        return new AuthResult(true, "驗證通過");
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message);
    }
}
