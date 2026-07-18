富邦銀行 ATM 提款服務 — 完整專案實作

以下為符合 DDD 分層、SOLID 原則、Java 21 與 Cucumber BDD 的完整可編譯專案，測試涵蓋率設計目標 ≥ 80%。

專案結構
atm-bdd/
├── pom.xml
├── src/main/java/com/example/atm/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Money.java
│   │   │   ├── Pin.java
│   │   │   ├── CardNumber.java
│   │   │   └── Account.java
│   │   ├── exception/
│   │   │   ├── InsufficientFundsException.java
│   │   │   ├── InvalidPinFormatException.java
│   │   │   └── CardNotFoundException.java
│   │   └── port/
│   │       ├── AccountRepository.java
│   │       └── CashDispenser.java
│   └── application/
│       ├── WithdrawalResult.java
│       ├── AtmSession.java
│       └── AtmService.java
├── src/main/java/com/example/atm/infrastructure/
│   ├── InMemoryAccountRepository.java
│   └── SimpleCashDispenser.java
└── src/test/
    ├── java/com/example/atm/
    │   ├── RunCucumberTest.java
    │   ├── steps/AtmWithdrawalSteps.java
    │   ├── domain/model/{MoneyTest, PinTest, CardNumberTest, AccountTest}.java
    │   ├── application/AtmServiceTest.java
    │   └── infrastructure/{InMemoryAccountRepositoryTest, SimpleCashDispenserTest}.java
    └── resources/features/atm_withdrawal.feature

Production Code — Domain 層
// src/main/java/com/example/atm/domain/model/Money.java
package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 金額值物件（Value Object）。
 * 不可變、非負，封裝金額運算業務規則。
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

    public static Money zero() {
        return new Money(0);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        Objects.requireNonNull(other, "比較金額不可為 null");
        return this.amount >= other.amount;
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "扣除金額不可為 null");
        return new Money(this.amount - other.amount);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "增加金額不可為 null");
        return new Money(this.amount + other.amount);
    }
}

// src/main/java/com/example/atm/domain/model/Pin.java
package com.example.atm.domain.model;

import com.example.atm.domain.exception.InvalidPinFormatException;

import java.util.Objects;

/**
 * 密碼值物件（Value Object）。
 * 保證為 4 位數字格式。
 */
public record Pin(String value) {

    public Pin {
        Objects.requireNonNull(value, "密碼不可為 null");
        if (!value.matches("\\d{4}")) {
            throw new InvalidPinFormatException("密碼必須為 4 位數字, 收到: " + value);
        }
    }

    public static Pin of(String value) {
        return new Pin(value);
    }

    public boolean matches(Pin input) {
        return this.equals(input);
    }
}

// src/main/java/com/example/atm/domain/model/CardNumber.java
package com.example.atm.domain.model;

import java.util.Objects;

/**
 * 卡號值物件（Value Object）。
 */
public record CardNumber(String value) {

    public CardNumber {
        Objects.requireNonNull(value, "卡號不可為 null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("卡號不可為空白");
        }
    }

    public static CardNumber of(String value) {
        return new CardNumber(value);
    }
}

// src/main/java/com/example/atm/domain/model/Account.java
package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientFundsException;

import java.util.Objects;

/**
 * 帳戶聚合根（Aggregate Root）。
 * 負責密碼驗證與提款扣款之核心業務規則，維護聚合內部一致性。
 */
public class Account {

    private final CardNumber cardNumber;
    private final Pin pin;
    private Money balance;

    public Account(CardNumber cardNumber, Pin pin, Money balance) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不可為 null");
        this.pin = Objects.requireNonNull(pin, "密碼不可為 null");
        this.balance = Objects.requireNonNull(balance, "餘額不可為 null");
    }

    public boolean verifyPin(Pin input) {
        Objects.requireNonNull(input, "輸入密碼不可為 null");
        return pin.matches(input);
    }

    public boolean canWithdraw(Money amount) {
        Objects.requireNonNull(amount, "提款金額不可為 null");
        return amount.isPositive() && balance.isGreaterThanOrEqualTo(amount);
    }

    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "提款金額不可為 null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("提款金額必須大於 0");
        }
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException("餘額不足, 目前餘額: " + balance.amount());
        }
        this.balance = balance.subtract(amount);
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }

    public Money balance() {
        return balance;
    }
}

