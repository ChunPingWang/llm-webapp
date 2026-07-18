# ADR-006: Agent Profile 與 System Prompt 版本化

## Status
Accepted

## Context
平台定位為 AI Agent:不同任務(BDD 規格、Java 產碼、Code Review)需不同 system prompt、模型與參數。銀行場景要求可追溯「產出物由哪版 prompt 生成」。

## Decision
引入 `agent_profiles(id, name, description, system_prompt, default_model_id, temperature, tools, version, enabled)`。
- System prompt 支援範本變數(`{project_name}`、`{gherkin_locale}`),以 Spring AI `PromptTemplate` 套用
- Prompt 內容修改即產生新 version(append-only),`messages` 記錄 `agent_profile_id` + `agent_profile_version`
- Prompt 主版本庫可託管於 Langfuse Prompt Management(→ ADR-007),平台快取並記錄取用版本;Langfuse 不可用時退回 DB 內容
- 對話建立時選定 Agent Profile;對話中可切換,逐訊息記錄

## Consequences
- (+) 產出物 ↔ prompt 版本完整追溯;prompt 調優可 A/B 比較
- (+) 與 SDLC 多 agent 架構(Orchestrator + sub-agents)對齊,本平台可作為 agent 前台
- (−) 增加一層設定管理 UI;Langfuse 整合增加部署元件
