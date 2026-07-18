package com.example.atm.steps;

import com.example.atm.application.AtmSession;
import com.example.atm.application.AuthResult;
import com.example.atm.application.WithdrawalResult;
import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.RecordingCashDispenser;
import com.example.atm.infrastructure.SimpleCardReader;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 對應繁體中文 feature 的 step definitions（使用英文 annotation）。
 */
public class AtmWithdrawalSteps {

    private InMemoryAccountRepository repository;
    private RecordingCashDispenser dispenser;
    private SimpleCardReader cardReader;
    private AtmSession session;
    private AuthResult authResult;
    private WithdrawalResult withdrawalResult;

    @Before
    public void setup() {
        repository = new InMemoryAccountRepository();
        dispenser = new RecordingCashDispenser();
        cardReader = new SimpleCardReader();
        session = new AtmSession(repository, dispenser, cardReader);
        authResult = null;
        withdrawalResult = null;
    }

    @Given("富邦銀行 ATM 已就緒")
    public void atmReady() {
        assertNotNull(session);
    }

    @Given("使用者 {string} 持有提款卡，密碼為 {string}，帳戶餘額為 {int} 元")
    public void ownerHasCard(String accountId, String pin, int balance) {
        repository.save(new Account(AccountId.of(accountId), Pin.of(pin), Money.of(balance)));
    }

    @Given("使用者 {string} 帳戶餘額為 {int} 元")
    public void resetBalance(String accountId, int balance) {
        repository.save(new Account(AccountId.of(accountId), Pin.of("1234"), Money.of(balance)));
        // 本步驟於插卡後設定餘額前置條件,需重新載入卡片使 session 反映最新餘額
        session.insertCard(accountId);
    }

    @Given("使用者 {string} 插入提款卡")
    public void insertCard(String accountId) {
        session.insertCard(accountId);
    }

    @When("使用者 {string} 輸入密碼 {string}")
    public void enterPin(String accountId, String pin) {
        authResult = session.enterPin(pin);
    }

    @Then("密碼驗證通過")
    public void pinAccepted() {
        assertTrue(authResult.authenticated());
        assertTrue(session.canSelectService());
    }

    @Then("密碼驗證失敗")
    public void pinRejected() {
        assertFalse(authResult.authenticated());
    }

    @And("使用者 {string} 可以在畫面上選擇服務")
    public void canSelectService(String accountId) {
        assertTrue(session.canSelectService());
    }

    @Then("使用者 {string} 無法在畫面上選擇服務")
    public void cannotSelectService(String accountId) {
        assertFalse(session.canSelectService());
    }

    @When("使用者 {string} 選擇提款 {int} 元")
    public void selectWithdraw(String accountId, int amount) {
        withdrawalResult = session.withdraw(amount);
    }

    @Then("提款機提供 {int} 元現金")
    public void dispensed(int amount) {
        assertTrue(withdrawalResult.success());
        assertEquals(amount, dispenser.totalDispensed());
    }

    @Then("提款機不提供現金")
    public void notDispensed() {
        assertFalse(withdrawalResult.success());
        assertEquals(0, dispenser.totalDispensed());
    }

    @And("使用者 {string} 帳戶餘額應為 {int} 元")
    public void balanceShouldBe(String accountId, int expected) {
        long actual = repository.findById(AccountId.of(accountId)).orElseThrow().balance().amount();
        assertEquals(expected, actual);
    }

    @And("畫面應顯示錯誤訊息 {string}")
    public void screenShows(String message) {
        assertEquals(message, session.lastMessage());
    }

    @When("使用者 {string} 選擇結束服務")
    public void endService(String accountId) {
        session.endSession();
    }

    @Then("提款卡應被退出")
    public void cardEjected() {
        assertTrue(session.isCardEjected());
    }

    @Then("提款結果應為 {string}")
    public void resultShouldBe(String expected) {
        boolean expectSuccess = "成功".equals(expected);
        assertEquals(expectSuccess, withdrawalResult.success());
    }
}