// src/main/java/com/example/atm/domain/exception/InsufficientFundsException.java
package com.example.atm.domain.exception;

/**
 * 餘額不足領域例外。
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/exception/InvalidPinFormatException.java
package com.example.atm.domain.exception;

/**
 * 密碼格式錯誤領域例外。
 */
public class InvalidPinFormatException extends RuntimeException {

    public InvalidPinFormatException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/exception/CardNotFoundException.java
package com.example.atm.domain.exception;

/**
 * 卡片查無帳戶領域例外。
 */
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String message) {
        super(message);
    }
}

// src/main/java/com/example/atm/domain/port/AccountRepository.java
package com.example.atm.domain.port;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;

import java.util.Optional;

/**
 * 帳戶儲存庫埠（Port）。
 * 由 infrastructure 實作，符合 DIP 與 ISP。
 */
public interface AccountRepository {

    Optional<Account> findByCardNumber(CardNumber cardNumber);

    void save(Account account);
}

// src/main/java/com/example/atm/domain/port/CashDispenser.java
package com.example.atm.domain.port;

import com.example.atm.domain.model.Money;

/**
 * 出鈔裝置埠（Port）。
 * 抽象化硬體行為，符合 DIP。
 */
public interface CashDispenser {

    void dispense(Money amount);

    Money lastDispensed();
}

Production Code — Application 層
// src/main/java/com/example/atm/application/WithdrawalResult.java
package com.example.atm.application;

/**
 * 提款結果狀態列舉。
 */
public enum WithdrawalResult {
    成功,
    密碼錯誤,
    餘額不足,
    卡片無效,
    金額無效
}

// src/main/java/com/example/atm/application/AtmSession.java
package com.example.atm.application;

import com.example.atm.domain.model.CardNumber;

import java.util.Objects;

/**
 * ATM 交易會話（Application 層狀態物件）。
 * 表達卡片插入與驗證狀態，符合 SRP。
 */
public class AtmSession {

    private final CardNumber cardNumber;
    private boolean authenticated;
    private boolean active;

    private AtmSession(CardNumber cardNumber) {
        this.cardNumber = Objects.requireNonNull(cardNumber, "卡號不可為 null");
        this.authenticated = false;
        this.active = true;
    }

    public static AtmSession start(CardNumber cardNumber) {
        return new AtmSession(cardNumber);
    }

    public void markAuthenticated() {
        this.authenticated = true;
    }

    public boolean canSelectService() {
        return active && authenticated;
    }

    public void end() {
        this.active = false;
        this.authenticated = false;
    }

    public boolean isActive() {
        return active;
    }

    public CardNumber cardNumber() {
        return cardNumber;
    }
}

// src/main/java/com/example/atm/application/AtmService.java
package com.example.atm.application;

import com.example.atm.domain.exception.InsufficientFundsException;
import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.domain.port.AccountRepository;
import com.example.atm.domain.port.CashDispenser;

import java.util.Objects;
import java.util.Optional;

/**
 * ATM 應用服務（Application Service）。
 * 協調領域物件完成提款用例，僅依賴抽象埠（DIP）。
 */
public class AtmService {

    private final AccountRepository accountRepository;
    private final CashDispenser cashDispenser;

    public AtmService(AccountRepository accountRepository, CashDispenser cashDispenser) {
        this.accountRepository = Objects.requireNonNull(accountRepository, "儲存庫不可為 null");
        this.cashDispenser = Objects.requireNonNull(cashDispenser, "出鈔裝置不可為 null");
    }

    /**
     * 驗證密碼並回傳會話；密碼正確時會話標記為已驗證。
     */
    public AtmSession authenticate(CardNumber cardNumber, Pin pin) {
        AtmSession session = AtmSession.start(cardNumber);
        accountRepository.findByCardNumber(cardNumber)
                .filter(account -> account.verifyPin(pin))
                .ifPresent(account -> session.markAuthenticated());
        return session;
    }

