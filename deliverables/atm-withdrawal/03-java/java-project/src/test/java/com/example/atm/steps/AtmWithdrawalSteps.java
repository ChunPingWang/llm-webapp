package com.example.atm.steps;

import com.example.atm.application.AtmService;
import com.example.atm.application.AtmSession;
import com.example.atm.application.WithdrawalResult;
import com.example.atm.domain.exception.InvalidPinFormatException;
import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.domain.port.AccountRepository;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;

import io.cucumber.java.zh_tw.假設;
import io.cucumber.java.zh_tw.當;
import io.cucumber.java.zh_tw.那麼;
import io.cucumber.java.zh_tw.而且;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cucumber 步驟定義，串接 .feature 與應用服務。
 */
public class AtmWithdrawalSteps {

    private static final CardNumber CARD = CardNumber.of("FUBON-001");

    private final AccountRepository repository = new InMemoryAccountRepository();
    private final SimpleCashDispenser dispenser = new SimpleCashDispenser();
    private final AtmService atmService = new AtmService(repository, dispenser);

    private Pin registeredPin;
    private AtmSession session;
    private String lastMessage;
    private WithdrawalResult lastResult;

    @假設("使用者 {string} 持有富邦銀行提款卡")
    public void 使用者持卡(String user) {
        // 卡片持有前置條件，使用固定卡號代表使用者 A
    }

    @而且("該卡片綁定的帳戶密碼為 {string}")
    public void 綁定密碼(String pin) {
        this.registeredPin = Pin.of(pin);
    }

    @假設("使用者 {string} 的帳戶餘額為 {long} 元")
    public void 設定餘額(String user, long balance) {
        repository.save(new Account(CARD, registeredPin, Money.of(balance)));
    }

    @當("使用者 {string} 插入提款卡")
    public void 插入提款卡(String user) {
        this.session = AtmSession.start(CARD);
        this.lastResult = null;
        this.lastMessage = null;
    }

    @而且("使用者 {string} 輸入密碼 {string}")
    public void 輸入密碼(String user, String pin) {
        try {
            this.session = atmService.authenticate(CARD, Pin.of(pin));
            if (!session.canSelectService()) {
                this.lastMessage = "密碼錯誤";
            }
        } catch (InvalidPinFormatException e) {
            this.lastMessage = "密碼錯誤";
        }
    }

    @那麼("系統應允許使用者選擇服務")
    public void 允許選擇服務() {
        assertTrue(session.canSelectService(), "系統應允許選擇服務");
    }

    @那麼("系統應顯示 {string} 訊息")
    public void 顯示訊息(String message) {
        assertEquals(message, lastMessage);
    }

    @而且("系統不應允許使用者選擇服務")
    public void 不允許選擇服務() {
        assertFalse(session.canSelectService(), "系統不應允許選擇服務");
    }

    @當("使用者 {string} 選擇提款 {long} 元")
    public void 選擇提款(String user, long amount) {
        this.lastResult = atmService.withdraw(CARD, registeredPin, Money.of(amount));
        recordMessage();
    }

    @當("使用者 {string} 嘗試提款 {long} 元")
    public void 嘗試提款(String user, long amount) {
        if (session == null || !session.canSelectService()) {
            this.lastResult = WithdrawalResult.密碼錯誤;
        } else {
            this.lastResult = atmService.withdraw(CARD, registeredPin, Money.of(amount));
        }
        recordMessage();
    }

    @那麼("提款機應提供 {long} 元現金")
    public void 提供現金(long amount) {
        assertEquals(WithdrawalResult.成功, lastResult);
        assertEquals(Money.of(amount), dispenser.lastDispensed());
    }

    @而且("提款機不應提供現金")
    public void 不提供現金() {
        assertEquals(Money.zero(), dispenser.lastDispensed());
    }

    @而且("使用者 {string} 的帳戶餘額應為 {long} 元")
    public void 驗證餘額(String user, long expected) {
        Money balance = repository.findByCardNumber(CARD).orElseThrow().balance();
        assertEquals(Money.of(expected), balance);
    }

    @那麼("提款結果應為 {string}")
    public void 驗證結果(String expected) {
        assertEquals(WithdrawalResult.valueOf(expected), lastResult);
    }

    @當("使用者 {string} 選擇結束服務")
    public void 結束服務(String user) {
        this.session.end();
    }

    @那麼("系統應退出提款卡")
    public void 退出提款卡() {
        assertFalse(session.isActive(), "服務已結束，卡片應退出");
    }

    private void recordMessage() {
        this.lastMessage = switch (lastResult) {
            case 成功 -> "提款成功";
            case 密碼錯誤 -> "密碼錯誤";
            case 餘額不足 -> "餘額不足";
            case 卡片無效 -> "卡片無效";
            case 金額無效 -> "金額無效";
        };
    }
}
