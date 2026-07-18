富邦銀行 ATM 提款服務—完整專案原始碼

以下為完整可編譯的 Java 21 + Cucumber 專案，符合 DDD 分層與 SOLID 原則，測試涵蓋率 ≥ 80%（含 JaCoCo 覆蓋率門檻設定）。

專案結構
fubon-atm/
├── pom.xml
├── src/main/java/com/example/atm/
│   ├── domain/
│   │   ├── Money.java
│   │   ├── Pin.java
│   │   ├── AccountId.java
│   │   ├── Account.java
│   │   ├── AccountRepository.java
│   │   ├── CashDispenser.java
│   │   ├── CardReader.java
│   │   ├── InsufficientBalanceException.java
│   │   ├── InvalidPinException.java
│   │   └── AccountNotFoundException.java
│   ├── application/
│   │   ├── WithdrawalResult.java
│   │   ├── AuthResult.java
│   │   └── AtmSession.java
│   └── infrastructure/
│       ├── InMemoryAccountRepository.java
│       ├── RecordingCashDispenser.java
│       └── SimpleCardReader.java
└── src/test/
    ├── java/com/example/atm/
    │   ├── RunCucumberTest.java
    │   ├── steps/AtmWithdrawalSteps.java
    │   ├── domain/MoneyTest.java
    │   ├── domain/PinTest.java
    │   ├── domain/AccountTest.java
    │   ├── application/AtmSessionTest.java
    │   └── infrastructure/InfrastructureTest.java
    └── resources/features/atm_withdrawal.feature

Production Code — Domain Layer
// src/main/java/com/example/atm/domain/Money.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 金額值物件（Value Object），以「元」為單位，不可為負。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不可為負數: " + amount);
        }
    }

    public static final Money ZERO = new Money(0);

    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        if (this.amount < other.amount) {
            throw new IllegalArgumentException("餘額不足");
        }
        return new Money(this.amount - other.amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other, "other 不可為 null");
        return this.amount >= other.amount;
    }

    public boolean isPositive() {
        return this.amount > 0;
    }
}

// src/main/java/com/example/atm/domain/Pin.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 密碼值物件（Value Object），格式為 4 位數字。
 */
public record Pin(String value) {

    public Pin {
        Objects.requireNonNull(value, "密碼不可為 null");
        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException("密碼必須為 4 位數字");
        }
    }

    public static Pin of(String value) {
        return new Pin(value);
    }

    public boolean matches(Pin other) {
        return this.equals(other);
    }
}

// src/main/java/com/example/atm/domain/AccountId.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶識別碼值物件，用於卡片與帳戶對應。
 */
public record AccountId(String value) {

    public AccountId {
        Objects.requireNonNull(value, "帳戶識別碼不可為 null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("帳戶識別碼不可為空白");
        }
    }

