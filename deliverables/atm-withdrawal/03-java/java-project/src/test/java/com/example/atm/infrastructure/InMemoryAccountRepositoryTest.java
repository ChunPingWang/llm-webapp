package com.example.atm.infrastructure;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccountRepositoryTest {

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();

    @Test
    void 儲存後可依卡號查詢() {
        Account account = new Account(CardNumber.of("CARD-1"), Pin.of("1234"), Money.of(1000));
        repository.save(account);
        Optional<Account> found = repository.findByCardNumber(CardNumber.of("CARD-1"));
        assertTrue(found.isPresent());
        assertEquals(Money.of(1000), found.get().balance());
    }

    @Test
    void 查詢不存在卡號回傳空() {
        assertTrue(repository.findByCardNumber(CardNumber.of("NONE")).isEmpty());
    }

    @Test
    void 查詢null卡號回傳空() {
        assertTrue(repository.findByCardNumber(null).isEmpty());
    }
}
