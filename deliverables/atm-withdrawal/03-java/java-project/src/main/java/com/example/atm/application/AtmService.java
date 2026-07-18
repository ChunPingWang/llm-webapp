package com.example.atm.application;

import com.example.atm.domain.Card;
import com.example.atm.domain.CardLockedException;
import com.example.atm.domain.CardRepository;
import com.example.atm.domain.InsufficientBalanceException;
import com.example.atm.domain.Money;

import java.util.Objects;

/**
 * ATM 應用服務，協調卡片驗證、提款與吐鈔的用例流程。
 */
public class AtmService {

    private final CardRepository cardRepository;
    private final CashDispenser cashDispenser;

    public AtmService(CardRepository cardRepository, CashDispenser cashDispenser) {
        this.cardRepository = Objects.requireNonNull(cardRepository);
        this.cashDispenser = Objects.requireNonNull(cashDispenser);
    }

    /**
     * 驗證密碼。
     *
     * @return true 表示密碼正確，可進入服務選單
     */
    public boolean authenticate(String cardNumber, String pin) {
        Card card = loadCard(cardNumber);
        boolean valid = card.verifyPin(pin);
        cardRepository.save(card);
        return valid;
    }

    /**
     * 執行提款用例。
     */
    public WithdrawalResult withdraw(String cardNumber, long amount) {
        Card card = loadCard(cardNumber);
        if (card.isLocked()) {
            throw new CardLockedException("提款卡已被鎖定，無法交易");
        }
        Money withdrawAmount = Money.of(amount);
        var account = card.getAccount();
        try {
            account.withdraw(withdrawAmount);
        } catch (InsufficientBalanceException e) {
            return WithdrawalResult.failure("餘額不足", account.getBalance().amount());
        }
        cashDispenser.dispense(withdrawAmount);
        cardRepository.save(card);
        return WithdrawalResult.success(account.getBalance().amount());
    }

    public boolean isCardLocked(String cardNumber) {
        return loadCard(cardNumber).isLocked();
    }

    private Card loadCard(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("查無此卡: " + cardNumber));
    }
}
