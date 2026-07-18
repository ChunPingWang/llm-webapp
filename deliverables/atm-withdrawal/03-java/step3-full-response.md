以下為符合 DDD 分層與 SOLID 原則的完整實作，包含 domain / application / infrastructure 三層、Cucumber step definitions、JUnit 5 runner 及單元測試。

1. Domain Layer
// src/main/java/com/example/atm/domain/Money.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 金額值物件（Value Object），確保金額不為負且提供不可變運算。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不得為負數: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "比較對象不得為 null");
        return this.amount >= other.amount;
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "扣除金額不得為 null");
        return new Money(this.amount - other.amount);
    }

    @Override
    public String toString() {
        return amount + " 元";
    }
}

// src/main/java/com/example/atm/domain/CardStatus.java
package com.example.atm.domain;

/**
 * 提款卡狀態。
 */
public enum CardStatus {
    ACTIVE,   // 正常可用
    LOCKED    // 已鎖卡
}

// src/main/java/com/example/atm/domain/Account.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root），負責維護餘額一致性。
 */
public class Account {

    private final String accountId;
    private Money balance;

    public Account(String accountId, Money balance) {
        this.accountId = Objects.requireNonNull(accountId, "帳號不得為 null");
        this.balance = Objects.requireNonNull(balance, "餘額不得為 null");
    }

    public boolean canWithdraw(Money amount) {
        return balance.isGreaterThanOrEqual(amount);
    }

