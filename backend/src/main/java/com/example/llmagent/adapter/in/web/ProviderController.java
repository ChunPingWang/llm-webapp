package com.example.llmagent.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.application.ModelService;
import com.example.llmagent.application.ProviderService;
import com.example.llmagent.domain.provider.ModelInfo;
import com.example.llmagent.domain.provider.Provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import reactor.core.publisher.Flux;

/**
 * Provider 端點:註冊清單(WP2-T1)、動態模型清單(WP2-T2)、連線測試(WP2-T5)。
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ModelService modelService;
    private final ProviderService providerService;

    public ProviderController(ModelService modelService, ProviderService providerService) {
        this.modelService = modelService;
        this.providerService = providerService;
    }

    public record ProviderCreate(
            @NotBlank String id,
            @NotBlank String type,
            @NotBlank String baseUrl,
            String apiKeyRef,
            Boolean enabled) {
    }

    @GetMapping
    public List<Provider> list() {
        return providerService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Provider register(@Valid @RequestBody ProviderCreate req) {
        return providerService.register(new Provider(
                req.id(), Provider.ProviderType.valueOf(req.type()),
                req.baseUrl(), req.apiKeyRef(), req.enabled() == null || req.enabled()));
    }

    @PostMapping("/{providerId}/test")
    public reactor.core.publisher.Mono<ProviderService.TestResult> test(@PathVariable String providerId) {
        return providerService.testConnection(providerId);
    }

    @GetMapping("/{providerId}/models")
    public Flux<ModelInfo> models(@PathVariable String providerId) {
        return modelService.listModels(providerId);
    }
}
