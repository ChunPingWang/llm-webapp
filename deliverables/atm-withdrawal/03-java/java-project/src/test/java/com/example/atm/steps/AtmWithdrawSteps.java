package com.example.atm.steps;

import com.example.atm.application.WithdrawalResult;
import com.example.atm.application.WithdrawalService;
import com.example.atm.domain.exception.InsufficientBalanceException;
import com.example.atm.domain.exception.InvalidPinException;
import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cucumber step definitions（英文 annotation，繁中步驟文字）。
 */
public class AtmWithdrawSteps {

    private InMemoryAccountRepository accountRepository;
    private SimpleCashDispenser cashDispenser;
    private WithdrawalService service;

    private CardNumber cardNumber;
    private Pin storedPin;
    private long initialBalance;
    private long cashStock = 10000L;

    private boolean cardInserted;
    private boolean menuShown;
    private boolean cashDispensed;
    private String message;
    private String authResult;

    /** 依當前設定重建物件圖，確保各場景獨立。 */
    private void rebuild() {
        accountRepository = new InMemoryAccountRepository();
        cashDispenser = new SimpleCashDispenser(Money.of(cashStock));
        service = new WithdrawalService(accountRepository, cashDispenser);
        accountRepository.save(new Account(cardNumber, storedPin, Money.of(initialBalance)));
        cashDispensed = false;
        menuShown = false;
        message = null;
        authResult = null;
    }

    @Given("使用者 {string} 持有富邦銀行提款卡 {string}")
    public void 使用者持有提款卡(String user, String card) {
        this.cardNumber = CardNumber.of(card);
    }

    @And("該卡片綁定的密碼為 {string}")
    public void 卡片密碼為(String pin) {
        this.storedPin = Pin.of(pin);
    }

    @And("使用者 {string} 的帳戶餘額為 {long} 元")
    public void 帳戶餘額為(String user, long balance) {
        this.initialBalance = balance;
        rebuild();
    }

    @And("ATM 現金庫存為 {long} 元")
    public void 現金庫存為(long stock) {
        this.cashStock = stock;
        rebuild();
    }

    @Given("使用者 {string} 已插入提款卡")
    public void 已插入提款卡(String user) {
        this.cardInserted = true;
    }

    @When("使用者 {string} 輸入密碼 {string}")
    public void 輸入密碼(String user, String pin) {
        assertTrue(cardInserted, "尚未插卡");
        boolean ok = service.authenticate(cardNumber, Pin.of(pin));
        if (ok) {
            menuShown = true;
            authResult = "成功";
        } else {
            menuShown = false;
            message = "密碼錯誤";
            authResult = "失敗";
        }
    }

    @Then("系統應顯示服務選單")
    public void 顯示服務選單() {
        assertTrue(menuShown);
    }

    @Then("系統不應顯示服務選單")
    public void 不顯示服務選單() {
        assertFalse(menuShown);
    }

    @When("使用者 {string} 選擇提款服務並輸入金額 {long} 元")
    public void 選擇提款(String user, long amount) {
        try {
            WithdrawalResult result = service.withdraw(cardNumber, storedPin, Money.of(amount));
            cashDispensed = result.success();
            if (!result.success()) {
                message = result.message();
            }
        } catch (InsufficientBalanceException e) {
            message = "餘額不足";
            cashDispensed = false;
        } catch (InvalidPinException e) {
            message = "密碼錯誤";
            cashDispensed = false;
        }
    }

    @Then("ATM 應吐出 {long} 元現金")
    public void 吐出現金(long amount) {
        assertTrue(cashDispensed);
        assertEquals(cashStock - amount, cashDispenser.remaining().amount());
    }

    @Then("ATM 不應吐出現金")
    public void 不吐出現金() {
        assertFalse(cashDispensed);
    }

    @And("使用者 {string} 的帳戶餘額應為 {long} 元")
    public void 帳戶餘額應為(String user, long expected) {
        Account account = accountRepository.findByCardNumber(cardNumber).orElseThrow();
        assertEquals(expected, account.balance().amount());
    }

    @Then("系統應顯示 {string} 訊息")
    public void 顯示訊息(String expected) {
        assertEquals(expected, message);
    }

    @When("使用者 {string} 選擇結束服務")
    public void 結束服務(String user) {
        this.cardInserted = false;
    }

    @Then("ATM 應退出提款卡")
    public void 退出提款卡() {
        assertFalse(cardInserted);
    }

    @And("系統應回到待機畫面")
    public void 回到待機畫面() {
        assertFalse(cardInserted);
    }

    @Then("系統驗證結果應為 {string}")
    public void 驗證結果應為(String expected) {
        assertNotNull(authResult);
        assertEquals(expected, authResult);
    }
}