    public static AccountId of(String value) {
        return new AccountId(value);
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

// src/main/java/com/example/atm/domain/InvalidPinException.java
package com.example.atm.domain;

/**
 * 密碼驗證失敗領域例外。
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/AccountNotFoundException.java
package com.example.atm.domain;

/**
 * 查無帳戶領域例外。
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/Account.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root），維護密碼驗證與餘額變更的一致性邊界。
 */
public class Account {

    private final AccountId id;
    private final Pin pin;
    private Money balance;

    public Account(AccountId id, Pin pin, Money balance) {
        this.id = Objects.requireNonNull(id, "id 不可為 null");
        this.pin = Objects.requireNonNull(pin, "pin 不可為 null");
        this.balance = Objects.requireNonNull(balance, "balance 不可為 null");
    }

    public boolean verifyPin(Pin input) {
        Objects.requireNonNull(input, "input 不可為 null");
        return this.pin.matches(input);
    }

    /**
     * 提款：金額必須為正，且不得超過餘額（BR-03、BR-04）。
     *
     * @throws IllegalArgumentException      金額 ≤ 0
     * @throws InsufficientBalanceException  餘額不足
     */
    public void withdraw(Money requested) {
        Objects.requireNonNull(requested, "requested 不可為 null");
        if (!requested.isPositive()) {
            throw new IllegalArgumentException("提款金額必須大於 0");
        }
        if (!this.balance.isGreaterThanOrEqual(requested)) {
            throw new InsufficientBalanceException("餘額不足");
        }
        this.balance = this.balance.subtract(requested);
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }
}

// src/main/java/com/example/atm/domain/AccountRepository.java
package com.example.atm.domain;

import java.util.Optional;

/**
 * 帳戶儲存庫介面（Repository），由基礎設施層實作（DIP）。
 */
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    void save(Account account);
}

// src/main/java/com/example/atm/domain/CashDispenser.java
package com.example.atm.domain;

/**
 * 出鈔裝置介面，抽象化實體 ATM 硬體（DIP、ISP）。
 */
public interface CashDispenser {
    void dispense(Money money);
}

// src/main/java/com/example/atm/domain/CardReader.java
package com.example.atm.domain;

/**
 * 讀卡與退卡裝置介面（DIP、ISP）。
 */
public interface CardReader {
    void eject();
    boolean isEjected();
}

Production Code — Application Layer
// src/main/java/com/example/atm/application/AuthResult.java
package com.example.atm.application;

/**
 * 密碼驗證結果。
 */
public record AuthResult(boolean authenticated, String message) {

    public static AuthResult success() {
        return new AuthResult(true, "驗證通過");
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message);
    }
}

// src/main/java/com/example/atm/application/WithdrawalResult.java
package com.example.atm.application;

/**
 * 提款結果，供應用層對外回傳。
 */
public record WithdrawalResult(boolean success, long remainingBalance, String message) {

    public static WithdrawalResult success(long remainingBalance) {
        return new WithdrawalResult(true, remainingBalance, "提款成功");
    }

    public static WithdrawalResult failure(long remainingBalance, String message) {
        return new WithdrawalResult(false, remainingBalance, message);
    }
}

// src/main/java/com/example/atm/application/AtmSession.java
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

Production Code — Infrastructure Layer
// src/main/java/com/example/atm/infrastructure/InMemoryAccountRepository.java
package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體版帳戶儲存庫，執行緒安全，供測試與示範使用。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Account account) {
        store.put(account.id(), account);
    }
}

// src/main/java/com/example/atm/infrastructure/RecordingCashDispenser.java
package com.example.atm.infrastructure;

import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.Money;

/**
 * 記錄出鈔總額的出鈔裝置實作，供測試驗證出鈔一致性。
 */
public class RecordingCashDispenser implements CashDispenser {

    private long totalDispensed = 0;

    @Override
    public void dispense(Money money) {
        this.totalDispensed += money.amount();
    }

    public long totalDispensed() {
        return totalDispensed;
    }
}

// src/main/java/com/example/atm/infrastructure/SimpleCardReader.java
package com.example.atm.infrastructure;

import com.example.atm.domain.CardReader;

/**
 * 簡易讀卡機實作，記錄退卡狀態。
 */
public class SimpleCardReader implements CardReader {

    private boolean ejected = false;

    @Override
    public void eject() {
        this.ejected = true;
    }

    @Override
    public boolean isEjected() {
        return ejected;
    }
}

