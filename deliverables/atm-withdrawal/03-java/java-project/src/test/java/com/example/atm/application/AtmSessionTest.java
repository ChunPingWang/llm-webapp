package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.AccountNotFoundException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.RecordingCashDispenser;
import com.example.atm.infrastructure.SimpleCardReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmSessionTest {

    private InMemoryAccountRepository repository;
    private RecordingCashDispenser dispenser;
    private SimpleCardReader cardReader;
    private AtmSession session;

    @BeforeEach
    void init() {
        repository = new InMemoryAccountRepository();
        dispenser = new RecordingCashDispenser();
        cardReader = new SimpleCardReader();
        session = new AtmSession(repository, dispenser, cardReader);
        repository.save(new Account(AccountId.of("A"), Pin.of("1234"), Money.of(5000)));
    }

    @Test
    void happyPath_success() {
        session.insertCard("A");
        assertTrue(session.enterPin("1234").authenticated());
        assertTrue(session.canSelectService());

        WithdrawalResult result = session.withdraw(1000);
        assertTrue(result.success());
        assertEquals(4000, result.remainingBalance());
        assertEquals(1000, dispenser.totalDispensed());

        session.endSession();
        assertTrue(session.isCardEjected());
        assertEquals(AtmSession.State.ENDED, session.state());
    }

    @Test
    void wrongPin_cannotSelectService() {
        session.insertCard("A");
        AuthResult result = session.enterPin("9999");
        assertFalse(result.authenticated());
        assertFalse(session.canSelectService());
        assertEquals("密碼錯誤", session.lastMessage());
    }

    @Test
    void insufficientBalance_fails() {
        repository.save(new Account(AccountId.of("A"), Pin.of("1234"), Money.of(500)));
        session.insertCard("A");
        session.enterPin("1234");

        WithdrawalResult result = session.withdraw(1000);
        assertFalse(result.success());
        assertEquals("餘額不足", result.message());
        assertEquals(500, result.remainingBalance());
        assertEquals(0, dispenser.totalDispensed());
    }

    @Test
    void zeroAmount_fails() {
        session.insertCard("A");
        session.enterPin("1234");
        WithdrawalResult result = session.withdraw(0);
        assertFalse(result.success());
        assertEquals("提款金額必須大於 0", result.message());
    }

    @Test
    void withdrawBeforeAuth_throws() {
        session.insertCard("A");
        assertThrows(IllegalStateException.class, () -> session.withdraw(1000));
    }

    @Test
    void enterPinBeforeInsert_throws() {
        assertThrows(IllegalStateException.class, () -> session.enterPin("1234"));
    }

    @Test
    void insertUnknownAccount_throws() {
        assertThrows(AccountNotFoundException.class, () -> session.insertCard("Z"));
    }

    @Test
    void constructor_nullArgs_throw() {
        assertThrows(NullPointerException.class,
                () -> new AtmSession(null, dispenser, cardReader));
        assertThrows(NullPointerException.class,
                () -> new AtmSession(repository, null, cardReader));
        assertThrows(NullPointerException.class,
                () -> new AtmSession(repository, dispenser, null));
    }

    @Test
    void authResult_and_withdrawalResult_messages() {
        assertTrue(AuthResult.success().authenticated());
        assertFalse(AuthResult.failure("x").authenticated());
        assertTrue(WithdrawalResult.success(100).success());
        assertFalse(WithdrawalResult.failure(100, "x").success());
    }
}
