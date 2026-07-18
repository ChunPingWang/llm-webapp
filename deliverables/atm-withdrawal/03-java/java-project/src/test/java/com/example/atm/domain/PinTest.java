package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinTest {

    @Test
    void validFourDigits_ok() {
        assertTrue(Pin.of("1234").matches(Pin.of("1234")));
    }

    @Test
    void differentPin_notMatch() {
        assertFalse(Pin.of("1234").matches(Pin.of("9999")));
    }

    @Test
    void nonNumeric_throws() {
        assertThrows(IllegalArgumentException.class, () -> Pin.of("abcd"));
    }

    @Test
    void wrongLength_throws() {
        assertThrows(IllegalArgumentException.class, () -> Pin.of("123"));
        assertThrows(IllegalArgumentException.class, () -> Pin.of("12345"));
    }

    @Test
    void nullValue_throws() {
        assertThrows(NullPointerException.class, () -> Pin.of(null));
    }
}
