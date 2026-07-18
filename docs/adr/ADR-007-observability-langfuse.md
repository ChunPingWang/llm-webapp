# ADR-007: 可觀測性採 Langfuse(經 OpenTelemetry),不採 LangSmith

## Status
Accepted

## Context
需要 LLM 呼叫層級的 trace(prompt、completion、token、延遲、成本)、prompt 版本比較與 evaluation。候選:Langfuse、LangSmith。

## Decision
採 Langfuse(自架,Docker/K8s)。整合路徑:Spring AI 內建 Micrometer Observation → OTel exporter(OTLP)→ Langfuse endpoint。
- 平台內「日誌面板」(即時、給使用者)與 Langfuse(離線分析、給團隊)並存,前者由 SSE `log` 事件驅動,後者由 OTel 匯出
- Langfuse Prompt Management 作為 Agent Profile prompt 版本庫(→ ADR-006)
- 不採 LangSmith:生態偏 LangChain/Python,且自架選項受限,不利銀行客戶資料落地要求

## Consequences
- (+) 開源可自架、資料不出環境;與 Spring AI 為官方支援路徑
- (+) trace 可關聯 conversation_id / agent_profile_version(以 OTel attributes 傳遞)
- (−) 多一個部署元件(Langfuse + ClickHouse/Postgres);納入 docker compose 與 K8s manifests
