package com.example.llmagent.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.AuditLogStore;

/** 記憶體版稽核日誌(WP4-T3)。 */
@Repository
public class InMemoryAuditLogStore implements AuditLogStore {

    private final CopyOnWriteArrayList<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong seq = new AtomicLong();

    @Override
    public void append(String conversationId, String level, String source, String payload) {
        entries.add(new AuditEntry(seq.incrementAndGet(), conversationId, level, source, payload, Instant.now()));
    }

    @Override
    public List<AuditEntry> findByConversation(String conversationId) {
        return entries.stream()
                .filter(e -> e.conversationId().equals(conversationId))
                .toList();
    }
}
