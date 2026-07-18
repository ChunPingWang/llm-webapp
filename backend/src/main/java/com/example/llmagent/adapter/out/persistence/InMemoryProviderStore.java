package com.example.llmagent.adapter.out.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.llmagent.application.port.out.ProviderStore;
import com.example.llmagent.domain.provider.Provider;

/** 記憶體版 Provider 儲存。 */
@Repository
public class InMemoryProviderStore implements ProviderStore {

    private final Map<String, Provider> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Provider provider) {
        byId.put(provider.id(), provider);
    }

    @Override
    public List<Provider> findAll() {
        return byId.values().stream().sorted(Comparator.comparing(Provider::id)).toList();
    }

    @Override
    public Optional<Provider> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
