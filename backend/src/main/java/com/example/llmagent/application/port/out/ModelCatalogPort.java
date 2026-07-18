package com.example.llmagent.application.port.out;

import com.example.llmagent.domain.provider.ModelInfo;

import reactor.core.publisher.Flux;

/**
 * 動態拉取 Provider 模型清單 port(WP2-T2)。實作對 OpenAI-Compatible {@code /v1/models}
 * 或 Ollama {@code /api/tags} 發出管理查詢(非模型推論路徑)。
 */
public interface ModelCatalogPort {

    /** 回傳指定 Provider 的模型清單;逾時或錯誤時以空串流快速失敗。 */
    Flux<ModelInfo> listModels(String providerId);
}
