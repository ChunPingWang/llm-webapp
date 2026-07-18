package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    @Test
    void 餘額充足可提款() {
        Account account = new Account("ACC-1", Money.of(5000));
        assertTrue(account.canWithdraw(Money.of(1000)));
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(4000), account.getBalance());
    }

    @Test
    void 餘額不足不可提款() {
        Account account = new Account("ACC-1", Money.of(500));
        assertFalse(account.canWithdraw(Money.of(1000)));
        assertThrows(InsufficientBalanceException.class,
                () -> account.withdraw(Money.of(1000)));
        assertEquals(Money.of(500), account.getBalance());
    }

    @Test
    void 取得帳號() {
        Account account = new Account("ACC-1", Money.of(0));
        assertEquals("ACC-1", account.getAccountId());
    }
}
