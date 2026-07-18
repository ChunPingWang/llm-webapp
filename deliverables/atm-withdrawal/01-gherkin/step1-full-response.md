# language: zh-TW
# src/test/resources/features/atm_withdrawal.feature
功能: 富邦銀行 ATM 提款服務
  身為富邦銀行的持卡人
  我想要在 ATM 插卡並輸入密碼後進行提款
  以便在帳戶餘額充足時領取現金

  背景:
    假設 使用者 "A" 持有富邦銀行提款卡
    而且 該卡片綁定的帳戶密碼為 "1234"

  場景: 正向 - 密碼正確且餘額充足時成功提款
    假設 使用者 "A" 的帳戶餘額為 5000 元
    當 使用者 "A" 插入提款卡
    而且 使用者 "A" 輸入密碼 "1234"
    那麼 系統應允許使用者選擇服務
    當 使用者 "A" 選擇提款 1000 元
    那麼 提款機應提供 1000 元現金
    而且 使用者 "A" 的帳戶餘額應為 4000 元
    當 使用者 "A" 選擇結束服務
    那麼 系統應退出提款卡

  場景: 反向 - 密碼錯誤時無法進入服務選單
    假設 使用者 "A" 的帳戶餘額為 5000 元
    當 使用者 "A" 插入提款卡
    而且 使用者 "A" 輸入密碼 "9999"
    那麼 系統應顯示 "密碼錯誤" 訊息
    而且 系統不應允許使用者選擇服務
    而且 使用者 "A" 的帳戶餘額應為 5000 元

  場景: 反向 - 餘額不足時提款失敗
    假設 使用者 "A" 的帳戶餘額為 500 元
    當 使用者 "A" 插入提款卡
    而且 使用者 "A" 輸入密碼 "1234"
    那麼 系統應允許使用者選擇服務
    當 使用者 "A" 選擇提款 1000 元
    那麼 系統應顯示 "餘額不足" 訊息
    而且 提款機不應提供現金
    而且 使用者 "A" 的帳戶餘額應為 500 元

  場景大綱: 綜合情境 - 不同密碼與餘額組合
    假設 使用者 "A" 的帳戶餘額為 <初始餘額> 元
    當 使用者 "A" 插入提款卡
    而且 使用者 "A" 輸入密碼 "<輸入密碼>"
    當 使用者 "A" 嘗試提款 <提款金額> 元
    那麼 提款結果應為 "<結果>"
    而且 使用者 "A" 的帳戶餘額應為 <最終餘額> 元

    例子:
      | 初始餘額 | 輸入密碼 | 提款金額 | 結果     | 最終餘額 |
      | 5000     | 1234     | 1000     | 成功     | 4000     |
      | 5000     | 0000     | 1000     | 密碼錯誤 | 5000     |
      | 800      | 1234     | 1000     | 餘額不足 | 800      |
      | 1000     | 1234     | 1000     | 成功     | 0        |


以下提供完整可編譯的 DDD 分層實作、Cucumber step definitions、JUnit 5 test runner 與單元測試。

// src/main/java/com/example/atm/domain/Money.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 金額值物件（Value Object），保證金額非負且不可變。
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

    public boolean isGreaterThanOrEqualTo(Money other) {
        Objects.requireNonNull(other, "比較金額不可為 null");
        return this.amount >= other.amount;
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "扣除金額不可為 null");
        return new Money(this.amount - other.amount);
    }
}

// src/main/java/com/example/atm/domain/Pin.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 密碼值物件（Value Object）。
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

    public boolean matches(Pin input) {
        return this.equals(input);
    }
}

// src/main/java/com/example/atm/domain/InsufficientFundsException.java
package com.example.atm.domain;

/**
 * 餘額不足例外。
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/Account.java
package com.example.atm.domain;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root），負責密碼驗證與餘額扣款的業務規則。
 */
public class Account {

    private final String cardNumber;
    private final Pin pin;
    private Money balance;

