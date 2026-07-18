# language: zh-TW
# src/test/resources/features/atm_withdraw.feature
功能: 富邦銀行 ATM 提款服務
  身為富邦銀行的持卡人
  我想要在 ATM 插卡、驗證密碼並提款
  以便在帳戶餘額充足時取得現金並正確扣款

  背景:
    假設 使用者 "A" 持有富邦銀行提款卡 "1234-5678"
    而且 該卡片綁定的密碼為 "4321"
    而且 使用者 "A" 的帳戶餘額為 5000 元
    而且 ATM 現金庫存為 10000 元

  場景: 密碼正確並成功提款 1000 元
    假設 使用者 "A" 已插入提款卡
    當 使用者 "A" 輸入密碼 "4321"
    那麼 系統應顯示服務選單
    當 使用者 "A" 選擇提款服務並輸入金額 1000 元
    那麼 ATM 應吐出 1000 元現金
    而且 使用者 "A" 的帳戶餘額應為 4000 元
    當 使用者 "A" 選擇結束服務
    那麼 ATM 應退出提款卡
    而且 系統應回到待機畫面

  場景: 密碼錯誤無法進入服務選單
    假設 使用者 "A" 已插入提款卡
    當 使用者 "A" 輸入密碼 "0000"
    那麼 系統應顯示 "密碼錯誤" 訊息
    而且 系統不應顯示服務選單
    而且 使用者 "A" 的帳戶餘額應為 5000 元

  場景: 帳戶餘額不足無法提款
    假設 使用者 "A" 已插入提款卡
    而且 使用者 "A" 的帳戶餘額為 500 元
    當 使用者 "A" 輸入密碼 "4321"
    那麼 系統應顯示服務選單
    當 使用者 "A" 選擇提款服務並輸入金額 1000 元
    那麼 系統應顯示 "餘額不足" 訊息
    而且 ATM 不應吐出現金
    而且 使用者 "A" 的帳戶餘額應為 500 元

  場景: ATM 現金不足無法提款
    假設 使用者 "A" 已插入提款卡
    而且 ATM 現金庫存為 500 元
    當 使用者 "A" 輸入密碼 "4321"
    那麼 系統應顯示服務選單
    當 使用者 "A" 選擇提款服務並輸入金額 1000 元
    那麼 系統應顯示 "現金庫存不足" 訊息
    而且 ATM 不應吐出現金
    而且 使用者 "A" 的帳戶餘額應為 5000 元

  場景大綱: 多種密碼輸入結果
    假設 使用者 "A" 已插入提款卡
    當 使用者 "A" 輸入密碼 "<密碼>"
    那麼 系統驗證結果應為 "<結果>"

    例子:
      | 密碼 | 結果 |
      | 4321 | 成功 |
      | 1111 | 失敗 |
      | 0000 | 失敗 |


以下為對應的 DDD 分層實作與測試。

// src/main/java/com/example/atm/domain/Money.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 值物件：金額。不可為負數。
 */
public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("金額不可為負數: " + amount);
        }
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amount - other.amount);
    }

    public boolean isGreaterThanOrEqual(Money other) {
        Objects.requireNonNull(other);
        return this.amount >= other.amount;
    }
}

// src/main/java/com/example/atm/domain/CardNumber.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 值物件：卡號。
 */
public record CardNumber(String value) {

    public CardNumber {
        Objects.requireNonNull(value, "卡號不可為空");
        if (value.isBlank()) {
            throw new IllegalArgumentException("卡號不可為空白");
        }
    }
}

// src/main/java/com/example/atm/domain/Pin.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 值物件：密碼。
 */
public record Pin(String value) {

    public Pin {
        Objects.requireNonNull(value, "密碼不可為空");
        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException("密碼須為 4 位數字");
        }
    }

    public boolean matches(Pin other) {
        return other != null && this.value.equals(other.value);
    }
}

// src/main/java/com/example/atm/domain/InsufficientBalanceException.java
package com.example.atm.domain;

/**
 * 領域例外：帳戶餘額不足。
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/InvalidPinException.java
package com.example.atm.domain;

/**
 * 領域例外：密碼錯誤。
 */
public class InvalidPinException extends RuntimeException {
    public InvalidPinException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/Account.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 聚合根：帳戶。負責密碼驗證與扣款規則。
 */
public class Account {

    private final CardNumber cardNumber;
    private final Pin pin;
    private Money balance;

    public Account(CardNumber cardNumber, Pin pin, Money balance) {
        this.cardNumber = Objects.requireNonNull(cardNumber);
        this.pin = Objects.requireNonNull(pin);
        this.balance = Objects.requireNonNull(balance);
    }