    /**
     * 執行提款用例，回傳結果狀態。
     * 交易保證原子性：先扣款成功才出鈔並持久化。
     */
    public WithdrawalResult withdraw(CardNumber cardNumber, Pin pin, Money amount) {
        if (!amount.isPositive()) {
            return WithdrawalResult.金額無效;
        }

        Optional<Account> maybeAccount = accountRepository.findByCardNumber(cardNumber);
        if (maybeAccount.isEmpty()) {
            return WithdrawalResult.卡片無效;
        }

        Account account = maybeAccount.get();
        if (!account.verifyPin(pin)) {
            return WithdrawalResult.密碼錯誤;
        }

        try {
            account.withdraw(amount);
            cashDispenser.dispense(amount);
            accountRepository.save(account);
            return WithdrawalResult.成功;
        } catch (InsufficientFundsException e) {
            return WithdrawalResult.餘額不足;
        }
    }
}

Production Code — Infrastructure 層
// src/main/java/com/example/atm/infrastructure/InMemoryAccountRepository.java
package com.example.atm.infrastructure;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.port.AccountRepository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 記憶體版帳戶儲存庫實作（Adapter）。
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<String, Account> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findByCardNumber(CardNumber cardNumber) {
        if (cardNumber == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(cardNumber.value()));
    }

    @Override
    public void save(Account account) {
        store.put(account.cardNumber().value(), account);
    }
}

// src/main/java/com/example/atm/infrastructure/SimpleCashDispenser.java
package com.example.atm.infrastructure;

import com.example.atm.domain.model.Money;
import com.example.atm.domain.port.CashDispenser;

/**
 * 簡易出鈔裝置實作（Adapter）。
 */
public class SimpleCashDispenser implements CashDispenser {

    private Money lastDispensed = Money.zero();

    @Override
    public void dispense(Money amount) {
        this.lastDispensed = amount;
    }

    @Override
    public Money lastDispensed() {
        return lastDispensed;
    }
}

Test Code — Cucumber Feature
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

Test Code — Step Definitions 與 Runner
// src/test/java/com/example/atm/steps/AtmWithdrawalSteps.java
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

Test Code — 單元測試（Domain）
// src/test/java/com/example/atm/domain/model/MoneyTest.java
package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void 建立負數金額拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(-1));
    }

    @Test
    void zero工廠回傳零金額() {
        assertEquals(Money.of(0), Money.zero());
    }

    @Test
    void 金額大於等於比較正確() {
        assertTrue(Money.of(1000).isGreaterThanOrEqualTo(Money.of(1000)));
        assertTrue(Money.of(2000).isGreaterThanOrEqualTo(Money.of(1000)));
        assertFalse(Money.of(500).isGreaterThanOrEqualTo(Money.of(1000)));
    }

    @Test
    void isPositive判斷正確() {
        assertTrue(Money.of(1).isPositive());
        assertFalse(Money.zero().isPositive());
    }

    @Test
    void 金額相減正確() {
        assertEquals(Money.of(4000), Money.of(5000).subtract(Money.of(1000)));
    }

    @Test
    void 金額相加正確() {
        assertEquals(Money.of(6000), Money.of(5000).add(Money.of(1000)));
    }

    @Test
    void 相減為負拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(500).subtract(Money.of(1000)));
    }
}

// src/test/java/com/example/atm/domain/model/PinTest.java
package com.example.atm.domain.model;

import com.example.atm.domain.exception.InvalidPinFormatException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinTest {

    @Test
    void 合法四位數密碼建立成功() {
        assertTrue(Pin.of("1234").matches(Pin.of("1234")));
    }

    @Test
    void 密碼不符時比對失敗() {
        assertFalse(Pin.of("1234").matches(Pin.of("9999")));
    }

    @Test
    void 非四位數密碼拋出例外() {
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("123"));
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("12345"));
        assertThrows(InvalidPinFormatException.class, () -> Pin.of("abcd"));
    }

    @Test
    void 空值密碼拋出例外() {
        assertThrows(NullPointerException.class, () -> Pin.of(null));
    }
}

// src/test/java/com/example/atm/domain/model/CardNumberTest.java
package com.example.atm.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CardNumberTest {

    @Test
    void 合法卡號建立成功() {
        assertEquals("FUBON-001", CardNumber.of("FUBON-001").value());
    }

    @Test
    void 空白卡號拋出例外() {
        assertThrows(IllegalArgumentException.class, () -> CardNumber.of("  "));
    }

    @Test
    void 空值卡號拋出例外() {
        assertThrows(NullPointerException.class, () -> CardNumber.of(null));
    }
}

