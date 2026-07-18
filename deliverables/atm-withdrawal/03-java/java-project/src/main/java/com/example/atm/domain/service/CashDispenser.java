package com.example.atm.domain.service;

import com.example.atm.domain.model.Money;

/**
 * 現金匣抽象：負責出鈔與庫存管理。
 */
public interface CashDispenser {
    boolean hasEnoughCash(Money amount);
    void dispense(Money amount);
    Money remaining();
}
