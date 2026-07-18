package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void negativeAmount_throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void zeroConstant_isZero() {
        assertEquals(0, Money.ZERO.amount());
    }

    @Test
    void add_returnsSum() {
        assertEquals(3000, Money.of(1000).add(Money.of(2000)).amount());
    }

    @Test
    void subtract_returnsDifference() {
        assertEquals(500, Money.of(1500).subtract(Money.of(1000)).amount());
    }

    @Test
    void subtract_overBalance_throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(100).subtract(Money.of(200)));
    }

    @Test
    void isGreaterThanOrEqual_boundaryValues() {
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(1000)));
        assertTrue(Money.of(1001).isGreaterThanOrEqual(Money.of(1000)));
        assertFalse(Money.of(999).isGreaterThanOrEqual(Money.of(1000)));
    }

    @Test
    void isPositive() {
        assertTrue(Money.of(1).isPositive());
        assertFalse(Money.ZERO.isPositive());
    }

    @Test
    void nullArgument_throws() {
        assertThrows(NullPointerException.class, () -> Money.of(100).add(null));
        assertThrows(NullPointerException.class, () -> Money.of(100).subtract(null));
        assertThrows(NullPointerException.class, () -> Money.of(100).isGreaterThanOrEqual(null));
    }
}