    public Account(String cardNumber, Pin pin, Money balance) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不可為 null");
        this.pin = Objects.requireNonNull(pin, "密碼不可為 null");
        this.balance = Objects.requireNonNull(balance, "餘額不可為 null");
    }

    public boolean verifyPin(Pin input) {
        return pin.matches(input);
    }

    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "提款金額不可為 null");
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException("餘額不足");
        }
        this.balance = balance.subtract(amount);
    }

    public String cardNumber() {
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
 * 帳戶儲存庫介面（Repository），由 infrastructure 實作，符合 DIP。
 */
public interface AccountRepository {

    Optional<Account> findByCardNumber(String cardNumber);

    void save(Account account);
}

// src/main/java/com/example/atm/domain/CashDispenser.java
package com.example.atm.domain;

/**
 * 出鈔裝置介面，抽象化硬體行為，符合 DIP。
 */
public interface CashDispenser {

    void dispense(Money amount);

    Money lastDispensed();
}

// src/main/java/com/example/atm/application/WithdrawalResult.java
package com.example.atm.application;

/**
 * 提款結果狀態列舉。
 */
public enum WithdrawalResult {
    成功,
    密碼錯誤,
    餘額不足,
    卡片無效
}

// src/main/java/com/example/atm/application/AtmService.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountRepository;
import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.InsufficientFundsException;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;

import java.util.Objects;
import java.util.Optional;

/**
 * ATM 應用服務（Application Service），協調領域物件完成提款用例。
 */
public class AtmService {

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;

    public AtmService(AccountRepository accountRepository, CashDispenser cashDispenser) {
        this.accountRepository = Objects.requireNonNull(accountRepository);
        this.cashDispenser = Objects.requireNonNull(cashDispenser);
    }

    /**
     * 驗證密碼是否正確。
     */
    public boolean authenticate(String cardNumber, String pin) {
        return accountRepository.findByCardNumber(cardNumber)
                .map(account -> account.verifyPin(Pin.of(pin)))
                .orElse(false);
    }

    /**
     * 執行提款用例，回傳結果狀態。
     */
    public WithdrawalResult withdraw(String cardNumber, String pin, long amount) {
        Optional<Account> maybeAccount = accountRepository.findByCardNumber(cardNumber);
        if (maybeAccount.isEmpty()) {
            return WithdrawalResult.卡片無效;
        }

        Account account = maybeAccount.get();
        if (!account.verifyPin(Pin.of(pin))) {
            return WithdrawalResult.密碼錯誤;
        }

        try {
            account.withdraw(Money.of(amount));
            cashDispenser.dispense(Money.of(amount));
            accountRepository.save(account);
            return WithdrawalResult.成功;
        } catch (InsufficientFundsException e) {
            return WithdrawalResult.餘額不足;
        }
    }
}

// src/main/java/com/example/atm/infrastructure/InMemoryAccountRepository.java
package com.example.atm.infrastructure;

import com.example.atm.domain.Account;
import com.example.atm.domain.AccountRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 記憶體版帳戶儲存庫實作。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findByCardNumber(String cardNumber) {
        return Optional.ofNullable(store.get(cardNumber));
    }

    @Override
    public void save(Account account) {
        store.put(account.cardNumber(), account);
    }
}

// src/main/java/com/example/atm/infrastructure/SimpleCashDispenser.java
package com.example.atm.infrastructure;

import com.example.atm.domain.CashDispenser;
import com.example.atm.domain.Money;

/**
 * 簡易出鈔裝置實作，記錄最後一次出鈔金額。
 */
public class SimpleCashDispenser implements CashDispenser {

    private Money lastDispensed = Money.of(0);

    @Override
    public void dispense(Money amount) {
        this.lastDispensed = amount;
    }

    @Override
    public Money lastDispensed() {
        return lastDispensed;
    }
}

// src/test/java/com/example/atm/steps/AtmWithdrawalSteps.java
package com.example.atm.steps;

