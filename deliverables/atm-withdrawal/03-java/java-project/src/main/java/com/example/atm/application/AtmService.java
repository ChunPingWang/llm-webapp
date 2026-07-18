package com.example.atm.application;

import com.example.atm.domain.exception.InsufficientFundsException;
import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.domain.port.AccountRepository;
import com.example.atm.domain.port.CashDispenser;

import java.util.Objects;
import java.util.Optional;

/**
 * ATM 應用服務（Application Service）。
 * 協調領域物件完成提款用例，僅依賴抽象埠（DIP）。
 */
public class AtmService {

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;

    public AtmService(AccountRepository accountRepository, CashDispenser cashDispenser) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "儲存庫不可為 null");
        this.cashDispenser = Objects.requireNonNull(cashDispenser, "出鈔裝置不可為 null");
    }

    /**
     * 驗證密碼並回傳會話；密碼正確時會話標記為已驗證。
     */
    public AtmSession authenticate(CardNumber cardNumber, Pin pin) {
        AtmSession session = AtmSession.start(cardNumber);
        accountRepository.findByCardNumber(cardNumber)
                .filter(account -> account.verifyPin(pin))
                .ifPresent(account -> session.markAuthenticated());
        return session;
    }

    /**
     * 執行提款用例，回傳結果狀態。
     * 交易保證原子性：先扣款成功才出鈔並持久化。
     */
    public WithdrawalResult withdraw(CardNumber cardNumber, Pin pin, Money amount) {
        if (!amount.isPositive()) {
            return WithdrawalResult.金額無效;
        }

        Optional<Account> maybeAccount = accountRepository.findByCardNumber(cardNumber);
        if (maybeAccount.isEmpty()) {
            return WithdrawalResult.卡片無效;
        }

        Account account = maybeAccount.get();
        if (!account.verifyPin(pin)) {
            return WithdrawalResult.密碼錯誤;
        }

        try {
            account.withdraw(amount);
            cashDispenser.dispense(amount);
            accountRepository.save(account);
            return WithdrawalResult.成功;
        } catch (InsufficientFundsException e) {
            return WithdrawalResult.餘額不足;
        }
    }
}
