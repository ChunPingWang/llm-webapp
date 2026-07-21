package com.example.llmagent.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * BRD 類「LLM 填寫式」Word 模板套版 port:於原模板 docx 上置換 {@code {{佔位符}}}、
 * 展開索引列({@code X_1}…{@code X_N})與重複區塊(◤重複區塊◢),刪除【填寫指引】與
 * 「模板使用說明」章,樣式與版面完全保留。
 */
public interface DocxTemplateFiller {

    /**
     * @param templateDocx 模板 .docx 位元組
     * @param values       佔位符值(鍵不含大括號;索引族以 _1/_2/… 具體鍵提供)
     * @param repeats      重複區塊資料,鍵為區塊名稱(如「需求場景」),每元素一份佔位符值
     * @return 填寫完成的 .docx 位元組
     */
    byte[] fill(byte[] templateDocx, Map<String, String> values,
                Map<String, List<Map<String, String>>> repeats);
}
