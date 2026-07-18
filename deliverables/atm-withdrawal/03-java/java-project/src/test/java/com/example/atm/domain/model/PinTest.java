package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinTest {

    @Test
    void 合法密碼應可建立() {
        assertTrue(Pin.of("4321").matches(Pin.of("4321")));
    }

    @Test
    void 非四位數字應擲例外() {
        assertThrows(IllegalArgumentException.class, () -> Pin.of("123"));
        assertThrows(IllegalArgumentException.class, () -> Pin.of("abcd"));
        assertThrows(IllegalArgumentException.class, () -> Pin.of("12345"));
    }

    @Test
    void 空值應擲例外() {
        assertThrows(NullPointerException.class, () -> Pin.of(null));
    }

    @Test
    void 不同密碼不相符() {
        assertFalse(Pin.of("1111").matches(Pin.of("2222")));
        assertFalse(Pin.of("1111").matches(null));
    }
}
