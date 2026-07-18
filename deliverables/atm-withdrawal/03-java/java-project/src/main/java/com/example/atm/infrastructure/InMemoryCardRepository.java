package com.example.atm.infrastructure;

import com.example.atm.domain.Card;
import com.example.atm.domain.CardRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體版卡片儲存庫實作。
 */
public class InMemoryCardRepository implements CardRepository {

    private final Map<String, Card> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Card> findByCardNumber(String cardNumber) {
        return Optional.ofNullable(store.get(cardNumber));
    }

    @Override
    public void save(Card card) {
        store.put(card.getCardNumber(), card);
    }
}
