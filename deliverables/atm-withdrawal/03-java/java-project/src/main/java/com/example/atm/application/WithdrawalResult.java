package com.example.atm.application;

/**
 * 提款結果狀態列舉。
 */
public enum WithdrawalResult {
    成功,
    密碼錯誤,
    餘額不足,
    卡片無效,
    金額無效
}
