package com.example.atm.infrastructure;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccountRepositoryTest {

    private final CardNumber card = CardNumber.of("1234-5678");

    @Test
    void 儲存後應可查得() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        repo.save(new Account(card, Pin.of("4321"), Money.of(5000)));
        assertTrue(repo.findByCardNumber(card).isPresent());
        assertEquals(5000, repo.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 查無卡片應回傳空() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        assertFalse(repo.findByCardNumber(CardNumber.of("9999-9999")).isPresent());
    }

    @Test
    void 傳入null卡號應回傳空() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        assertFalse(repo.findByCardNumber(null).isPresent());
    }
}
