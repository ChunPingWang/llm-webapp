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
