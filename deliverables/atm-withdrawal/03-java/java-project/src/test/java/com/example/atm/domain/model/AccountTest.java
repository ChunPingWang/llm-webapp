package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private Account newAccount(long balance) {
        return new Account(CardNumber.of("CARD-1"), Pin.of("1234"), Money.of(balance));
    }

    @Test
    void 密碼正確時驗證成功() {
        assertTrue(newAccount(1000).verifyPin(Pin.of("1234")));
    }

    @Test
    void 密碼錯誤時驗證失敗() {
        assertFalse(newAccount(1000).verifyPin(Pin.of("9999")));
    }

    @Test
    void canWithdraw餘額充足回傳true() {
        assertTrue(newAccount(5000).canWithdraw(Money.of(1000)));
    }

    @Test
    void canWithdraw餘額不足回傳false() {
        assertFalse(newAccount(500).canWithdraw(Money.of(1000)));
    }

    @Test
    void canWithdraw零金額回傳false() {
        assertFalse(newAccount(5000).canWithdraw(Money.zero()));
    }

    @Test
    void 餘額充足時提款成功並扣款() {
        Account account = newAccount(5000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(4000), account.balance());
    }

    @Test
    void 提款至餘額為零成功() {
        Account account = newAccount(1000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.zero(), account.balance());
    }

    @Test
    void 餘額不足時提款拋出例外且不異動餘額() {
        Account account = newAccount(500);
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(Money.of(1000)));
        assertEquals(Money.of(500), account.balance());
    }

    @Test
    void 提款零金額拋出例外() {
        Account account = newAccount(5000);
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(Money.zero()));
    }

    @Test
    void 卡號可取得() {
        assertEquals(CardNumber.of("CARD-1"), newAccount(1000).cardNumber());
    }
}