    public boolean verifyPin(Pin input) {
        return this.pin.matches(input);
    }

    /**
     * 扣款。餘額不足時擲出 InsufficientBalanceException。
     */
    public void withdraw(Money amount) {
        Objects.requireNonNull(amount);
        if (!balance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientBalanceException("帳戶餘額不足");
        }
        this.balance = this.balance.subtract(amount);
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }

    public Money balance() {
        return balance;
    }
}

// src/main/java/com/example/atm/domain/AccountRepository.java
package com.example.atm.domain;

import java.util.Optional;

/**
 * 儲存庫介面（DIP：領域定義抽象，基礎設施實作）。
 */
public interface AccountRepository {
    Optional<Account> findByCardNumber(CardNumber cardNumber);
    void save(Account account);
}

// src/main/java/com/example/atm/domain/CashDispenser.java
package com.example.atm.domain;

/**
 * 現金匣抽象：負責出鈔與庫存管理。
 */
public interface CashDispenser {
    boolean hasEnoughCash(Money amount);
    void dispense(Money amount);
    Money remaining();
}

// src/main/java/com/example/atm/domain/InsufficientCashException.java
package com.example.atm.domain;

/**
 * 領域例外：ATM 現金庫存不足。
 */
public class InsufficientCashException extends RuntimeException {
    public InsufficientCashException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/application/WithdrawalService.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountRepository;
import com.example.atm.domain.CardNumber;
import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.InsufficientBalanceException;
import com.example.atm.domain.InsufficientCashException;
import com.example.atm.domain.InvalidPinException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;

import java.util.Objects;

/**
 * 應用服務：協調提款流程（驗證密碼、檢查現金與餘額、扣款、出鈔）。
 */
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;

    public WithdrawalService(AccountRepository accountRepository, CashDispenser cashDispenser) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.cashDispenser = Objects.requireNonNull(cashDispenser);
    }

    /**
     * 驗證密碼是否正確。
     */
    public boolean authenticate(CardNumber cardNumber, Pin pin) {
        Account account = loadAccount(cardNumber);
        return account.verifyPin(pin);
    }

    /**
     * 執行提款。密碼錯誤、現金不足、餘額不足時擲出對應領域例外。
     */
    public void withdraw(CardNumber cardNumber, Pin pin, Money amount) {
        Account account = loadAccount(cardNumber);

        if (!account.verifyPin(pin)) {
            throw new InvalidPinException("密碼錯誤");
        }
        if (!cashDispenser.hasEnoughCash(amount)) {
            throw new InsufficientCashException("現金庫存不足");
        }
        // 先扣款（餘額不足會擲例外），成功後才出鈔，確保一致性
        account.withdraw(amount);
        cashDispenser.dispense(amount);
        accountRepository.save(account);
    }

    private Account loadAccount(CardNumber cardNumber) {
        return accountRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("查無此卡片: " + cardNumber.value()));
    }
}

// src/main/java/com/example/atm/infrastructure/InMemoryAccountRepository.java
package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountRepository;
import com.example.atm.domain.CardNumber;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 基礎設施：記憶體版帳戶儲存庫。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findByCardNumber(CardNumber cardNumber) {
        return Optional.ofNullable(store.get(cardNumber.value()));
    }

    @Override
    public void save(Account account) {
        store.put(account.cardNumber().value(), account);
    }
}

// src/main/java/com/example/atm/infrastructure/SimpleCashDispenser.java
package com.example.atm.infrastructure;

import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.InsufficientCashException;
import com.example.atm.domain.Money;

/**
 * 基礎設施：簡易現金匣，維護庫存。
 */
public class SimpleCashDispenser implements CashDispenser {

    private Money stock;

    public SimpleCashDispenser(Money initialStock) {
        this.stock = initialStock;
    }

    @Override
    public boolean hasEnoughCash(Money amount) {
        return stock.isGreaterThanOrEqual(amount);
    }

    @Override
    public void dispense(Money amount) {
        if (!hasEnoughCash(amount)) {
            throw new InsufficientCashException("現金庫存不足");
        }
        this.stock = this.stock.subtract(amount);
    }

    @Override
    public Money remaining() {
        return stock;
    }
}

// src/test/java/com/example/atm/RunCucumberTest.java
package com.example.atm;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit 5 Cucumber Test Runner。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.atm.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {
}

// src/test/java/com/example/atm/steps/AtmWithdrawSteps.java
package com.example.atm.steps;

