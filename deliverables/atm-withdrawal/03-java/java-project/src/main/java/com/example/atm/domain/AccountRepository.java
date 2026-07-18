package com.example.atm.domain;

import java.util.Optional;

/**
 * 帳戶儲存庫介面（Repository），由基礎設施層實作（DIP）。
 */
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    void save(Account account);
}
