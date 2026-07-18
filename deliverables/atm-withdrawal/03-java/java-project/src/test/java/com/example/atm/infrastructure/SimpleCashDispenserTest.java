package com.example.atm.infrastructure;

import com.example.atm.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleCashDispenserTest {

    @Test
    void 初始未出鈔為零() {
        assertEquals(Money.zero(), new SimpleCashDispenser().lastDispensed());
    }

    @Test
    void 出鈔後記錄最後金額() {
        SimpleCashDispenser dispenser = new SimpleCashDispenser();
        dispenser.dispense(Money.of(1000));
        assertEquals(Money.of(1000), dispenser.lastDispensed());
    }
}
