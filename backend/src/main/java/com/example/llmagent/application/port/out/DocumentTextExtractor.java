package com.example.llmagent.application.port.out;

/**
 * 上傳文件文字抽取 port:供對話附件將檔案內容併入提示。
 * 由 adapter 實作(.docx 以 Apache POI;純文字類直接解碼)。
 */
public interface DocumentTextExtractor {

    /** 是否支援此檔名(副檔名判斷)。 */
    boolean supports(String filename);

    /** 抽取純文字;不支援或解析失敗時擲出 {@link IllegalArgumentException}。 */
    String extractText(String filename, byte[] content);
}
