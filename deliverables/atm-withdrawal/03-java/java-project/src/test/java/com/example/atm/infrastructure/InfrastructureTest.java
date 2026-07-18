package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureTest {

    @Test
    void repository_saveAndFind() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        Account acc = new Account(AccountId.of("A"), Pin.of("1234"), Money.of(5000));
        repo.save(acc);
        assertTrue(repo.findById(AccountId.of("A")).isPresent());
        assertFalse(repo.findById(AccountId.of("B")).isPresent());
    }

    @Test
    void dispenser_accumulates() {
        RecordingCashDispenser dispenser = new RecordingCashDispenser();
        dispenser.dispense(Money.of(1000));
        dispenser.dispense(Money.of(500));
        assertEquals(1500, dispenser.totalDispensed());
    }

    @Test
    void cardReader_ejectLifecycle() {
        SimpleCardReader reader = new SimpleCardReader();
        assertFalse(reader.isEjected());
        reader.eject();
        assertTrue(reader.isEjected());
    }
}
