package com.example.atm.bdd;

import com.example.atm.application.AtmService;
import com.example.atm.application.WithdrawalResult;
import com.example.atm.domain.Account;
import com.example.atm.domain.Card;
import com.example.atm.domain.CardRepository;
import com.example.atm.domain.Money;
import com.example.atm.infrastructure.InMemoryCardRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AtmStepDefinitions {

    private static final String CARD_NUMBER = "1234-5678";
    private static final String CORRECT_PIN = "8888";

    private CardRepository cardRepository;
    private SimpleCashDispenser cashDispenser;
    private AtmService atmService;

    private boolean cardInserted;
    private boolean pinAccepted;
    private WithdrawalResult withdrawalResult;

    @Before
    public void setUp() {
        cardRepository = new InMemoryCardRepository();
        cashDispenser = new SimpleCashDispenser();
        atmService = new AtmService(cardRepository, cashDispenser);
        cardInserted = false;
        pinAccepted = false;
        withdrawalResult = null;
    }

    @Given("使用者 A 持有富邦銀行的提款卡")
    public void 使用者_A_持有富邦銀行的提款卡() {
        Account account = new Account("ACC-A", Money.of(0));
        Card card = new Card(CARD_NUMBER, CORRECT_PIN, account);
        cardRepository.save(card);
    }

    @And("使用者 A 到富邦銀行的 ATM 插入提款卡")
    public void 使用者_A_到富邦銀行的_ATM_插入提款卡() {
        cardInserted = true;
    }

    @Given("使用者 A 的帳戶餘額為 {long} 元")
    public void 使用者_A_的帳戶餘額為_元(long balance) {
        Account account = new Account("ACC-A", Money.of(balance));
        Card card = new Card(CARD_NUMBER, CORRECT_PIN, account);
        cardRepository.save(card);
    }

    @When("使用者 A 輸入正確的密碼")
    public void 使用者_A_輸入正確的密碼() {
        assertTrue(cardInserted, "尚未插卡");
        pinAccepted = atmService.authenticate(CARD_NUMBER, CORRECT_PIN);
    }

    @When("使用者 A 輸入錯誤的密碼")
    public void 使用者_A_輸入錯誤的密碼() {
        assertTrue(cardInserted, "尚未插卡");
        pinAccepted = atmService.authenticate(CARD_NUMBER, "0000");
    }

    @When("使用者 A 連續 {int} 次輸入錯誤的密碼")
    public void 使用者_A_連續_次輸入錯誤的密碼(int times) {
        assertTrue(cardInserted, "尚未插卡");
        for (int i = 0; i < times; i++) {
            pinAccepted = atmService.authenticate(CARD_NUMBER, "0000");
        }
    }

    @Then("畫面上顯示可以選擇服務的選單")
    public void 畫面上顯示可以選擇服務的選單() {
        assertTrue(pinAccepted, "密碼驗證未通過，不應顯示選單");
    }

    @Then("畫面上顯示密碼錯誤的訊息")
    public void 畫面上顯示密碼錯誤的訊息() {
        assertFalse(pinAccepted, "密碼應為錯誤");
    }

    @And("畫面上不顯示服務選單")
    public void 畫面上不顯示服務選單() {
        assertFalse(pinAccepted, "密碼錯誤時不應顯示選單");
    }

    @And("使用者 A 無法選擇任何服務")
    public void 使用者_A_無法選擇任何服務() {
        assertFalse(pinAccepted);
    }

    @Then("畫面上顯示提款卡已被鎖定的訊息")
    public void 畫面上顯示提款卡已被鎖定的訊息() {
        assertTrue(atmService.isCardLocked(CARD_NUMBER), "提款卡應被鎖定");
    }

    @And("提款機保留提款卡不予退出")
    public void 提款機保留提款卡不予退出() {
        assertTrue(atmService.isCardLocked(CARD_NUMBER));
    }

    @When("使用者 A 選擇提款 {long} 元")
    public void 使用者_A_選擇提款_元(long amount) {
        withdrawalResult = atmService.withdraw(CARD_NUMBER, amount);
    }

    @Then("提款機提供 {long} 元現金")
    public void 提款機提供_元現金(long amount) {
        assertTrue(withdrawalResult.success(), "提款應成功");
        assertEquals(amount, cashDispenser.getLastDispensedAmount());
    }

    @And("使用者 A 帳戶餘額扣除 {long} 元後為 {long} 元")
    public void 使用者_A_帳戶餘額扣除_元後為_元(long deducted, long remaining) {
        assertEquals(remaining, withdrawalResult.remainingBalance());
    }

    @Then("提款機顯示餘額不足的訊息")
    public void 提款機顯示餘額不足的訊息() {
        assertFalse(withdrawalResult.success());
        assertEquals("餘額不足", withdrawalResult.message());
    }

    @And("提款機不提供任何現金")
    public void 提款機不提供任何現金() {
        assertEquals(0, cashDispenser.getLastDispensedAmount());
    }

    @And("使用者 A 帳戶餘額維持 {long} 元")
    public void 使用者_A_帳戶餘額維持_元(long balance) {
        assertEquals(balance, withdrawalResult.remainingBalance());
    }

    @When("使用者 A 選擇結束服務")
    public void 使用者_A_選擇結束服務() {
        // 結束服務不涉及狀態變更，僅表示流程結束
    }

    @Then("提款機退出提款卡")
    public void 提款機退出提款卡() {
        assertFalse(atmService.isCardLocked(CARD_NUMBER), "正常交易後應退卡");
    }

    @Then("提款機的處理結果為 {string}")
    public void 提款機的處理結果為(String expected) {
        assertEquals(expected, withdrawalResult.message());
    }

    @And("使用者 A 帳戶餘額為 {long} 元")
    public void 使用者_A_帳戶餘額為_元(long balance) {
        assertEquals(balance, withdrawalResult.remainingBalance());
    }
}
