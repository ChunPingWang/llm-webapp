package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardNumberTest {

    @Test
    void 合法卡號應可建立() {
        assertEquals("1234-5678", CardNumber.of("1234-5678").value());
    }

    @Test
    void 空白卡號應擲例外() {
        assertThrows(IllegalArgumentException.class, () -> CardNumber.of("  "));
    }

    @Test
    void null卡號應擲例外() {
        assertThrows(NullPointerException.class, () -> CardNumber.of(null));
    }
}
