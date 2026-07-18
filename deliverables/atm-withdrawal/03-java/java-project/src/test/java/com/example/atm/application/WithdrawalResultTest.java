package com.example.atm.application;

import com.example.atm.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalResultTest {

    @Test
    void 成功結果欄位正確() {
        WithdrawalResult r = WithdrawalResult.ok(Money.of(1000), Money.of(4000));
        assertTrue(r.success());
        assertEquals(1000, r.dispensed().amount());
        assertEquals(4000, r.remainingBalance().amount());
        assertEquals("交易成功", r.message());
    }

    @Test
    void 失敗結果欄位正確() {
        WithdrawalResult r = WithdrawalResult.fail("餘額不足", Money.of(500));
        assertFalse(r.success());
        assertEquals(0, r.dispensed().amount());
        assertEquals("餘額不足", r.message());
    }
}
