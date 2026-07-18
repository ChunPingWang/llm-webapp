package com.example.llmagent.application.port.out;

import java.util.List;
import java.util.Optional;

import com.example.llmagent.domain.provider.Provider;

/** Provider 註冊儲存 port(WP2-T1,ADR-001)。 */
public interface ProviderStore {

    void save(Provider provider);

    List<Provider> findAll();

    Optional<Provider> findById(String id);
}
