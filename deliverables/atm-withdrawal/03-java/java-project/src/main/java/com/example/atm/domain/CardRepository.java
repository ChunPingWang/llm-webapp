package com.example.atm.domain;

import java.util.Optional;

/**
 * 提款卡儲存庫介面（Repository），由 infrastructure 層實作，遵循 DIP。
 */
public interface CardRepository {
    Optional<Card> findByCardNumber(String cardNumber);
    void save(Card card);
}