Test Code — Cucumber Feature
# language: zh-TW
# src/test/resources/features/atm_withdrawal.feature
功能: 富邦銀行 ATM 提款服務
  身為富邦銀行的持卡人
  我想在 ATM 插卡並驗證密碼後進行提款
  以便快速領取現金並安全結束服務

  背景:
    假設 富邦銀行 ATM 已就緒
    而且 使用者 "A" 持有提款卡，密碼為 "1234"，帳戶餘額為 5000 元

  場景: 密碼正確且餘額充足時成功提款 1000 元
    假設 使用者 "A" 插入提款卡
    當 使用者 "A" 輸入密碼 "1234"
    那麼 密碼驗證通過
    而且 使用者 "A" 可以在畫面上選擇服務
    當 使用者 "A" 選擇提款 1000 元
    那麼 提款機提供 1000 元現金
    而且 使用者 "A" 帳戶餘額應為 4000 元
    當 使用者 "A" 選擇結束服務
    那麼 提款卡應被退出

  場景: 密碼錯誤時無法進入服務選單
    假設 使用者 "A" 插入提款卡
    當 使用者 "A" 輸入密碼 "9999"
    那麼 密碼驗證失敗
    而且 畫面應顯示錯誤訊息 "密碼錯誤"
    但是 使用者 "A" 無法在畫面上選擇服務

  場景: 密碼正確但餘額不足時提款失敗
    假設 使用者 "A" 插入提款卡
    而且 使用者 "A" 帳戶餘額為 500 元
    當 使用者 "A" 輸入密碼 "1234"
    那麼 密碼驗證通過
    當 使用者 "A" 選擇提款 1000 元
    那麼 提款機不提供現金
    而且 畫面應顯示錯誤訊息 "餘額不足"
    而且 使用者 "A" 帳戶餘額應為 500 元

  場景大綱: 不同提款金額的結果驗證
    假設 使用者 "A" 插入提款卡
    而且 使用者 "A" 帳戶餘額為 <餘額> 元
    當 使用者 "A" 輸入密碼 "1234"
    那麼 密碼驗證通過
    當 使用者 "A" 選擇提款 <提款金額> 元
    那麼 提款結果應為 "<結果>"
    而且 使用者 "A" 帳戶餘額應為 <剩餘餘額> 元

    例子:
      | 餘額 | 提款金額 | 結果 | 剩餘餘額 |
      | 5000 | 1000     | 成功 | 4000     |
      | 1000 | 1000     | 成功 | 0        |
      | 800  | 1000     | 失敗 | 800      |
      | 5000 | 0        | 失敗 | 5000     |

Test Code — Cucumber Runner 與 Step Definitions
// src/test/java/com/example/atm/RunCucumberTest.java
package com.example.atm;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit 5 Platform 上的 Cucumber Test Runner。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.atm.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {
}

// src/test/java/com/example/atm/steps/AtmWithdrawalSteps.java
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

Test Code — 單元測試
// src/test/java/com/example/atm/domain/MoneyTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void negativeAmount_throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void zeroConstant_isZero() {
        assertEquals(0, Money.ZERO.amount());
    }

    @Test
    void add_returnsSum() {
        assertEquals(3000, Money.of(1000).add(Money.of(2000)).amount());
    }

    @Test
    void subtract_returnsDifference() {
        assertEquals(500, Money.of(1500).subtract(Money.of(1000)).amount());
    }

    @Test
    void subtract_overBalance_throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(100).subtract(Money.of(200)));
    }

    @Test
    void isGreaterThanOrEqual_boundaryValues() {
        assertTrue(Money.of(1000).isGreaterThanOrEqual(Money.of(1000)));
        assertTrue(Money.of(1001).isGreaterThanOrEqual(Money.of(1000)));
        assertFalse(Money.of(999).isGreaterThanOrEqual(Money.of(1000)));
    }

    @Test
    void isPositive() {
        assertTrue(Money.of(1).isPositive());
        assertFalse(Money.ZERO.isPositive());
    }

    @Test
    void nullArgument_throws() {
        assertThrows(NullPointerException.class, () -> Money.of(100).add(null));
        assertThrows(NullPointerException.class, () -> Money.of(100).subtract(null));
        assertThrows(NullPointerException.class, () -> Money.of(100).isGreaterThanOrEqual(null));
    }
}

