package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void 建立負數金額拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void zero工廠回傳零金額() {
        assertEquals(Money.of(0), Money.zero());
    }

    @Test
    void 金額大於等於比較正確() {
        assertTrue(Money.of(1000).isGreaterThanOrEqualTo(Money.of(1000)));
        assertTrue(Money.of(2000).isGreaterThanOrEqualTo(Money.of(1000)));
        assertFalse(Money.of(500).isGreaterThanOrEqualTo(Money.of(1000)));
    }

    @Test
    void isPositive判斷正確() {
        assertTrue(Money.of(1).isPositive());
        assertFalse(Money.zero().isPositive());
    }

    @Test
    void 金額相減正確() {
        assertEquals(Money.of(4000), Money.of(5000).subtract(Money.of(1000)));
    }

    @Test
    void 金額相加正確() {
        assertEquals(Money.of(6000), Money.of(5000).add(Money.of(1000)));
    }

    @Test
    void 相減為負拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(500).subtract(Money.of(1000)));
    }
}
