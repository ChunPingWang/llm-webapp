package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.AccountNotFoundException;
import com.example.atm.domain.AccountRepository;
import com.example.atm.domain.CardReader;
import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.InsufficientBalanceException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;

import java.util.Objects;

/**
 * ATM 會話應用服務（Application Service）。
 * 依賴抽象介面（DIP），協調領域物件完成提款使用案例。
 * 狀態機：IDLE -> CARD_INSERTED -> AUTHENTICATED -> ENDED。
 */
public class AtmSession {

    public enum State { IDLE, CARD_INSERTED, AUTHENTICATED, ENDED }

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;
    private final CardReader cardReader;

    private State state = State.IDLE;
    private Account currentAccount;
    private String lastMessage = "";

    public AtmSession(AccountRepository accountRepository,
                      CashDispenser cashDispenser,
                      CardReader cardReader) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository 不可為 null");
        this.cashDispenser = Objects.requireNonNull(cashDispenser, "cashDispenser 不可為 null");
        this.cardReader = Objects.requireNonNull(cardReader, "cardReader 不可為 null");
    }

    /** FR-01 插入提款卡 */
    public void insertCard(String accountId) {
        AccountId id = AccountId.of(accountId);
        this.currentAccount = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("查無此帳戶: " + accountId));
        this.state = State.CARD_INSERTED;
        this.lastMessage = "";
    }

    /** FR-02 密碼驗證（BR-01） */
    public AuthResult enterPin(String pin) {
        requireState(State.CARD_INSERTED, "請先插入提款卡");
        boolean ok = currentAccount.verifyPin(Pin.of(pin));
        if (ok) {
            this.state = State.AUTHENTICATED;
            this.lastMessage = "";
            return AuthResult.success();
        }
        this.lastMessage = "密碼錯誤";
        return AuthResult.failure(this.lastMessage);
    }

    /** FR-03 服務選單存取控制（BR-02） */
    public boolean canSelectService() {
        return this.state == State.AUTHENTICATED;
    }

    /** FR-04、FR-05、FR-06、FR-07 提款（BR-03、BR-04、BR-05） */
    public WithdrawalResult withdraw(long amount) {
        requireState(State.AUTHENTICATED, "尚未通過密碼驗證，無法選擇服務");
        try {
            Money requested = Money.of(amount);
            currentAccount.withdraw(requested);
            cashDispenser.dispense(requested);
            accountRepository.save(currentAccount);
            this.lastMessage = "提款成功";
            return WithdrawalResult.success(currentAccount.balance().amount());
        } catch (InsufficientBalanceException e) {
            this.lastMessage = "餘額不足";
            return WithdrawalResult.failure(currentAccount.balance().amount(), this.lastMessage);
        } catch (IllegalArgumentException e) {
            this.lastMessage = e.getMessage();
            return WithdrawalResult.failure(currentAccount.balance().amount(), this.lastMessage);
        }
    }

    /** FR-08 結束服務並退卡（BR-07） */
    public void endSession() {
        cardReader.eject();
        this.state = State.ENDED;
        this.currentAccount = null;
    }

    public boolean isCardEjected() {
        return this.state == State.ENDED && cardReader.isEjected();
    }

    public String lastMessage() {
        return lastMessage;
    }

    public State state() {
        return state;
    }

    private void requireState(State expected, String errorMessage) {
        if (this.state != expected) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
