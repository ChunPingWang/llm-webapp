package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void 建立負金額應拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void 金額大於等於比較() {
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(1000)));
        assertTrue(Money.of(1001).isGreaterThanOrEqual(Money.of(1000)));
        assertFalse(Money.of(999).isGreaterThanOrEqual(Money.of(1000)));
    }

    @Test
    void 金額扣除() {
        assertEquals(Money.of(4000), Money.of(5000).subtract(Money.of(1000)));
    }

    @Test
    void 扣除後為負應拋出例外() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(500).subtract(Money.of(1000)));
    }

    @Test
    void toString應包含元() {
        assertEquals("1000 元", Money.of(1000).toString());
    }
}