// src/test/java/com/example/atm/domain/PinTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinTest {

    @Test
    void validFourDigits_ok() {
        assertTrue(Pin.of("1234").matches(Pin.of("1234")));
    }

    @Test
    void differentPin_notMatch() {
        assertFalse(Pin.of("1234").matches(Pin.of("9999")));
    }

    @Test
    void nonNumeric_throws() {
        assertThrows(IllegalArgumentException.class, () -> Pin.of("abcd"));
    }

    @Test
    void wrongLength_throws() {
        assertThrows(IllegalArgumentException.class, () -> Pin.of("123"));
        assertThrows(IllegalArgumentException.class, () -> Pin.of("12345"));
    }

    @Test
    void nullValue_throws() {
        assertThrows(NullPointerException.class, () -> Pin.of(null));
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

    private Account newAccount(long balance) {
        return new Account(AccountId.of("A"), Pin.of("1234"), Money.of(balance));
    }

    @Test
    void verifyPin_correct() {
        assertTrue(newAccount(5000).verifyPin(Pin.of("1234")));
    }

    @Test
    void verifyPin_wrong() {
        assertFalse(newAccount(5000).verifyPin(Pin.of("9999")));
    }

    @Test
    void withdraw_sufficient_deducts() {
        Account acc = newAccount(5000);
        acc.withdraw(Money.of(1000));
        assertEquals(4000, acc.balance().amount());
    }

    @Test
    void withdraw_exactBalance_toZero() {
        Account acc = newAccount(1000);
        acc.withdraw(Money.of(1000));
        assertEquals(0, acc.balance().amount());
    }

    @Test
    void withdraw_insufficient_throws() {
        Account acc = newAccount(500);
        assertThrows(InsufficientBalanceException.class, () -> acc.withdraw(Money.of(1000)));
        assertEquals(500, acc.balance().amount());
    }

    @Test
    void withdraw_zero_throws() {
        Account acc = newAccount(5000);
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(Money.ZERO));
    }

    @Test
    void constructor_nullArgs_throw() {
        assertThrows(NullPointerException.class,
                () -> new Account(null, Pin.of("1234"), Money.of(1)));
        assertThrows(NullPointerException.class,
                () -> new Account(AccountId.of("A"), null, Money.of(1)));
        assertThrows(NullPointerException.class,
                () -> new Account(AccountId.of("A"), Pin.of("1234"), null));
    }

    @Test
    void accountId_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> AccountId.of(" "));
        assertThrows(NullPointerException.class, () -> AccountId.of(null));
    }

    @Test
    void idAndBalance_getters() {
        Account acc = newAccount(5000);
        assertEquals("A", acc.id().value());
        assertEquals(5000, acc.balance().amount());
    }
}

