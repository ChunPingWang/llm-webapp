# TASKS.md — 開發任務清單

> Claude Code 使用方式:依 Phase 順序認領任務,完成後勾選並註記 commit hash。
> 每個任務先寫/確認 `.feature` 再實作(見 CLAUDE.md Definition of Done)。
> 依賴標記:`deps: []` 為前置任務。

---

## Phase 0 — 基礎框架(WP1)

- [x] **WP1-T1** Monorepo 初始化(backend Gradle Kotlin DSL + frontend Vite)`deps: []` — Step 1
  - 驗收:`./gradlew test` 與 `npm run dev` 可執行;Hexagonal 套件骨架建立 ✅
- [ ] **WP1-T2** docker/compose.yaml(postgres:16 + minio + ollama + langfuse)`deps: []`
  - 驗收:`docker compose up -d` 後四服務健康;README 記載各服務 port
- [ ] **WP1-T3** Flyway migration V1(conversations, messages, artifacts, providers, agent_profiles, audit_logs)`deps: [WP1-T1]`
  - 驗收:migration 於 Testcontainers Postgres 通過;schema 與規劃書 §6 一致
- [x] **WP1-T4** SSE 骨架:`GET /api/ping/stream` 回傳心跳事件 `deps: [WP1-T1]` — Step 1
  - 驗收:curl 可收到 event-stream;WebTestClient 測試通過 ✅

## Phase 1 — Provider 與 Agent Profile(WP2)

- [ ] **WP2-T1** ProviderRegistry + 設定載入(OLLAMA / OPENAI_COMPATIBLE / ANTHROPIC)`deps: [WP1-T3]`
  - 驗收:三型 Provider 可註冊;apiKey 僅接受環境變數參照(ADR-001)
- [x] **WP2-T2** 模型清單動態拉取 API `GET /api/providers/{id}/models` `deps: [WP2-T1]` — Step 4
  - 驗收:WireMock 模擬 Ollama `/api/tags` 與 OpenAI `/v1/models`;timeout 3s 快速失敗 ✅
  - 實作:IcaModelCatalogAdapter(WebClient,3s timeout,錯誤→空串流)、ModelService(Claude 置前)、ProviderController;WireMock 4 例(解析/未知 provider/500 快速失敗/5s 逾時)通過;前端模型選擇器改為動態拉取(後備清單容錯)。Live 驗證 ICA 回 18 模型 ✅
- [ ] **WP2-T3** Agent Profile CRUD + prompt 版本化(append-only)`deps: [WP1-T3]`
  - 驗收:feature `agent_profile.feature` 全過;修改 prompt 產生新 version,舊版可查
- [ ] **WP2-T4** PromptTemplate 變數套用(`{project_name}`、`{gherkin_locale}`)`deps: [WP2-T3]`
  - 驗收:缺變數時明確錯誤;套用結果單元測試驗證
- [ ] **WP2-T5** Provider 管理 UI + 連線測試按鈕 `deps: [WP2-T2]`
- [ ] **WP2-T6** Agent Profile 管理 UI(含版本歷史檢視)`deps: [WP2-T3]`

## Phase 2 — 對話與串流(WP3, WP4)

- [x] **WP3-T1** `POST /api/chat` + `GET /api/chat/{id}/stream`(五型事件,ADR-002/003)`deps: [WP2-T1, WP2-T3, WP1-T4]` — Step 2(後端)
  - 驗收:feature `chat_streaming.feature`;訊息記錄 model_id 與 agent_profile_version
  - 實作:`POST /api/conversations` → `POST /api/conversations/{id}/messages` → `GET /api/messages/{id}/stream`(依 openapi.yaml)。ICA(claude-opus-4-8)串流 e2e 驗證通過,thinking/content/log/done 事件正確 ✅
- [x] **WP3-T2** Chat Panel UI:串流渲染 + Markdown + Shiki 高亮 `deps: [WP3-T1]` — Step 3
  - 實作:React 三欄式(對話 / Artifacts / 日誌);react-markdown + rehype-highlight(Shiki 可後續替換);串流游標 ✅
