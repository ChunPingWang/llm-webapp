package com.example.atm.domain.repository;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;

import java.util.Optional;

/**
 * 儲存庫介面（DIP：領域定義抽象，基礎設施實作）。
 */
public interface AccountRepository {
    Optional<Account> findByCardNumber(CardNumber cardNumber);
    void save(Account account);
}
