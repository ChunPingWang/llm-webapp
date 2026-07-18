package com.example.atm.domain;

/**
 * 讀卡與退卡裝置介面（DIP、ISP）。
 */
public interface CardReader {
    void eject();
    boolean isEjected();
}
