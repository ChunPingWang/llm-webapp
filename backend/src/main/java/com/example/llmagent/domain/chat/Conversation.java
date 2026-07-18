package com.example.llmagent.domain.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 對話聚合根。持有訊息歷史、預設模型與 system prompt。
 *
 * <p>本類別為純 domain,不依賴 Spring。持久化由 adapter 負責。
 */
public class Conversation {

    private final String id;
    private final String title;
    private final String systemPrompt;
    private final String defaultModelId;
    private final Double temperature;
    private final Instant createdAt;
    private final List<Message> messages = new ArrayList<>();

    public Conversation(String id, String title, String systemPrompt,
                        String defaultModelId, Double temperature, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.systemPrompt = systemPrompt;
        this.defaultModelId = defaultModelId;
        this.temperature = temperature;
        this.createdAt = createdAt;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String defaultModelId() {
        return defaultModelId;
    }

    public Double temperature() {
        return temperature;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public Optional<Message> findMessage(String messageId) {
        return messages.stream().filter(m -> m.id().equals(messageId)).findFirst();
    }

    /** 回傳指定訊息(含)之前的歷史,供組裝 prompt 使用。 */
    public List<Message> historyUpToAndIncluding(String messageId) {
        List<Message> history = new ArrayList<>();
        for (Message m : messages) {
            history.add(m);
            if (m.id().equals(messageId)) {
                break;
            }
        }
        return history;
    }
}
