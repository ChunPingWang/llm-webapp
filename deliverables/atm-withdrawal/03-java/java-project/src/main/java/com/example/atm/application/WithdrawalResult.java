package com.example.atm.application;

import com.example.atm.domain.model.Money;

/**
 * 提款結果 DTO。
 *
 * @param success       是否成功
 * @param dispensed     實際吐出的現金
 * @param remainingBalance 交易後帳戶餘額
 * @param message       結果訊息
 */
public record WithdrawalResult(boolean success, Money dispensed, Money remainingBalance, String message) {

    public static WithdrawalResult ok(Money dispensed, Money remainingBalance) {
        return new WithdrawalResult(true, dispensed, remainingBalance, "交易成功");
    }

    public static WithdrawalResult fail(String message, Money remainingBalance) {
        return new WithdrawalResult(false, Money.zero(), remainingBalance, message);
    }
}
