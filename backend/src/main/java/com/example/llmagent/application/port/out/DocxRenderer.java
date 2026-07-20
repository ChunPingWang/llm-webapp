package com.example.llmagent.application.port.out;

/**
 * 將 Markdown 文件渲染為 Word(.docx)位元組(WP6-T3,ADR-004)。
 * 由 adapter 以 Apache POI 實作,application 不感知具體技術。
 */
public interface DocxRenderer {

    /** @return .docx 檔案位元組 */
    byte[] render(String title, String markdown);

    /**
     * 以上傳的 Word 範本套版:置換 {@code {{title}}} 佔位,於 {@code {{content}}} 佔位處
     * 插入渲染後內文(無佔位則附加於範本之後),保留範本樣式與頁首頁尾。
     *
     * @return .docx 檔案位元組
     */
    byte[] renderWithTemplate(String title, String markdown, byte[] template);
}
