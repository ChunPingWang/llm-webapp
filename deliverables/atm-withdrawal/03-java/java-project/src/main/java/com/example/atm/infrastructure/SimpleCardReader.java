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
