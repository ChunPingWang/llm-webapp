package com.example.atm.application;

import com.example.atm.domain.Money;

/**
 * 吐鈔設備介面，抽象化硬體，遵循 DIP 與 ISP。
 */
public interface CashDispenser {
    void dispense(Money amount);
}
