package com.example.atm.application;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmServiceTest {

    private static final CardNumber CARD = CardNumber.of("CARD-1");
    private static final Pin CORRECT_PIN = Pin.of("1234");
    private static final Pin WRONG_PIN = Pin.of("0000");

    private InMemoryAccountRepository repository;
    private SimpleCashDispenser dispenser;
    private AtmService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        dispenser = new SimpleCashDispenser();
        service = new AtmService(repository, dispenser);
    }

    private void givenAccount(long balance) {
        repository.save(new Account(CARD, CORRECT_PIN, Money.of(balance)));
    }

    @Test
    void 密碼正確驗證會話通過() {
        givenAccount(5000);
        AtmSession session = service.authenticate(CARD, CORRECT_PIN);
        assertTrue(session.canSelectService());
    }

    @Test
    void 密碼錯誤驗證會話失敗() {
        givenAccount(5000);
        AtmSession session = service.authenticate(CARD, WRONG_PIN);
        assertFalse(session.canSelectService());
    }

    @Test
    void 卡片不存在驗證會話失敗() {
        AtmSession session = service.authenticate(CARD, CORRECT_PIN);
        assertFalse(session.canSelectService());
    }

    @Test
    void 餘額充足提款成功() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.成功, result);
        assertEquals(Money.of(1000), dispenser.lastDispensed());
        assertEquals(Money.of(4000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 密碼錯誤提款失敗且不出鈔不扣款() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, WRONG_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.密碼錯誤, result);
        assertEquals(Money.zero(), dispenser.lastDispensed());
        assertEquals(Money.of(5000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 餘額不足提款失敗且不出鈔不扣款() {
        givenAccount(500);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.餘額不足, result);
        assertEquals(Money.zero(), dispenser.lastDispensed());
        assertEquals(Money.of(500), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 提款零金額回傳金額無效() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.zero());
        assertEquals(WithdrawalResult.金額無效, result);
    }

    @Test
    void 卡片無效提款失敗() {
        WithdrawalResult result = service.withdraw(CardNumber.of("UNKNOWN"), CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.卡片無效, result);
    }

    @Test
    void 提款至餘額為零成功() {
        givenAccount(1000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.成功, result);
        assertEquals(Money.zero(), repository.findByCardNumber(CARD).orElseThrow().balance());
    }
}
