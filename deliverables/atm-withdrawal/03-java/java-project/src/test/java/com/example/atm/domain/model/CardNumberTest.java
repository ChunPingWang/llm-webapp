package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardNumberTest {

    @Test
    void 合法卡號建立成功() {
        assertEquals("FUBON-001", CardNumber.of("FUBON-001").value());
    }

    @Test
    void 空白卡號拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> CardNumber.of("  "));
    }

    @Test
    void 空值卡號拋出例外() {
        assertThrows(NullPointerException.class, () -> CardNumber.of(null));
    }
}
