package com.example.atm.domain;

/**
 * 出鈔裝置介面，抽象化實體 ATM 硬體（DIP、ISP）。
 */
public interface CashDispenser {
    void dispense(Money money);
}
