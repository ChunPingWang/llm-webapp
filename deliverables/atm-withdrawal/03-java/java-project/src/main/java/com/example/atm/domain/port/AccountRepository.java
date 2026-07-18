package com.example.atm.domain.port;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;

import java.util.Optional;

/**
 * 帳戶儲存庫埠（Port）。
 * 由 infrastructure 實作，符合 DIP 與 ISP。
 */
public interface AccountRepository {

    Optional<Account> findByCardNumber(CardNumber cardNumber);

    void save(Account account);
}