// src/test/java/com/example/atm/domain/model/AccountTest.java
package com.example.atm.domain.model;

import com.example.atm.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private Account newAccount(long balance) {
        return new Account(CardNumber.of("CARD-1"), Pin.of("1234"), Money.of(balance));
    }

    @Test
    void 密碼正確時驗證成功() {
        assertTrue(newAccount(1000).verifyPin(Pin.of("1234")));
    }

    @Test
    void 密碼錯誤時驗證失敗() {
        assertFalse(newAccount(1000).verifyPin(Pin.of("9999")));
    }

    @Test
    void canWithdraw餘額充足回傳true() {
        assertTrue(newAccount(5000).canWithdraw(Money.of(1000)));
    }

    @Test
    void canWithdraw餘額不足回傳false() {
        assertFalse(newAccount(500).canWithdraw(Money.of(1000)));
    }

    @Test
    void canWithdraw零金額回傳false() {
        assertFalse(newAccount(5000).canWithdraw(Money.zero()));
    }

    @Test
    void 餘額充足時提款成功並扣款() {
        Account account = newAccount(5000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.of(4000), account.balance());
    }

    @Test
    void 提款至餘額為零成功() {
        Account account = newAccount(1000);
        account.withdraw(Money.of(1000));
        assertEquals(Money.zero(), account.balance());
    }

    @Test
    void 餘額不足時提款拋出例外且不異動餘額() {
        Account account = newAccount(500);
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(Money.of(1000)));
        assertEquals(Money.of(500), account.balance());
    }

    @Test
    void 提款零金額拋出例外() {
        Account account = newAccount(5000);
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(Money.zero()));
    }

    @Test
    void 卡號可取得() {
        assertEquals(CardNumber.of("CARD-1"), newAccount(1000).cardNumber());
    }
}

Test Code — 單元測試（Application 與 Infrastructure）
// src/test/java/com/example/atm/application/AtmServiceTest.java
package com.example.atm.application;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import com.example.atm.infrastructure.InMemoryAccountRepository;
import com.example.atm.infrastructure.SimpleCashDispenser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmServiceTest {

    private static final CardNumber CARD = CardNumber.of("CARD-1");
    private static final Pin CORRECT_PIN = Pin.of("1234");
    private static final Pin WRONG_PIN = Pin.of("0000");

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
        repository.save(new Account(CARD, CORRECT_PIN, Money.of(balance)));
    }

    @Test
    void 密碼正確驗證會話通過() {
        givenAccount(5000);
        AtmSession session = service.authenticate(CARD, CORRECT_PIN);
        assertTrue(session.canSelectService());
    }

    @Test
    void 密碼錯誤驗證會話失敗() {
        givenAccount(5000);
        AtmSession session = service.authenticate(CARD, WRONG_PIN);
        assertFalse(session.canSelectService());
    }

    @Test
    void 卡片不存在驗證會話失敗() {
        AtmSession session = service.authenticate(CARD, CORRECT_PIN);
        assertFalse(session.canSelectService());
    }

    @Test
    void 餘額充足提款成功() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.成功, result);
        assertEquals(Money.of(1000), dispenser.lastDispensed());
        assertEquals(Money.of(4000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 密碼錯誤提款失敗且不出鈔不扣款() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, WRONG_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.密碼錯誤, result);
        assertEquals(Money.zero(), dispenser.lastDispensed());
        assertEquals(Money.of(5000), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 餘額不足提款失敗且不出鈔不扣款() {
        givenAccount(500);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.餘額不足, result);
        assertEquals(Money.zero(), dispenser.lastDispensed());
        assertEquals(Money.of(500), repository.findByCardNumber(CARD).orElseThrow().balance());
    }

    @Test
    void 提款零金額回傳金額無效() {
        givenAccount(5000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.zero());
        assertEquals(WithdrawalResult.金額無效, result);
    }

    @Test
    void 卡片無效提款失敗() {
        WithdrawalResult result = service.withdraw(CardNumber.of("UNKNOWN"), CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.卡片無效, result);
    }

    @Test
    void 提款至餘額為零成功() {
        givenAccount(1000);
        WithdrawalResult result = service.withdraw(CARD, CORRECT_PIN, Money.of(1000));
        assertEquals(WithdrawalResult.成功, result);
        assertEquals(Money.zero(), repository.findByCardNumber(CARD).orElseThrow().balance());
    }
}

