# ADR-001: Provider 抽象採 Spring AI ChatModel

## Status
Accepted

## Context
需串接 Ollama(native protocol :11434)、OpenAI-compatible(vLLM/LocalAI)、Anthropic 三類 Provider,並統一 streaming 介面。

## Decision
使用 Spring AI 1.0.x 的 `ChatModel` / `ChatClient` 抽象。各 Provider 以 Spring AI 官方 starter 接入,streaming 統一為 `Flux<ChatResponse>`。平台自訂 `ProviderRegistry` 管理多實例設定(baseUrl、apiKey ref、模型白名單),模型清單動態拉取(Ollama `/api/tags`、OpenAI `/v1/models`)。

## Consequences
- (+) 免自造 HTTP client 與 SSE 解析;新 Provider 成本低
- (+) 原生整合 Micrometer Observation(→ ADR-007)
- (−) 受 Spring AI 版本演進影響,升版需回歸測試
- 約束:業務程式碼不得繞過 ChatModel 直接呼叫模型 API
