package com.example.atm.infrastructure;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.repository.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基礎設施：記憶體版帳戶儲存庫。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findByCardNumber(CardNumber cardNumber) {
        if (cardNumber == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(cardNumber.value()));
    }

    @Override
    public void save(Account account) {
        store.put(account.cardNumber().value(), account);
    }
}
