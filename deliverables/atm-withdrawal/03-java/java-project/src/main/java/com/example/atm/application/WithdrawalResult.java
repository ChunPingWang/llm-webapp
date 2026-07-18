package com.example.atm.application;

/**
 * 提款結果值物件。
 */
public record WithdrawalResult(boolean success, String message, long remainingBalance) {

    public static WithdrawalResult success(long remainingBalance) {
        return new WithdrawalResult(true, "提款成功", remainingBalance);
    }

    public static WithdrawalResult failure(String message, long remainingBalance) {
        return new WithdrawalResult(false, message, remainingBalance);
    }
}
