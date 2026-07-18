# ADR-005: 產出物(Artifact)獨立資料表並版本化

## Status
Accepted

## Context
Gherkin/Java 產出物需供下游 coding agent 消費,須可追溯、可下載、可比對版本。

## Decision
串流完成後由 `ArtifactService` 解析 code fence(```gherkin/```java),寫入 `artifacts(id, message_id, agent_profile_id, type, language, content, storage_key, version)`。同一對話中同名產出物遞增 version,提供 diff。下載格式:`.feature` / `.java` / `.md`。抽取失敗時整段降級為 MARKDOWN artifact 並記 WARN log。

## Consequences
- (+) 產出物可追溯至 message 與 agent profile 版本
- (−) fence parser 需容錯(模型格式偏差);以 system prompt 硬約束 + 降級策略處理
