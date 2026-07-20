# CLAUDE.md — LLM Agent Web Platform

> 本檔為 Claude Code 開發指引。開始任何任務前先讀本檔與 `tasks/TASKS.md`。

## 專案概述

類 Open WebUI 的多模型 AI Agent 平台:串接 Ollama / OpenAI-compatible / Anthropic,
以 Agent Profile(system prompt 版本化)驅動對話,產出 Gherkin feature file 與 Java 程式碼(Markdown 呈現),
支援 Word (.docx) 瀏覽器預覽,對話串流即時呈現模型思考過程(thinking)與日誌。
可觀測性以 Langfuse(OTel)收斂。

完整規劃見 `00-評估規劃書.md`,架構決策見 `adr/`。

## 技術棧(固定,勿自行升降版)

| 層 | 技術 | 版本 |
|----|------|------|
| Backend | Java + Spring Boot | Java 21, Spring Boot 3.3.x |
| AI 抽象 | Spring AI | 1.0.x(ChatClient / ChatModel) |
| 串流 | SSE(Reactor `Flux<ServerSentEvent>`) | — |
| DB | PostgreSQL | 16 |
| 物件儲存 | MinIO(S3 API) | — |
| Frontend | React + TypeScript + Vite | React 18, TS 5.x |
| Markdown 渲染 | react-markdown + Shiki | — |
| Word 預覽 | docx-preview(前端) | — |
| Word 產生 | Apache POI(後端) | — |
| 可觀測性 | Micrometer → OTel → Langfuse(自架) | — |
| 測試 | JUnit 5, Cucumber-JVM(zh-TW Gherkin), Testcontainers, WireMock | — |

## 架構約束(必須遵守)

1. **Hexagonal Architecture**:`domain` 不依賴 Spring;Provider、DB、MinIO 皆為 adapter
   - 套件結構:`com.example.llmagent.{domain,application,adapter.in.web,adapter.out.{persistence,provider,storage}}`
2. **BDD-first**:每個功能任務先寫/確認 `.feature`(zh-TW Gherkin,關鍵字用 `功能:/場景:/假設/當/那麼`),再實作
3. **Provider 抽象只透過 Spring AI ChatModel 介面**,不得直接呼叫 HTTP client 打模型 API(WireMock 測試除外)
4. **SSE 事件協定**固定五型:`thinking` / `content` / `tool_call` / `log` / `done`(schema 見 `specs/openapi.yaml`)
5. **`<think>` 解析在後端** `ThinkingParser` 統一處理,前端不解析原始標籤
6. **Artifact 抽取**:串流完成後解析 code fence(```gherkin / ```java),寫入 `artifacts` 表並版本化
7. **API 契約先行**:所有 REST 端點以 `specs/openapi.yaml` 為準,新端點先改 spec 再實作
8. API Key 一律走環境變數 / K8s Secret,禁止寫入程式碼或設定檔明碼

## Monorepo 結構

```
gherkins-converter/
├── CLAUDE.md
├── backend/          # Spring Boot(Gradle Kotlin DSL)
├── frontend/         # React + Vite
├── docs/             # 本文件包
├── specs/openapi.yaml
└── docker/compose.yaml   # postgres + minio + ollama + langfuse
```

## 常用指令

```bash
# Backend
cd backend && ./gradlew test          # 單元 + Cucumber 測試
cd backend && ./gradlew bootRun       # 本機啟動(需 docker compose up)

# Frontend
cd frontend && npm run dev
cd frontend && npm run test && npm run lint

# 基礎設施
docker compose -f docker/compose.yaml up -d
```

## Definition of Done(每個任務)

- [ ] 對應 `.feature` 場景全數通過(Cucumber)
- [ ] 單元測試覆蓋 domain 邏輯;adapter 以 Testcontainers/WireMock 測試
- [ ] `specs/openapi.yaml` 與實作一致
- [ ] 無 lint 錯誤;無明碼 secret
- [ ] `tasks/TASKS.md` 勾選完成並註記 commit

## 工作流程

1. 從 `tasks/TASKS.md` 認領任務(依 Phase 與依賴順序)
2. 讀該任務引用的 ADR 與 spec 段落
3. 先寫/更新 `.feature` → 實作 → 測試綠燈 → 勾選任務
4. 一次只做一個任務,commit message 格式:`feat(WPx-Ty): 描述`
