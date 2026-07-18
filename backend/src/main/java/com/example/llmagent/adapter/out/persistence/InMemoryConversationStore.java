package com.example.llmagent.adapter.out.persistence;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.ConversationStore;
import com.example.llmagent.domain.chat.Conversation;

/**
 * 記憶體版對話儲存(首個 vertical slice)。後續由 PostgreSQL adapter 取代(WP1-T3)。
 */
@Repository
public class InMemoryConversationStore implements ConversationStore {

    private final ConcurrentHashMap<String, Conversation> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Conversation conversation) {
        byId.put(conversation.id(), conversation);
    }

    @Override
    public Optional<Conversation> findById(String conversationId) {
        return Optional.ofNullable(byId.get(conversationId));
    }

    @Override
    public Optional<Conversation> findByMessageId(String messageId) {
        return byId.values().stream()
                .filter(c -> c.findMessage(messageId).isPresent())
                .findFirst();
    }
}