    /**
     * 執行扣款，餘額不足時拋出領域例外。
     */
    public void withdraw(Money amount) {
        if (!canWithdraw(amount)) {
            throw new InsufficientBalanceException(
                    "餘額不足，目前餘額 " + balance + "，欲提領 " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public Money getBalance() {
        return balance;
    }

    public String getAccountId() {
        return accountId;
    }
}

// src/main/java/com/example/atm/domain/Card.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 提款卡聚合根，負責密碼驗證與鎖卡邏輯。
 */
public class Card {

    public static final int MAX_PIN_ATTEMPTS = 3;

    private final String cardNumber;
    private final String correctPin;
    private final Account account;
    private int failedAttempts;
    private CardStatus status;

    public Card(String cardNumber, String correctPin, Account account) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不得為 null");
        this.correctPin = Objects.requireNonNull(correctPin, "密碼不得為 null");
        this.account = Objects.requireNonNull(account, "帳戶不得為 null");
        this.failedAttempts = 0;
        this.status = CardStatus.ACTIVE;
    }

    /**
     * 驗證密碼；正確則重置錯誤次數，錯誤累積達上限則鎖卡。
     *
     * @return true 表示密碼正確
     */
    public boolean verifyPin(String inputPin) {
        if (status == CardStatus.LOCKED) {
            throw new CardLockedException("提款卡已被鎖定，卡號: " + cardNumber);
        }
        if (correctPin.equals(inputPin)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_PIN_ATTEMPTS) {
            status = CardStatus.LOCKED;
        }
        return false;
    }

    public boolean isLocked() {
        return status == CardStatus.LOCKED;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Account getAccount() {
        return account;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public CardStatus getStatus() {
        return status;
    }
}

// src/main/java/com/example/atm/domain/InsufficientBalanceException.java
package com.example.atm.domain;

/**
 * 餘額不足領域例外。
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/CardLockedException.java
package com.example.atm.domain;

/**
 * 提款卡已鎖定領域例外。
 */
public class CardLockedException extends RuntimeException {
    public CardLockedException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/CardRepository.java
package com.example.atm.domain;

import java.util.Optional;

/**
 * 提款卡儲存庫介面（Repository），由 infrastructure 層實作，遵循 DIP。
 */
public interface CardRepository {
    Optional<Card> findByCardNumber(String cardNumber);
    void save(Card card);
}

2. Application Layer
// src/main/java/com/example/atm/application/CashDispenser.java
package com.example.atm.application;

import com.example.atm.domain.Money;

/**
 * 吐鈔設備介面，抽象化硬體，遵循 DIP 與 ISP。
 */
public interface CashDispenser {
    void dispense(Money amount);
}

// src/main/java/com/example/atm/application/WithdrawalResult.java
package com.example.atm.application;

/**
 * 提款結果值物件。
 */
public record WithdrawalResult(boolean success, String message, long remainingBalance) {

    public static WithdrawalResult success(long remainingBalance) {
        return new WithdrawalResult(true, "提款成功", remainingBalance);
    }

    public static WithdrawalResult failure(String message, long remainingBalance) {
        return new WithdrawalResult(false, message, remainingBalance);
    }
}

// src/main/java/com/example/atm/application/AtmService.java
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

3. Infrastructure Layer
// src/main/java/com/example/atm/infrastructure/InMemoryCardRepository.java
package com.example.atm.infrastructure;

import com.example.atm.domain.Card;
import com.example.atm.domain.CardRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體版卡片儲存庫實作。
 */
public class InMemoryCardRepository implements CardRepository {

    private final Map<String, Card> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Card> findByCardNumber(String cardNumber) {
        return Optional.ofNullable(store.get(cardNumber));
    }

    @Override
    public void save(Card card) {
        store.put(card.getCardNumber(), card);
    }
}

// src/main/java/com/example/atm/infrastructure/SimpleCashDispenser.java
package com.example.atm.infrastructure;

import com.example.atm.application.CashDispenser;
import com.example.atm.domain.Money;

/**
 * 簡易吐鈔設備實作，記錄最後一次吐出的金額。
 */
public class SimpleCashDispenser implements CashDispenser {

    private long lastDispensedAmount = 0;

    @Override
    public void dispense(Money amount) {
        this.lastDispensedAmount = amount.amount();
    }

    public long getLastDispensedAmount() {
        return lastDispensedAmount;
    }
}

4. Cucumber Step Definitions
// src/test/java/com/example/atm/bdd/AtmStepDefinitions.java
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

    @And("使用者 A 選擇提款 {long} 元")
    public void 使用者_A_選擇提款_元_And(long amount) {
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

5. Cucumber JUnit 5 Runner
// src/test/java/com/example/atm/bdd/RunCucumberTest.java
package com.example.atm.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.atm.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html")
public class RunCucumberTest {
}

# language: zh-TW
# 檔名: src/test/resources/features/fubon_atm_withdrawal.feature

功能: 富邦銀行 ATM 提款服務

  背景:
    假設 使用者 A 持有富邦銀行的提款卡
    而且 使用者 A 到富邦銀行的 ATM 插入提款卡

  場景: 密碼正確且餘額充足時成功提款
    假設 使用者 A 的帳戶餘額為 5000 元
    當 使用者 A 輸入正確的密碼
    那麼 畫面上顯示可以選擇服務的選單
    當 使用者 A 選擇提款 1000 元
    那麼 提款機提供 1000 元現金
    而且 使用者 A 帳戶餘額扣除 1000 元後為 4000 元
    當 使用者 A 選擇結束服務
    那麼 提款機退出提款卡

  場景: 密碼錯誤無法進入服務選單
    當 使用者 A 輸入錯誤的密碼
    那麼 畫面上顯示密碼錯誤的訊息
    而且 畫面上不顯示服務選單
    而且 使用者 A 無法選擇任何服務

  場景: 密碼連續錯誤三次時鎖卡
    當 使用者 A 連續 3 次輸入錯誤的密碼
    那麼 畫面上顯示提款卡已被鎖定的訊息
    而且 提款機保留提款卡不予退出

  場景: 餘額不足時無法提款
    假設 使用者 A 的帳戶餘額為 500 元
    當 使用者 A 輸入正確的密碼
    那麼 畫面上顯示可以選擇服務的選單
    當 使用者 A 選擇提款 1000 元
    那麼 提款機顯示餘額不足的訊息
    而且 提款機不提供任何現金
    而且 使用者 A 帳戶餘額維持 500 元

  場景大綱: 不同餘額與提款金額的提款結果
    假設 使用者 A 的帳戶餘額為 <初始餘額> 元
    當 使用者 A 輸入正確的密碼
    而且 使用者 A 選擇提款 <提款金額> 元
    那麼 提款機的處理結果為 "<結果>"
    而且 使用者 A 帳戶餘額為 <剩餘餘額> 元

    例子:
      | 初始餘額 | 提款金額 | 結果     | 剩餘餘額 |
      | 5000     | 1000     | 提款成功 | 4000     |
      | 1000     | 1000     | 提款成功 | 0        |
      | 800      | 1000     | 餘額不足 | 800      |
      | 0        | 1000     | 餘額不足 | 0        |

6. 單元測試（提升覆蓋率至 80% 以上）
// src/test/java/com/example/atm/domain/MoneyTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void 建立負金額應拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void 金額大於等於比較() {
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(1000)));
        assertTrue(Money.of(1001).isGreaterThanOrEqual(Money.of(1000)));
        assertFalse(Money.of(999).isGreaterThanOrEqual(Money.of(1000)));
    }

    @Test
    void 金額扣除() {
        assertEquals(Money.of(4000), Money.of(5000).subtract(Money.of(1000)));
    }

    @Test
    void 扣除後為負應拋出例外() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(500).subtract(Money.of(1000)));
    }

    @Test
    void toString應包含元() {
        assertEquals("1000 元", Money.of(1000).toString());
    }
}

// src/test/java/com/example/atm/domain/AccountTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    @Test
    void 餘額充足可提款() {
        Account account = new Account("ACC-1", Money.of(5000));
        assertTrue(account.canWithdraw(Money.of(1000)));
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(4000), account.getBalance());
    }

    @Test
    void 餘額不足不可提款() {
        Account account = new Account("ACC-1", Money.of(500));
        assertFalse(account.canWithdraw(Money.of(1000)));
        assertThrows(InsufficientBalanceException.class,
                () -> account.withdraw(Money.of(1000)));
        assertEquals(Money.of(500), account.getBalance());
    }

    @Test
    void 取得帳號() {
        Account account = new Account("ACC-1", Money.of(0));
        assertEquals("ACC-1", account.getAccountId());
    }
}

