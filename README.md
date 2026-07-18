# llm-webapp — LLM Agent Web Platform

類 Open WebUI 的多模型 AI Agent 平台:串接 Ollama / OpenAI-compatible / Anthropic,
以 Agent Profile(system prompt 版本化)驅動對話,產出 Gherkin feature file 與 Java 程式碼,
支援 Word 預覽,對話串流即時呈現思考過程與日誌。

> 本專案依 `CLAUDE.md` 與 `docs/tasks/TASKS.md` 逐任務開發。完整規劃見 `docs/00-評估規劃書.md`,架構決策見 `docs/adr/`。

## 模型供應(本部署)

本部署透過 **IBM ICA(OpenAI-Compatible Gateway)** 呼叫 **最新 Claude 模型**。

| 項目 | 值 |
|------|-----|
| Provider 型別 | `OPENAI_COMPATIBLE`(ADR-001) |
| Base URL | `${ICA_API_URL}/v1` |
| API Key | `${ICA_CLAUDE_KEY}`(環境變數,禁止明碼) |
| 預設模型 | `claude-opus-4-8`(ICA 可用 Claude 中最新旗艦) |

環境變數由 `~/.zshrc` 提供:`ICA_API_URL`、`ICA_CLAUDE_KEY`。

## 專案結構

```
llm-webapp/
├── CLAUDE.md              # 開發指引與架構約束
├── backend/              # Spring Boot 3.3 + WebFlux(Hexagonal)
├── frontend/             # React 18 + TypeScript + Vite
├── specs/openapi.yaml    # API 契約(契約先行)
├── docker/compose.yaml   # postgres + minio + langfuse
└── docs/                 # 規劃書、ADR、TASKS
```

## 快速開始

前置:JDK 21、Node 20+、（選用）Podman/Docker。

```bash
# 後端(:8080)
cd backend && ./gradlew bootRun

# 前端(:5173,/api 代理至後端)
cd frontend && npm install && npm run dev

# 基礎設施(選用)
docker compose -f docker/compose.yaml up -d
```

開啟 http://localhost:5173,可執行「Ping 串流」驗證 SSE 管線。

## 開發狀態

依 `docs/tasks/TASKS.md`,以 vertical slice 逐步交付,每個重大步驟推送至 GitHub。

- [x] **Step 1 — 骨架(WP1-T1 / WP1-T4)**:monorepo、Hexagonal 後端骨架、SSE ping 管線、React/Vite 前端、docker compose
- [x] **Step 2 — ICA Provider + Chat 串流(WP3-T1 後端 / WP4-T1)**:透過 Spring AI 呼叫 ICA `claude-opus-4-8`,五型 SSE 事件、ThinkingParser、TTFT/token 用量;e2e 驗證通過
- [x] **Step 3 — Chat UI(WP3-T2 / WP4-T2 / WP5-T2 前端)**:三欄式介面(對話 / Artifacts / 日誌)、串流渲染、Thinking 摺疊區塊、Markdown + 語法高亮、Gherkin/Java 產出物抽取與下載、模型選擇器
- [x] **Step 4 — 動態模型清單(WP2-T2)**:`GET /api/providers/ica/models` 從 ICA `/v1/models` 拉取(3s timeout 快速失敗、Claude 置前),前端模型選擇器改為動態(後備清單容錯);WireMock 測試 + live 驗證
- [ ] 後續:Postgres/Flyway 落地(WP1-T3)、Agent Profile、後端 Artifact 版本化、Word 預覽、可觀測性、部署(見 `docs/tasks/TASKS.md`)

## 建置指令

```bash
cd backend  && ./gradlew build     # 編譯 + 測試
cd frontend && npm run build       # 型別檢查 + 打包
```
