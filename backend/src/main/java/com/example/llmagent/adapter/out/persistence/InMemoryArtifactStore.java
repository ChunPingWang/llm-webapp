package com.example.llmagent.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.ArtifactStore;
import com.example.llmagent.domain.artifact.Artifact;

/** 記憶體版產出物儲存(ADR-005)。 */
@Repository
public class InMemoryArtifactStore implements ArtifactStore {

    private record Entry(String conversationId, Artifact artifact) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final Map<String, Artifact> byId = new ConcurrentHashMap<>();

    @Override
    public void save(String conversationId, Artifact artifact) {
        entries.add(new Entry(conversationId, artifact));
        byId.put(artifact.id(), artifact);
    }

    @Override
    public List<Artifact> findByMessageId(String messageId) {
        return entries.stream()
                .map(Entry::artifact)
                .filter(a -> a.messageId().equals(messageId))
                .sorted(Comparator.comparing(Artifact::version))
                .toList();
    }

    @Override
    public Optional<Artifact> findById(String artifactId) {
        return Optional.ofNullable(byId.get(artifactId));
    }

    @Override
    public int nextVersion(String conversationId, Artifact.ArtifactType type) {
        return (int) entries.stream()
                .filter(e -> e.conversationId().equals(conversationId) && e.artifact().type() == type)
                .count() + 1;
    }

    @Override
    public List<Artifact> findByConversationAndType(String conversationId, Artifact.ArtifactType type) {
        return entries.stream()
                .filter(e -> e.conversationId().equals(conversationId) && e.artifact().type() == type)
                .map(Entry::artifact)
                .sorted(Comparator.comparing(Artifact::version))
                .toList();
    }
}
