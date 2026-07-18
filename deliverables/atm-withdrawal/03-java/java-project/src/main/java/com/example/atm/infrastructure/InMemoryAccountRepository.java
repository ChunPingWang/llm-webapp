package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體版帳戶儲存庫，執行緒安全，供測試與示範使用。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Account account) {
        store.put(account.id(), account);
    }
}
