package com.example.atm.domain.port;

import com.example.atm.domain.model.Money;

/**
 * 出鈔裝置埠（Port）。
 * 抽象化硬體行為，符合 DIP。
 */
public interface CashDispenser {

    void dispense(Money amount);

    Money lastDispensed();
}
