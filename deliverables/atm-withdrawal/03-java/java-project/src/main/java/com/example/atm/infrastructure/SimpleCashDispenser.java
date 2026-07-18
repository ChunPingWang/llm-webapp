package com.example.atm.infrastructure;

import com.example.atm.domain.exception.InsufficientCashException;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.service.CashDispenser;

import java.util.Objects;

/**
 * 基礎設施：簡易現金匣，維護庫存。
 */
public class SimpleCashDispenser implements CashDispenser {

    private Money stock;

    public SimpleCashDispenser(Money initialStock) {
        this.stock = Objects.requireNonNull(initialStock, "初始庫存不可為 null");
    }

    @Override
    public boolean hasEnoughCash(Money amount) {
        return stock.isGreaterThanOrEqual(amount);
    }

    @Override
    public void dispense(Money amount) {
        if (!hasEnoughCash(amount)) {
            throw new InsufficientCashException("現金庫存不足");
        }
        this.stock = this.stock.subtract(amount);
    }

    @Override
    public Money remaining() {
        return stock;
    }
}