- [~] **WP3-T3** 對話中切換模型/Agent Profile `deps: [WP3-T1]` — Step 3(部分:頂欄模型選擇器,對話級切換;訊息級切換待後端支援)
- [x] **WP4-T1** ThinkingParser:`<think>` 標籤分流為 `thinking` 事件(後端,ADR-003)`deps: [WP3-T1]` — Step 2
  - 驗收:DeepSeek-R1/Qwen3 標籤樣本測試集全過;不完整標籤容錯 ✅(跨 chunk 切斷、未閉合、`<` 非標籤等單元測試通過)
- [x] **WP4-T2** Thinking 區塊 UI(串流展開、完成自動收合)`deps: [WP4-T1, WP3-T2]` — Step 3 ✅
- [~] **WP4-T3** 日誌面板(INFO/WARN/ERROR 三色)+ `log` 事件 + audit_logs 落地 `deps: [WP3-T1]` — Step 3(部分)
  - 驗收:TTFT、token 用量、錯誤堆疊皆呈現(前端日誌面板已完成);audit_logs 資料表落地待 WP1-T3/Postgres
- [ ] **WP4-T4** Langfuse 整合:Micrometer → OTel → OTLP(ADR-007)`deps: [WP3-T1, WP1-T2]`
  - 驗收:Langfuse 可見 trace,attributes 含 conversation_id、agent_profile_version
- [ ] **WP4-T5** Langfuse Prompt Management 對接(取用 + 版本記錄 + DB fallback,ADR-006)`deps: [WP4-T4, WP2-T3]`

## Phase 3 — Artifact 與 Word(WP5, WP6)

- [ ] **WP5-T1** ArtifactService:fence 抽取(gherkin/java)+ 版本化 + 降級策略(ADR-005)`deps: [WP3-T1]`
  - 驗收:feature `artifact_extraction.feature`;格式偏差樣本降級為 MARKDOWN 並記 WARN
- [~] **WP5-T2** Artifact Panel UI:高亮、複製、下載(.feature/.java/.md)`deps: [WP5-T1]` — Step 3(前端)
  - 前端已完成:自 assistant Markdown 抽取 Gherkin/Java code fence,高亮、複製、下載 .feature/.java;後端 ArtifactService 版本化待 WP5-T1
- [ ] **WP5-T3** Artifact 版本 diff 檢視 `deps: [WP5-T2]`
- [ ] **WP6-T1** 檔案上傳 + MinIO 儲存(pre-signed URL)`deps: [WP1-T2, WP1-T3]`
- [ ] **WP6-T2** docx-preview 前端整合(ADR-004)`deps: [WP6-T1]`
  - 驗收:含表格/圖片/頁首之測試文件正確渲染;保真度測試清單通過
- [ ] **WP6-T3** POI 匯出:對話產出物 → .docx → MinIO → 預覽管線 `deps: [WP6-T1, WP5-T1]`

## Phase 4 — 安全、部署與驗收(WP7, WP8)

- [ ] **WP7-T1** OIDC SSO 整合(Spring Security)`deps: [WP3-T1]`
- [ ] **WP7-T2** K8s manifests(含 Langfuse)+ Secret 管理 `deps: [WP1-T2]`
- [ ] **WP8-T1** Cucumber 驗收全套(zh-TW feature files 對應規劃書 §10)`deps: [Phase 3 全部]`
- [ ] **WP8-T2** Provider 相容性測試矩陣(Ollama/vLLM/Anthropic × streaming/thinking)`deps: [WP4-T1]`
- [ ] **WP8-T3** 負載測試(20 併發 SSE)`deps: [WP7-T2]`

---

## 估算對照

| Phase | 人天 |
|-------|------|
| 0 | 6 |
| 1 | 11 |
| 2 | 16 |
| 3 | 13 |
| 4 | 12 |
| **合計** | **58** |
