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
