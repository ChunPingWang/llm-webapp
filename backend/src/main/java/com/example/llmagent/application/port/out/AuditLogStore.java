package com.example.llmagent.application.port.out;

import java.time.Instant;
import java.util.List;

/** 稽核日誌儲存 port(WP4-T3):log/done 事件落地,供銀行客戶稽核查詢。 */
public interface AuditLogStore {

    record AuditEntry(long id, String conversationId, String level, String source,
                      String payload, Instant createdAt) {
    }

    void append(String conversationId, String level, String source, String payload);

    /** 依對話查詢(舊→新)。 */
    List<AuditEntry> findByConversation(String conversationId);
}
