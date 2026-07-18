package com.example.llmagent.application.port.out;

import java.util.List;
import java.util.Optional;

import com.example.llmagent.domain.agent.AgentProfile;

/**
 * Agent Profile 儲存 port(ADR-006,append-only 版本化)。
 * {@code save} 永遠新增一列 {@code (id, version)},不覆寫舊版。
 */
public interface AgentProfileStore {

    void save(AgentProfile profile);

    /** 每個 id 的最新版本。 */
    List<AgentProfile> findAllLatest();

    Optional<AgentProfile> findLatest(String profileId);

    Optional<AgentProfile> findVersion(String profileId, int version);

    /** 指定 id 的全部版本,新→舊。 */
    List<AgentProfile> findVersions(String profileId);
}
