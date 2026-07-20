package com.example.llmagent.application.port.out;

import java.util.Optional;

import com.example.llmagent.domain.chat.Conversation;

/**
 * 對話持久化 port(hexagonal out)。首個 vertical slice 以記憶體實作,
 * 後續替換為 PostgreSQL(WP1-T3)不影響 application 層。
 */
public interface ConversationStore {

    void save(Conversation conversation);

    Optional<Conversation> findById(String conversationId);

    /** 依 messageId 找到其所屬對話。 */
    Optional<Conversation> findByMessageId(String messageId);

    /** 刪除對話與其訊息;不存在時不動作(冪等)。 */
    void deleteById(String conversationId);
}
