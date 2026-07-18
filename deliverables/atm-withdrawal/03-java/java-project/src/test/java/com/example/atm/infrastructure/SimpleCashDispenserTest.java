package com.example.atm.infrastructure;

import com.example.atm.domain.exception.InsufficientCashException;
import com.example.atm.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCashDispenserTest {

    @Test
    void 庫存足夠應可出鈔並更新餘量() {
        SimpleCashDispenser dispenser = new SimpleCashDispenser(Money.of(10000));
        assertTrue(dispenser.hasEnoughCash(Money.of(1000)));
        dispenser.dispense(Money.of(1000));
        assertEquals(9000, dispenser.remaining().amount());
    }

    @Test
    void 庫存不足應擲例外() {
        SimpleCashDispenser dispenser = new SimpleCashDispenser(Money.of(500));
        assertFalse(dispenser.hasEnoughCash(Money.of(1000)));
        assertThrows(InsufficientCashException.class, () -> dispenser.dispense(Money.of(1000)));
    }

    @Test
    void 建構null應擲例外() {
        assertThrows(NullPointerException.class, () -> new SimpleCashDispenser(null));
    }
}
