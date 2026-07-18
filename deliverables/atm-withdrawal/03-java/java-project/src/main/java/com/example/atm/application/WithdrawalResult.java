package com.example.atm.application;

/**
 * 提款結果，供應用層對外回傳。
 */
public record WithdrawalResult(boolean success, long remainingBalance, String message) {

    public static WithdrawalResult success(long remainingBalance) {
        return new WithdrawalResult(true, remainingBalance, "提款成功");
    }

    public static WithdrawalResult failure(long remainingBalance, String message) {
        return new WithdrawalResult(false, remainingBalance, message);
    }
}
