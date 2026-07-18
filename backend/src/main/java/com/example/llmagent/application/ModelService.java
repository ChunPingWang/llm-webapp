package com.example.llmagent.application;

import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.example.llmagent.application.port.out.ModelCatalogPort;
import com.example.llmagent.domain.provider.ModelInfo;

import reactor.core.publisher.Flux;

/**
 * 模型清單應用服務(WP2-T2)。將 Provider 回傳的模型排序:Claude 置前,便於使用者選用最新 Claude。
 */
@Service
public class ModelService {

    // Claude 置前,其餘依 id;同群組維持穩定排序。
    private static final Comparator<ModelInfo> ORDER =
            Comparator.comparing((ModelInfo m) -> m.id().toLowerCase().startsWith("claude") ? 0 : 1)
                    .thenComparing(ModelInfo::id);

    private final ModelCatalogPort port;

    public ModelService(ModelCatalogPort port) {
        this.port = port;
    }

    public Flux<ModelInfo> listModels(String providerId) {
        return port.listModels(providerId)
                .collectSortedList(ORDER)
                .flatMapMany(Flux::fromIterable);
    }
}
