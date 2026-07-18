package com.example.atm.infrastructure;

import com.example.atm.application.CashDispenser;
import com.example.atm.domain.Money;

/**
 * 簡易吐鈔設備實作，記錄最後一次吐出的金額。
 */
public class SimpleCashDispenser implements CashDispenser {

    private long lastDispensedAmount = 0;

    @Override
    public void dispense(Money amount) {
        this.lastDispensedAmount = amount.amount();
    }

    public long getLastDispensedAmount() {
        return lastDispensedAmount;
    }
}
