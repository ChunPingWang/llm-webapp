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
