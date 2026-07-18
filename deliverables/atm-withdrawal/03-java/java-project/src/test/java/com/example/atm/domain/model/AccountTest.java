package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private Account newAccount(long balance) {
        return new Account(CardNumber.of("1234-5678"), Pin.of("4321"), Money.of(balance));
    }

    @Test
    void 密碼正確應驗證通過() {
        assertTrue(newAccount(5000).verifyPin(Pin.of("4321")));
    }

    @Test
    void 密碼錯誤應驗證失敗() {
        assertFalse(newAccount(5000).verifyPin(Pin.of("0000")));
    }

    @Test
    void 餘額充足應可扣款() {
        Account account = newAccount(5000);
        account.withdraw(Money.of(1000));
        assertEquals(4000, account.balance().amount());
    }

    @Test
    void 餘額不足應擲例外且不扣款() {
        Account account = newAccount(500);
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(Money.of(1000)));
        assertEquals(500, account.balance().amount());
    }

    @Test
    void 提款金額為零應擲例外() {
        assertThrows(IllegalArgumentException.class, () -> newAccount(5000).withdraw(Money.zero()));
    }

    @Test
    void 建構時傳入null應擲例外() {
        assertThrows(NullPointerException.class,
                () -> new Account(null, Pin.of("4321"), Money.of(100)));
    }

    @Test
    void 回傳卡號正確() {
        assertEquals("1234-5678", newAccount(100).cardNumber().value());
    }
}