// src/test/java/com/example/atm/application/AtmSessionTest.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.AccountNotFoundException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.RecordingCashDispenser;
import com.example.atm.infrastructure.SimpleCardReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmSessionTest {

    private InMemoryAccountRepository repository;
    private RecordingCashDispenser dispenser;
    private SimpleCardReader cardReader;
    private AtmSession session;

    @BeforeEach
    void init() {
        repository = new InMemoryAccountRepository();
        dispenser = new RecordingCashDispenser();
        cardReader = new SimpleCardReader();
        session = new AtmSession(repository, dispenser, cardReader);
        repository.save(new Account(AccountId.of("A"), Pin.of("1234"), Money.of(5000)));
    }

    @Test
    void happyPath_success() {
        session.insertCard("A");
        assertTrue(session.enterPin("1234").authenticated());
        assertTrue(session.canSelectService());

        WithdrawalResult result = session.withdraw(1000);
        assertTrue(result.success());
        assertEquals(4000, result.remainingBalance());
        assertEquals(1000, dispenser.totalDispensed());

        session.endSession();
        assertTrue(session.isCardEjected());
        assertEquals(AtmSession.State.ENDED, session.state());
    }

    @Test
    void wrongPin_cannotSelectService() {
        session.insertCard("A");
        AuthResult result = session.enterPin("9999");
        assertFalse(result.authenticated());
        assertFalse(session.canSelectService());
        assertEquals("密碼錯誤", session.lastMessage());
    }

    @Test
    void insufficientBalance_fails() {
        repository.save(new Account(AccountId.of("A"), Pin.of("1234"), Money.of(500)));
        session.insertCard("A");
        session.enterPin("1234");

        WithdrawalResult result = session.withdraw(1000);
        assertFalse(result.success());
        assertEquals("餘額不足", result.message());
        assertEquals(500, result.remainingBalance());
        assertEquals(0, dispenser.totalDispensed());
    }

    @Test
    void zeroAmount_fails() {
        session.insertCard("A");
        session.enterPin("1234");
        WithdrawalResult result = session.withdraw(0);
        assertFalse(result.success());
        assertEquals("提款金額必須大於 0", result.message());
    }

    @Test
    void withdrawBeforeAuth_throws() {
        session.insertCard("A");
        assertThrows(IllegalStateException.class, () -> session.withdraw(1000));
    }

    @Test
    void enterPinBeforeInsert_throws() {
        assertThrows(IllegalStateException.class, () -> session.enterPin("1234"));
    }

    @Test
    void insertUnknownAccount_throws() {
        assertThrows(AccountNotFoundException.class, () -> session.insertCard("Z"));
    }

    @Test
    void constructor_nullArgs_throw() {
        assertThrows(NullPointerException.class,
                () -> new AtmSession(null, dispenser, cardReader));
        assertThrows(NullPointerException.class,
                () -> new AtmSession(repository, null, cardReader));
        assertThrows(NullPointerException.class,
                () -> new AtmSession(repository, dispenser, null));
    }

    @Test
    void authResult_and_withdrawalResult_messages() {
        assertTrue(AuthResult.success().authenticated());
        assertFalse(AuthResult.failure("x").authenticated());
        assertTrue(WithdrawalResult.success(100).success());
        assertFalse(WithdrawalResult.failure(100, "x").success());
    }
}

// src/test/java/com/example/atm/infrastructure/InfrastructureTest.java
package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountId;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureTest {

    @Test
    void repository_saveAndFind() {
        InMemoryAccountRepository repo = new InMemoryAccountRepository();
        Account acc = new Account(AccountId.of("A"), Pin.of("1234"), Money.of(5000));
        repo.save(acc);
        assertTrue(repo.findById(AccountId.of("A")).isPresent());
        assertFalse(repo.findById(AccountId.of("B")).isPresent());
    }

    @Test
    void dispenser_accumulates() {
        RecordingCashDispenser dispenser = new RecordingCashDispenser();
        dispenser.dispense(Money.of(1000));
        dispenser.dispense(Money.of(500));
        assertEquals(1500, dispenser.totalDispensed());
    }

    @Test
    void cardReader_ejectLifecycle() {
        SimpleCardReader reader = new SimpleCardReader();
        assertFalse(reader.isEjected());
        reader.eject();
        assertTrue(reader.isEjected());
    }
}

建置設定（含 JaCoCo 80% 覆蓋率門檻）
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
        <jacoco.version>0.8.11</jacoco.version>
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
                <version>${jacoco.version}</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
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
                        <phase>verify</phase>
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

執行與驗證
指令	用途
mvn test	執行 Cucumber 情境與 JUnit 單元測試
mvn verify	執行測試並驗證覆蓋率 ≥ 80%
target/site/jacoco/index.html	JaCoCo 覆蓋率報告
覆蓋率預估
分層	主要類別	覆蓋方式	預估行覆蓋率
Domain	Money、Pin、Account、AccountId	單元測試 + Cucumber	≥ 95%
Application	AtmSession、WithdrawalResult、AuthResult	單元測試 + Cucumber	≥ 90%
Infrastructure	三個介面實作	單元測試 + Cucumber	≥ 90%
整體	—	—	≥ 85%（達成 80% 門檻）
SOLID 對應說明
原則	落實方式
SRP	各值物件、聚合根、應用服務職責單一
OCP	透過介面擴充出鈔／讀卡／儲存實作，無須修改核心
LSP	所有介面實作可互換替換
ISP	CashDispenser、CardReader、AccountRepository 介面各自精簡
DIP	AtmSession 依賴抽象介面而非具體實作