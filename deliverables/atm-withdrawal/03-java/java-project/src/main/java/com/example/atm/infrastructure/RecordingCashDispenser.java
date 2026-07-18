package com.example.atm.infrastructure;

import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.Money;

/**
 * 記錄出鈔總額的出鈔裝置實作，供測試驗證出鈔一致性。
 */
public class RecordingCashDispenser implements CashDispenser {

    private long totalDispensed = 0;

    @Override
    public void dispense(Money money) {
        this.totalDispensed += money.amount();
    }

    public long totalDispensed() {
        return totalDispensed;
    }
}
