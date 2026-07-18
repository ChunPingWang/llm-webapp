package com.example.llmagent.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.AgentProfileStore;
import com.example.llmagent.domain.agent.AgentProfile;

/** 記憶體版 Agent Profile 儲存;versions 以 map 保存,append-only。 */
@Repository
public class InMemoryAgentProfileStore implements AgentProfileStore {

    private final Map<String, ConcurrentSkipListMap<Integer, AgentProfile>> byId = new ConcurrentHashMap<>();

    @Override
    public void save(AgentProfile profile) {
        byId.computeIfAbsent(profile.id(), k -> new ConcurrentSkipListMap<>())
                .put(profile.version(), profile);
    }

    @Override
    public List<AgentProfile> findAllLatest() {
        return byId.values().stream()
                .map(versions -> versions.lastEntry().getValue())
                .sorted(Comparator.comparing(AgentProfile::name))
                .toList();
    }

    @Override
    public Optional<AgentProfile> findLatest(String profileId) {
        var versions = byId.get(profileId);
        return versions == null || versions.isEmpty()
                ? Optional.empty()
                : Optional.of(versions.lastEntry().getValue());
    }

    @Override
    public Optional<AgentProfile> findVersion(String profileId, int version) {
        var versions = byId.get(profileId);
        return versions == null ? Optional.empty() : Optional.ofNullable(versions.get(version));
    }

    @Override
    public List<AgentProfile> findVersions(String profileId) {
        var versions = byId.get(profileId);
        return versions == null ? List.of()
                : versions.descendingMap().values().stream().toList();
    }
}
