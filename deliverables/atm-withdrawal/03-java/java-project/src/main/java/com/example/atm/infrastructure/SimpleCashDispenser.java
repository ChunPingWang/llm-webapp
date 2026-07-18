package com.example.atm.infrastructure;

import com.example.atm.domain.model.Money;
import com.example.atm.domain.port.CashDispenser;

/**
 * 簡易出鈔裝置實作（Adapter）。
 */
public class SimpleCashDispenser implements CashDispenser {

    private Money lastDispensed = Money.zero();

    @Override
    public void dispense(Money amount) {
        this.lastDispensed = amount;
    }

    @Override
    public Money lastDispensed() {
        return lastDispensed;
    }
}
