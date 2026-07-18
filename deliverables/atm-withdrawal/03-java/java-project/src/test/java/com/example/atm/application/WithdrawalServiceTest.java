package com.example.atm.application;

import com.example.atm.domain.exception.AccountNotFoundException;
import com.example.atm.domain.exception.InsufficientBalanceException;
import com.example.atm.domain.exception.InvalidPinException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalServiceTest {

    private final CardNumber card = CardNumber.of("1234-5678");
    private final Pin pin = Pin.of("4321");
    private InMemoryAccountRepository repository;
    private WithdrawalService service;

    private WithdrawalService buildService(long balance, long stock) {
        repository = new InMemoryAccountRepository();
        repository.save(new Account(card, pin, Money.of(balance)));
        return new WithdrawalService(repository, new SimpleCashDispenser(Money.of(stock)));
    }

    @BeforeEach
    void setUp() {
        service = buildService(5000, 10000);
    }

    @Test
    void 密碼正確可通過驗證() {
        assertTrue(service.authenticate(card, pin));
    }

    @Test
    void 密碼錯誤驗證失敗() {
        assertFalse(service.authenticate(card, Pin.of("0000")));
    }

    @Test
    void 成功提款應扣款並回傳成功結果() {
        WithdrawalResult result = service.withdraw(card, pin, Money.of(1000));
        assertTrue(result.success());
        assertEquals(1000, result.dispensed().amount());
        assertEquals(4000, result.remainingBalance().amount());
        assertEquals(4000, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 密碼錯誤提款應擲例外() {
        assertThrows(InvalidPinException.class,
                () -> service.withdraw(card, Pin.of("0000"), Money.of(1000)));
    }

    @Test
    void 餘額不足提款應擲例外且不扣款() {
        service = buildService(500, 10000);
        assertThrows(InsufficientBalanceException.class,
                () -> service.withdraw(card, pin, Money.of(1000)));
        assertEquals(500, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 現金不足提款應回傳失敗且不扣款() {
        service = buildService(5000, 500);
        WithdrawalResult result = service.withdraw(card, pin, Money.of(1000));
        assertFalse(result.success());
        assertEquals("現金庫存不足", result.message());
        assertEquals(5000, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 查無卡片驗證應擲例外() {
        assertThrows(AccountNotFoundException.class,
                () -> service.authenticate(CardNumber.of("0000-0000"), pin));
    }

    @Test
    void 建構傳入null應擲例外() {
        assertThrows(NullPointerException.class,
                () -> new WithdrawalService(null, new SimpleCashDispenser(Money.of(100))));
    }
}
