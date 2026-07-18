package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private Account newAccount(long balance) {
        return new Account(AccountId.of("A"), Pin.of("1234"), Money.of(balance));
    }

    @Test
    void verifyPin_correct() {
        assertTrue(newAccount(5000).verifyPin(Pin.of("1234")));
    }

    @Test
    void verifyPin_wrong() {
        assertFalse(newAccount(5000).verifyPin(Pin.of("9999")));
    }

    @Test
    void withdraw_sufficient_deducts() {
        Account acc = newAccount(5000);
        acc.withdraw(Money.of(1000));
        assertEquals(4000, acc.balance().amount());
    }

    @Test
    void withdraw_exactBalance_toZero() {
        Account acc = newAccount(1000);
        acc.withdraw(Money.of(1000));
        assertEquals(0, acc.balance().amount());
    }

    @Test
    void withdraw_insufficient_throws() {
        Account acc = newAccount(500);
        assertThrows(InsufficientBalanceException.class, () -> acc.withdraw(Money.of(1000)));
        assertEquals(500, acc.balance().amount());
    }

    @Test
    void withdraw_zero_throws() {
        Account acc = newAccount(5000);
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(Money.ZERO));
    }

    @Test
    void constructor_nullArgs_throw() {
        assertThrows(NullPointerException.class,
                () -> new Account(null, Pin.of("1234"), Money.of(1)));
        assertThrows(NullPointerException.class,
                () -> new Account(AccountId.of("A"), null, Money.of(1)));
        assertThrows(NullPointerException.class,
                () -> new Account(AccountId.of("A"), Pin.of("1234"), null));
    }

    @Test
    void accountId_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> AccountId.of(" "));
        assertThrows(NullPointerException.class, () -> AccountId.of(null));
    }

    @Test
    void idAndBalance_getters() {
        Account acc = newAccount(5000);
        assertEquals("A", acc.id().value());
        assertEquals(5000, acc.balance().amount());
    }
}