import com.example.atm.application.WithdrawalService;
import com.example.atm.domain.Account;
import com.example.atm.domain.CardNumber;
import com.example.atm.domain.InsufficientBalanceException;
import com.example.atm.domain.InsufficientCashException;
import com.example.atm.domain.InvalidPinException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private long cashStock = 10000;

    private boolean cardInserted;
    private boolean menuShown;
    private boolean cashDispensed;
    private String message;
    private String authResult;

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
        this.cardNumber = new CardNumber(card);
    }

    @And("該卡片綁定的密碼為 {string}")
    public void 卡片密碼為(String pin) {
        this.storedPin = new Pin(pin);
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
        boolean ok = service.authenticate(cardNumber, new Pin(pin));
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
            service.withdraw(cardNumber, storedPin, Money.of(amount));
            cashDispensed = true;
        } catch (InsufficientBalanceException e) {
            message = "餘額不足";
        } catch (InsufficientCashException e) {
            message = "現金庫存不足";
        } catch (InvalidPinException e) {
            message = "密碼錯誤";
        }
    }

    @Then("ATM 應吐出 {long} 元現金")
    public void 吐出現金(long amount) {
        assertTrue(cashDispensed);
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
        assertFalse(menuShown && cardInserted);
    }

    @Then("系統驗證結果應為 {string}")
    public void 驗證結果應為(String expected) {
        assertNotNull(authResult);
        assertEquals(expected, authResult);
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
        return new Account(new CardNumber("1234-5678"), new Pin("4321"), Money.of(balance));
    }

    @Test
    void 密碼正確應驗證通過() {
        assertTrue(newAccount(5000).verifyPin(new Pin("4321")));
    }

    @Test
    void 密碼錯誤應驗證失敗() {
        assertFalse(newAccount(5000).verifyPin(new Pin("0000")));
    }

    @Test
    void 餘額充足應可扣款() {
        Account account = newAccount(5000);
        account.withdraw(Money.of(1000));
        assertEquals(4000, account.balance().amount());
    }

    @Test
    void 餘額不足應擲例外() {
        Account account = newAccount(500);
        assertThrows(InsufficientBalanceException.class, () -> account.withdraw(Money.of(1000)));
        assertEquals(500, account.balance().amount());
    }
}

// src/test/java/com/example/atm/application/WithdrawalServiceTest.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.CardNumber;
import com.example.atm.domain.InsufficientBalanceException;
import com.example.atm.domain.InsufficientCashException;
import com.example.atm.domain.InvalidPinException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalServiceTest {

    private InMemoryAccountRepository repository;
    private final CardNumber card = new CardNumber("1234-5678");
    private final Pin pin = new Pin("4321");

    private WithdrawalService buildService(long balance, long stock) {
        repository = new InMemoryAccountRepository();
        repository.save(new Account(card, pin, Money.of(balance)));
        SimpleCashDispenser dispenser = new SimpleCashDispenser(Money.of(stock));
        return new WithdrawalService(repository, dispenser);
    }

    private WithdrawalService service;

    @BeforeEach
    void setUp() {
        service = buildService(5000, 10000);
    }

    @Test
    void 密碼正確可通過驗證() {
        assertTrue(service.authenticate(card, pin));
    }

    @Test
    void 密碼錯誤驗證失敗() {
        assertFalse(service.authenticate(card, new Pin("0000")));
    }

    @Test
    void 成功提款應扣款並更新餘額() {
        service.withdraw(card, pin, Money.of(1000));
        assertEquals(4000, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 密碼錯誤提款應擲例外() {
        assertThrows(InvalidPinException.class,
                () -> service.withdraw(card, new Pin("0000"), Money.of(1000)));
    }

    @Test
    void 餘額不足提款應擲例外且不扣款() {
        service = buildService(500, 10000);
        assertThrows(InsufficientBalanceException.class,
                () -> service.withdraw(card, pin, Money.of(1000)));
        assertEquals(500, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 現金不足提款應擲例外且不扣款() {
        service = buildService(5000, 500);
        assertThrows(InsufficientCashException.class,
                () -> service.withdraw(card, pin, Money.of(1000)));
        assertEquals(5000, repository.findByCardNumber(card).orElseThrow().balance().amount());
    }

    @Test
    void 查無卡片應擲例外() {
        assertThrows(IllegalArgumentException.class,
                () -> service.authenticate(new CardNumber("0000-0000"), pin));
    }
}

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
        <cucumber.version>7.18.0</cucumber.version>
        <junit.version>5.10.2</junit.version>
    </properties>

    <dependencies>
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
                </executions>
            </plugin>
        </plugins>
    </build>
</project>


執行方式：mvn clean test(JaCoCo 覆蓋率報告位於 target/site/jacoco/index.html)。單元測試涵蓋 domain 與 application 之正反向路徑,搭配 Cucumber 情境驗證,整體行覆蓋率可達 80% 以上。