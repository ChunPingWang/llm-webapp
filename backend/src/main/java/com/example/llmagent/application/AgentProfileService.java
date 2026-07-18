package com.example.llmagent.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.llmagent.application.port.out.AgentProfileStore;
import com.example.llmagent.domain.agent.AgentProfile;
import com.example.llmagent.domain.agent.PromptTemplate;

/**
 * Agent Profile 應用服務(WP2-T3/T4,ADR-006)。
 * 建立時 version=1;更新一律 append 新版本;prompt 套用 {@link PromptTemplate} 變數。
 */
@Service
public class AgentProfileService {

    private final AgentProfileStore store;

    public AgentProfileService(AgentProfileStore store) {
        this.store = store;
    }

    public AgentProfile create(String name, String description, String systemPrompt,
                               String defaultModelId, Double temperature, List<String> tools) {
        AgentProfile p = new AgentProfile(
                UUID.randomUUID().toString(), 1, name, description, systemPrompt,
                defaultModelId, temperature, tools == null ? List.of() : tools,
                true, Instant.now());
        store.save(p);
        return p;
    }

    /** 更新 → append 新版本(舊版不動,可追溯)。 */
    public AgentProfile update(String profileId, String name, String description, String systemPrompt,
                               String defaultModelId, Double temperature, List<String> tools) {
        AgentProfile latest = store.findLatest(profileId)
                .orElseThrow(() -> new IllegalArgumentException("agent profile not found: " + profileId));
        AgentProfile next = latest.nextVersion(
                name != null && !name.isBlank() ? name : latest.name(),
                description != null ? description : latest.description(),
                systemPrompt != null && !systemPrompt.isBlank() ? systemPrompt : latest.systemPrompt(),
                defaultModelId != null && !defaultModelId.isBlank() ? defaultModelId : latest.defaultModelId(),
                temperature != null ? temperature : latest.temperature(),
                tools != null ? tools : latest.tools(),
                Instant.now());
        store.save(next);
        return next;
    }

    public List<AgentProfile> listLatest() {
        return store.findAllLatest();
    }

    public Optional<AgentProfile> findLatest(String profileId) {
        return store.findLatest(profileId);
    }

    public List<AgentProfile> versions(String profileId) {
        return store.findVersions(profileId);
    }

    /**
     * 解析 Profile 的 system prompt(套用範本變數)。
     *
     * @throws PromptTemplate.MissingVariableException 缺變數時
     */
    public String renderPrompt(String profileId, Map<String, String> variables) {
        AgentProfile p = store.findLatest(profileId)
                .orElseThrow(() -> new IllegalArgumentException("agent profile not found: " + profileId));
        return PromptTemplate.render(p.systemPrompt(), variables == null ? Map.of() : variables);
    }
}