// src/test/java/com/example/atm/domain/CardTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardTest {

    private Card newCard() {
        return new Card("C-1", "8888", new Account("ACC-1", Money.of(1000)));
    }

    @Test
    void 密碼正確回傳true並重置錯誤次數() {
        Card card = newCard();
        card.verifyPin("0000"); // 累積一次錯誤
        assertEquals(1, card.getFailedAttempts());
        assertTrue(card.verifyPin("8888"));
        assertEquals(0, card.getFailedAttempts());
    }

    @Test
    void 密碼錯誤回傳false並累積次數() {
        Card card = newCard();
        assertFalse(card.verifyPin("0000"));
        assertEquals(1, card.getFailedAttempts());
    }

    @Test
    void 連續三次錯誤鎖卡() {
        Card card = newCard();
        card.verifyPin("0000");
        card.verifyPin("0000");
        card.verifyPin("0000");
        assertTrue(card.isLocked());
        assertEquals(CardStatus.LOCKED, card.getStatus());
    }

    @Test
    void 鎖卡後再驗證應拋出例外() {
        Card card = newCard();
        card.verifyPin("0000");
        card.verifyPin("0000");
        card.verifyPin("0000");
        assertThrows(CardLockedException.class, () -> card.verifyPin("8888"));
    }

    @Test
    void 取得卡號與帳戶() {
        Card card = newCard();
        assertEquals("C-1", card.getCardNumber());
        assertEquals("ACC-1", card.getAccount().getAccountId());
    }
}

// src/test/java/com/example/atm/application/AtmServiceTest.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.Card;
import com.example.atm.domain.CardLockedException;
import com.example.atm.domain.Money;
import com.example.atm.infrastructure.InMemoryCardRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmServiceTest {

    private InMemoryCardRepository repository;
    private SimpleCashDispenser dispenser;
    private AtmService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCardRepository();
        dispenser = new SimpleCashDispenser();
        service = new AtmService(repository, dispenser);
    }

    private void givenCard(long balance) {
        Account account = new Account("ACC-A", Money.of(balance));
        repository.save(new Card("1234-5678", "8888", account));
    }

    @Test
    void 密碼正確驗證成功() {
        givenCard(5000);
        assertTrue(service.authenticate("1234-5678", "8888"));
    }

    @Test
    void 密碼錯誤驗證失敗() {
        givenCard(5000);
        assertFalse(service.authenticate("1234-5678", "0000"));
    }

    @Test
    void 餘額充足提款成功() {
        givenCard(5000);
        WithdrawalResult result = service.withdraw("1234-5678", 1000);
        assertTrue(result.success());
        assertEquals(4000, result.remainingBalance());
        assertEquals(1000, dispenser.getLastDispensedAmount());
    }

    @Test
    void 餘額不足提款失敗且不吐鈔() {
        givenCard(500);
        WithdrawalResult result = service.withdraw("1234-5678", 1000);
        assertFalse(result.success());
        assertEquals("餘額不足", result.message());
        assertEquals(500, result.remainingBalance());
        assertEquals(0, dispenser.getLastDispensedAmount());
    }

    @Test
    void 鎖卡後提款拋出例外() {
        givenCard(5000);
        service.authenticate("1234-5678", "0000");
        service.authenticate("1234-5678", "0000");
        service.authenticate("1234-5678", "0000");
        assertTrue(service.isCardLocked("1234-5678"));
        assertThrows(CardLockedException.class, () -> service.withdraw("1234-5678", 1000));
    }

    @Test
    void 查無此卡拋出例外() {
        assertThrows(IllegalArgumentException.class,
                () -> service.withdraw("9999", 1000));
    }
}

7. 建置檔（Maven）
<!-- pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>fubon-atm</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.10.2</junit.version>
        <cucumber.version>7.15.0</cucumber.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <version>1.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check-coverage</id>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.80</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>

設計說明
原則	落實方式
SRP	Card 管密碼／鎖卡、Account 管餘額、AtmService 協調用例，各司其職。
OCP	新增吐鈔或儲存機制只需實作 CashDispenser／CardRepository，不修改既有程式。
LSP	所有 CashDispenser／CardRepository 實作可互換。
ISP	CashDispenser 僅暴露 dispense，介面精簡。
DIP	AtmService 依賴抽象介面而非具體實作，由外層注入。
DDD	domain（實體、值物件、聚合、Repository 介面）／application（用例服務）／infrastructure（技術實作）三層分離。

執行 mvn test 即會跑 Cucumber 場景與 JUnit 單元測試，並由 JaCoCo 驗證行覆蓋率須 ≥ 80%（check-coverage 未達標時建置失敗）。