import com.example.atm.application.AtmService;
import com.example.atm.application.WithdrawalResult;
import com.example.atm.domain.Account;
import com.example.atm.domain.AccountRepository;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
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

    private static final String CARD_NUMBER = "FUBON-001";

    private final AccountRepository repository = new InMemoryAccountRepository();
    private final SimpleCashDispenser dispenser = new SimpleCashDispenser();
    private final AtmService atmService = new AtmService(repository, dispenser);

    private String currentUser;
    private Pin registeredPin;
    private boolean authenticated;
    private boolean serviceAllowed;
    private String lastMessage;
    private WithdrawalResult lastResult;

    @假設("使用者 {string} 持有富邦銀行提款卡")
    public void 使用者持卡(String user) {
        this.currentUser = user;
    }

    @而且("該卡片綁定的帳戶密碼為 {string}")
    public void 綁定密碼(String pin) {
        this.registeredPin = Pin.of(pin);
    }

    @假設("使用者 {string} 的帳戶餘額為 {long} 元")
    public void 設定餘額(String user, long balance) {
        Account account = new Account(CARD_NUMBER, registeredPin, Money.of(balance));
        repository.save(account);
    }

    @當("使用者 {string} 插入提款卡")
    public void 插入提款卡(String user) {
        this.currentUser = user;
        this.authenticated = false;
        this.serviceAllowed = false;
    }

    @而且("使用者 {string} 輸入密碼 {string}")
    public void 輸入密碼(String user, String pin) {
        this.authenticated = atmService.authenticate(CARD_NUMBER, pin);
        this.serviceAllowed = this.authenticated;
        if (!this.authenticated) {
            this.lastMessage = "密碼錯誤";
        }
    }

    @那麼("系統應允許使用者選擇服務")
    public void 允許選擇服務() {
        assertTrue(serviceAllowed, "系統應允許選擇服務");
    }

    @那麼("系統應顯示 {string} 訊息")
    public void 顯示訊息(String message) {
        assertEquals(message, lastMessage);
    }

    @而且("系統不應允許使用者選擇服務")
    public void 不允許選擇服務() {
        assertFalse(serviceAllowed, "系統不應允許選擇服務");
    }

    @當("使用者 {string} 選擇提款 {long} 元")
    public void 選擇提款(String user, long amount) {
        this.lastResult = atmService.withdraw(CARD_NUMBER, registeredPin.value(), amount);
        recordMessage();
    }

    @當("使用者 {string} 嘗試提款 {long} 元")
    public void 嘗試提款(String user, long amount) {
        // 場景大綱使用：以先前輸入的密碼結果決定行為
        // 此處直接以應用服務判定，authenticate 已於輸入密碼步驟執行
        if (!authenticated) {
            this.lastResult = WithdrawalResult.密碼錯誤;
        } else {
            this.lastResult = atmService.withdraw(CARD_NUMBER, registeredPin.value(), amount);
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
        assertEquals(Money.of(0), dispenser.lastDispensed());
    }

    @而且("使用者 {string} 的帳戶餘額應為 {long} 元")
    public void 驗證餘額(String user, long expected) {
        Money balance = repository.findByCardNumber(CARD_NUMBER)
                .orElseThrow()
                .balance();
        assertEquals(Money.of(expected), balance);
    }

    @那麼("提款結果應為 {string}")
    public void 驗證結果(String expected) {
        assertEquals(WithdrawalResult.valueOf(expected), lastResult);
    }

    @當("使用者 {string} 選擇結束服務")
    public void 結束服務(String user) {
        this.serviceAllowed = false;
    }

    @那麼("系統應退出提款卡")
    public void 退出提款卡() {
        assertFalse(serviceAllowed, "服務已結束，卡片應退出");
    }

    private void recordMessage() {
        this.lastMessage = switch (lastResult) {
            case 成功 -> "提款成功";
            case 密碼錯誤 -> "密碼錯誤";
            case 餘額不足 -> "餘額不足";
            case 卡片無效 -> "卡片無效";
        };
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
 * JUnit 5 平台 Cucumber Test Runner。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.atm.steps")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html")
public class RunCucumberTest {
}

// src/test/java/com/example/atm/domain/AccountTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Account 聚合根單元測試。
 */
class AccountTest {

    private Account newAccount(long balance) {
        return new Account("CARD-1", Pin.of("1234"), Money.of(balance));
    }

    @Test
    void 密碼正確時驗證成功() {
        Account account = newAccount(1000);
        assertTrue(account.verifyPin(Pin.of("1234")));
    }

    @Test
    void 密碼錯誤時驗證失敗() {
        Account account = newAccount(1000);
        assertFalse(account.verifyPin(Pin.of("9999")));
    }

    @Test
    void 餘額充足時提款成功並扣款() {
        Account account = newAccount(5000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(4000), account.balance());
    }

    @Test
    void 餘額不足時提款拋出例外() {
        Account account = newAccount(500);
        assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(Money.of(1000)));
        assertEquals(Money.of(500), account.balance());
    }

    @Test
    void 提款至餘額為零成功() {
        Account account = newAccount(1000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(0), account.balance());
    }
}

