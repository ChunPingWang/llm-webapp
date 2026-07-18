package com.example.llmagent.domain.thinking;

import java.util.ArrayList;
import java.util.List;

/**
 * 後端統一解析模型輸出中的 {@code <think>...</think>} 區塊(ADR-003、CLAUDE.md #5)。
 *
 * <p>將串流文字增量分流為 {@link Segment.Kind 思考 / 內容} 片段。具狀態(stateful),
 * 每次對話建立一個實例。可正確處理跨 chunk 邊界被切斷的標籤(如 {@code "<thi"} + {@code "nk>"}),
 * 並容錯未閉合標籤(串流結束時以 {@link #flush()} 將殘餘視為內容輸出)。
 */
public class ThinkingParser {

    private static final String OPEN = "<think>";
    private static final String CLOSE = "</think>";

    private final StringBuilder buffer = new StringBuilder();
    private boolean inThinking = false;

    /** 餵入一段文字增量,回傳可立即輸出的片段(可能為空)。 */
    public List<Segment> accept(String delta) {
        buffer.append(delta);
        return drain(false);
    }

    /** 串流結束時呼叫:輸出所有殘餘緩衝(含未閉合標籤前的文字)。 */
    public List<Segment> flush() {
        return drain(true);
    }

    private List<Segment> drain(boolean atEnd) {
        List<Segment> out = new ArrayList<>();
        while (true) {
            String tag = inThinking ? CLOSE : OPEN;
            int idx = buffer.indexOf(tag);
            if (idx >= 0) {
                String before = buffer.substring(0, idx);
                if (!before.isEmpty()) {
                    out.add(segment(before));
                }
                buffer.delete(0, idx + tag.length());
                inThinking = !inThinking;
                continue;
            }
            // 無完整標籤:保留可能是標籤前綴的尾端,避免跨 chunk 誤切
            int keep = atEnd ? 0 : longestTagPrefixSuffix(tag);
            int emitLen = buffer.length() - keep;
            if (emitLen > 0) {
                String emit = buffer.substring(0, emitLen);
                out.add(segment(emit));
                buffer.delete(0, emitLen);
            }
            break;
        }
        return out;
    }

    private Segment segment(String text) {
        return inThinking ? Segment.thinking(text) : Segment.content(text);
    }

    /** 回傳 buffer 尾端同時是 {@code tag} 前綴的最長長度(0 表示無)。 */
    private int longestTagPrefixSuffix(String tag) {
        int max = Math.min(tag.length() - 1, buffer.length());
        for (int k = max; k >= 1; k--) {
            if (regionMatchesSuffix(tag, k)) {
                return k;
            }
        }
        return 0;
    }

    private boolean regionMatchesSuffix(String tag, int k) {
        int start = buffer.length() - k;
        for (int i = 0; i < k; i++) {
            if (buffer.charAt(start + i) != tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