// src/test/java/com/example/atm/application/AtmSessionTest.java
package com.example.atm.application;

import com.example.atm.domain.model.CardNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmSessionTest {

    private static final CardNumber CARD = CardNumber.of("CARD-1");

    @Test
    void 新會話尚未驗證不可選擇服務() {
        AtmSession session = AtmSession.start(CARD);
        assertTrue(session.isActive());
        assertFalse(session.canSelectService());
    }

    @Test
    void 驗證後可選擇服務() {
        AtmSession session = AtmSession.start(CARD);
        session.markAuthenticated();
        assertTrue(session.canSelectService());
    }

    @Test
    void 結束後不可選擇服務且非活躍() {
        AtmSession session = AtmSession.start(CARD);
        session.markAuthenticated();
        session.end();
        assertFalse(session.isActive());
        assertFalse(session.canSelectService());
    }

    @Test
    void 會話保存卡號() {
        AtmSession session = AtmSession.start(CARD);
        assertEquals(CARD, session.cardNumber());
    }
}

// src/test/java/com/example/atm/infrastructure/InMemoryAccountRepositoryTest.java
package com.example.atm.infrastructure;

import com.example.atm.domain.model.Account;
import com.example.atm.domain.model.CardNumber;
import com.example.atm.domain.model.Money;
import com.example.atm.domain.model.Pin;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAccountRepositoryTest {

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();

    @Test
    void 儲存後可依卡號查詢() {
        Account account = new Account(CardNumber.of("CARD-1"), Pin.of("1234"), Money.of(1000));
        repository.save(account);
        Optional<Account> found = repository.findByCardNumber(CardNumber.of("CARD-1"));
        assertTrue(found.isPresent());
        assertEquals(Money.of(1000), found.get().balance());
    }

    @Test
    void 查詢不存在卡號回傳空() {
        assertTrue(repository.findByCardNumber(CardNumber.of("NONE")).isEmpty());
    }

    @Test
    void 查詢null卡號回傳空() {
        assertTrue(repository.findByCardNumber(null).isEmpty());
    }
}

// src/test/java/com/example/atm/infrastructure/SimpleCashDispenserTest.java
package com.example.atm.infrastructure;

import com.example.atm.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleCashDispenserTest {

    @Test
    void 初始未出鈔為零() {
        assertEquals(Money.zero(), new SimpleCashDispenser().lastDispensed());
    }

    @Test
    void 出鈔後記錄最後金額() {
        SimpleCashDispenser dispenser = new SimpleCashDispenser();
        dispenser.dispense(Money.of(1000));
        assertEquals(Money.of(1000), dispenser.lastDispensed());
    }
}

建置設定
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
        <junit.platform.version>1.10.2</junit.platform.version>
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
            <version>${junit.platform.version}</version>
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
                                            <counter>INSTRUCTION</counter>
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

執行與覆蓋率驗證
目的	指令
執行所有測試	mvn test
執行測試並驗證覆蓋率門檻（≥ 80%）	mvn verify
檢視覆蓋率報告	開啟 target/site/jacoco/index.html
檢視 BDD 報告	開啟 target/cucumber-report.html
涵蓋率設計說明
分層	主要類別	測試來源	涵蓋重點
Domain	Money	MoneyTest	正負值、加減、比較全分支
Domain	Pin	PinTest	格式驗證正反向
Domain	CardNumber	CardNumberTest	空白/空值防呆
Domain	Account	AccountTest	驗證、扣款、例外全分支
Application	AtmService	AtmServiceTest + BDD	五種結果狀態全覆蓋
Application	AtmSession	AtmSessionTest	狀態轉換全覆蓋
Infrastructure	Repository / Dispenser	對應單元測試	CRUD 與邊界

透過單元測試涵蓋所有分支邏輯，加上 Cucumber 端對端情境（正向、密碼錯誤、餘額不足、邊界歸零），指令碼與領域邏輯的 INSTRUCTION 覆蓋率可穩定達成 ≥ 80%，並由 JaCoCo check 目標自動把關。