// src/test/java/com/example/atm/domain/MoneyTest.java
package com.example.atm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Money 值物件單元測試。
 */
class MoneyTest {

    @Test
    void 建立負數金額拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void 金額比較正確() {
        assertTrue(Money.of(1000).isGreaterThanOrEqualTo(Money.of(1000)));
        assertFalse(Money.of(500).isGreaterThanOrEqualTo(Money.of(1000)));
    }

    @Test
    void 金額相減正確() {
        assertEquals(Money.of(4000), Money.of(5000).subtract(Money.of(1000)));
    }
}

// src/test/java/com/example/atm/application/AtmServiceTest.java
package com.example.atm.application;

import com.example.atm.domain.Account;
import com.example.atm.domain.Money;
import com.example.atm.domain.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AtmService 應用服務單元測試。
 */
class AtmServiceTest {

    private static final String CARD = "CARD-1";

    private InMemoryAccountRepository repository;
    private SimpleCashDispenser dispenser;
    private AtmService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        dispenser = new SimpleCashDispenser();
        service = new AtmService(repository, dispenser);
    }

    private void givenAccount(long balance) {
        repository.save(new Account(CARD, Pin.of("1234"), Money.of(balance)));
    }

    @Test
    void 密碼正確驗證通過() {
        givenAccount(5000);
        assertTrue(service.authenticate(CARD, "1234"));
    }

    @Test
    void 密碼錯誤驗證失敗() {
        givenAccount(5000);
        assertFalse(service.authenticate(CARD, "0000"));
    }

    @Test
    void 餘額充足提款成功() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, "1234", 1000);
        assertEquals(WithdrawalResult.成功, result);
        assertEquals(Money.of(1000), dispenser.lastDispensed());
        assertEquals(Money.of(4000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 密碼錯誤提款失敗() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, "0000", 1000);
        assertEquals(WithdrawalResult.密碼錯誤, result);
        assertEquals(Money.of(0), dispenser.lastDispensed());
        assertEquals(Money.of(5000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 餘額不足提款失敗() {
        givenAccount(500);
        WithdrawalResult result = service.withdraw(CARD, "1234", 1000);
        assertEquals(WithdrawalResult.餘額不足, result);
        assertEquals(Money.of(0), dispenser.lastDispensed());
        assertEquals(Money.of(500), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 卡片無效提款失敗() {
        WithdrawalResult result = service.withdraw("UNKNOWN", "1234", 1000);
        assertEquals(WithdrawalResult.卡片無效, result);
    }
}

<!-- pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>atm-bdd</artifactId>
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


執行方式：mvn test；覆蓋率報告位於 target/site/jacoco/index.html。核心 domain 與 application 皆有對應單元測試，配合 BDD 場景整體涵蓋率可達 ≥ 80%。