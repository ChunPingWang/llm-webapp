package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.Card;
import com.example.atm.domain.CardLockedException;
import com.example.atm.domain.Money;
import com.example.atm.infrastructure.InMemoryCardRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmServiceTest {

    private InMemoryCardRepository repository;
    private SimpleCashDispenser dispenser;
    private AtmService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCardRepository();
        dispenser = new SimpleCashDispenser();
        service = new AtmService(repository, dispenser);
    }

    private void givenCard(long balance) {
        Account account = new Account("ACC-A", Money.of(balance));
        repository.save(new Card("1234-5678", "8888", account));
    }

    @Test
    void 密碼正確驗證成功() {
        givenCard(5000);
        assertTrue(service.authenticate("1234-5678", "8888"));
    }

    @Test
    void 密碼錯誤驗證失敗() {
        givenCard(5000);
        assertFalse(service.authenticate("1234-5678", "0000"));
    }

    @Test
    void 餘額充足提款成功() {
        givenCard(5000);
        WithdrawalResult result = service.withdraw("1234-5678", 1000);
        assertTrue(result.success());
        assertEquals(4000, result.remainingBalance());
        assertEquals(1000, dispenser.getLastDispensedAmount());
    }

    @Test
    void 餘額不足提款失敗且不吐鈔() {
        givenCard(500);
        WithdrawalResult result = service.withdraw("1234-5678", 1000);
        assertFalse(result.success());
        assertEquals("餘額不足", result.message());
        assertEquals(500, result.remainingBalance());
        assertEquals(0, dispenser.getLastDispensedAmount());
    }

    @Test
    void 鎖卡後提款拋出例外() {
        givenCard(5000);
        service.authenticate("1234-5678", "0000");
        service.authenticate("1234-5678", "0000");
        service.authenticate("1234-5678", "0000");
        assertTrue(service.isCardLocked("1234-5678"));
        assertThrows(CardLockedException.class, () -> service.withdraw("1234-5678", 1000));
    }

    @Test
    void 查無此卡拋出例外() {
        assertThrows(IllegalArgumentException.class,
                () -> service.withdraw("9999", 1000));
    }
}
