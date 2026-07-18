package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void 建立負數金額應擲例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void 零金額應可建立且非正數() {
        assertEquals(0, Money.zero().amount());
        assertFalse(Money.zero().isPositive());
    }

    @Test
    void 相加應正確() {
        assertEquals(1500, Money.of(1000).add(Money.of(500)).amount());
    }

    @Test
    void 相減應正確() {
        assertEquals(500, Money.of(1000).subtract(Money.of(500)).amount());
    }

    @Test
    void 大於等於比較應正確() {
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(1000)));
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(500)));
        assertFalse(Money.of(500).isGreaterThanOrEqual(Money.of(1000)));
    }

    @Test
    void 正數判斷應正確() {
        assertTrue(Money.of(1).isPositive());
    }
}
