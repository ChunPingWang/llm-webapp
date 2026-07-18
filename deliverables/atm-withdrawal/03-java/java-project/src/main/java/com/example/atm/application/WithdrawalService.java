package com.example.atm.application;

import com.example.atm.domain.exception.AccountNotFoundException;
import com.example.atm.domain.exception.InvalidPinException;
import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.domain.repository.AccountRepository;
import com.example.atm.domain.service.CashDispenser;

import java.util.Objects;

/**
 * 應用服務：協調提款流程（驗證密碼 → 檢查現金 → 扣款 → 出鈔）。
 * SRP：僅負責流程協調；業務規則委派給領域物件。
 */
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;

    public WithdrawalService(AccountRepository accountRepository, CashDispenser cashDispenser) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository 不可為 null");
        this.cashDispenser = Objects.requireNonNull(cashDispenser, "cashDispenser 不可為 null");
    }

    /** 驗證密碼是否正確。 */
    public boolean authenticate(CardNumber cardNumber, Pin pin) {
        return loadAccount(cardNumber).verifyPin(pin);
    }

    /**
     * 執行提款。BR-05：先扣款成功後才吐鈔，確保一致性。
     */
    public WithdrawalResult withdraw(CardNumber cardNumber, Pin pin, Money amount) {
        Account account = loadAccount(cardNumber);

        if (!account.verifyPin(pin)) {
            throw new InvalidPinException("密碼錯誤");
        }
        if (!cashDispenser.hasEnoughCash(amount)) {
            return WithdrawalResult.fail("現金庫存不足", account.balance());
        }

        account.withdraw(amount);          // 餘額不足會擲 InsufficientBalanceException
        cashDispenser.dispense(amount);    // 出鈔
        accountRepository.save(account);   // 持久化

        return WithdrawalResult.ok(amount, account.balance());
    }

    private Account loadAccount(CardNumber cardNumber) {
        return accountRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new AccountNotFoundException("查無此卡片: " + cardNumber.value()));
    }
}
