package com.example.atm.domain.model;

import com.example.atm.domain.exception.InvalidPinFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinTest {

    @Test
    void 合法四位數密碼建立成功() {
        assertTrue(Pin.of("1234").matches(Pin.of("1234")));
    }

    @Test
    void 密碼不符時比對失敗() {
        assertFalse(Pin.of("1234").matches(Pin.of("9999")));
    }

    @Test
    void 非四位數密碼拋出例外() {
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("123"));
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("12345"));
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("abcd"));
    }

    @Test
    void 空值密碼拋出例外() {
        assertThrows(NullPointerException.class, () -> Pin.of(null));
    }
}
