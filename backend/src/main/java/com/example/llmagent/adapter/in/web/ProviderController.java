package com.example.llmagent.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.application.ModelService;
import com.example.llmagent.domain.provider.ModelInfo;

import reactor.core.publisher.Flux;

/**
 * Provider 相關端點。目前提供動態模型清單(WP2-T2,openapi:/api/providers/{id}/models)。
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ModelService modelService;

    public ProviderController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping("/{providerId}/models")
    public Flux<ModelInfo> models(@PathVariable String providerId) {
        return modelService.listModels(providerId);
    }
}
