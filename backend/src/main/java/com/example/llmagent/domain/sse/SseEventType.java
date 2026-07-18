package com.example.llmagent.domain.sse;

/**
 * SSE 事件型別(ADR-003 固定五型)。前端據 event 名稱分流至不同面板。
 *
 * <ul>
 *   <li>{@link #THINKING} — 模型思考過程({@code <think>} 由後端 ThinkingParser 解析)</li>
 *   <li>{@link #CONTENT}  — 正式回應內容</li>
 *   <li>{@link #TOOL_CALL}— 工具呼叫狀態</li>
 *   <li>{@link #LOG}      — 系統/Provider 日誌(INFO/WARN/ERROR)</li>
 *   <li>{@link #DONE}     — 串流結束,附 token 用量與延遲</li>
 * </ul>
 */
public enum SseEventType {
    THINKING("thinking"),
    CONTENT("content"),
    TOOL_CALL("tool_call"),
    LOG("log"),
    DONE("done");

    private final String wireName;

    SseEventType(String wireName) {
        this.wireName = wireName;
    }

    /** SSE {@code event:} 欄位使用的名稱。 */
    public String wireName() {
        return wireName;
    }
}
