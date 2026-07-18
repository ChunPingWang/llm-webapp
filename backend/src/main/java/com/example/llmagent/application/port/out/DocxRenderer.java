package com.example.llmagent.application.port.out;

/**
 * 將 Markdown 文件渲染為 Word(.docx)位元組(WP6-T3,ADR-004)。
 * 由 adapter 以 Apache POI 實作,application 不感知具體技術。
 */
public interface DocxRenderer {

    /** @return .docx 檔案位元組 */
    byte[] render(String title, String markdown);
}
