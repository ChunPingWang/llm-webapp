# LLM Web 應用平台評估規劃書
## (類 Open WebUI:多模型串接 + SDLC 產出物生成平台)

> 文件性質:架構評估與規劃(不含實作)
> 版本:v1.1 | 日期:2026-07-18
> v1.1 變更:定位調整為 AI Agent 平台 — 新增 Agent Profile 與 system prompt 版本化(ADR-006)、Langfuse 可觀測性(ADR-007);文件包重構為 Claude Code 可消費格式(CLAUDE.md / adr/ / tasks/ / specs/)

---

## 1. 需求摘要

| # | 需求 | 說明 |
|---|------|------|
| R1 | 多模型串接 | 類似 Open WebUI,可連接 Ollama、OpenAI-compatible、Anthropic 等不同 Provider,並可於對話中切換模型 |
| R2 | 產出物:Gherkin Feature File | 對話產出的 BDD 規格以 `.feature` 語法呈現 |
| R3 | 產出物:Java 程式碼 | 對話產出的 Java 程式碼(可含 Spring Boot、測試碼) |
| R4 | 產出物以 Markdown 呈現 | Gherkin 與 Java 皆包在 Markdown code fence 中,支援語法高亮、複製、下載 |
| R5 | Word 檔可預覽 | 使用者上傳或系統產生的 `.docx` 可直接在瀏覽器中預覽,不需下載後開啟 |
| R6 | 思考過程與日誌即時呈現 | 每次送出對話,畫面即時顯示模型的 reasoning(`<think>` 區塊)、工具呼叫、系統日誌等訊息 |

### 範圍界定(Scope)

- **In Scope**:對話介面、Provider 管理、串流輸出、產出物渲染與下載、Word 預覽、思考過程/日誌面板
- **Out of Scope(本期)**:RAG 知識庫、多租戶權限細粒度控管、模型微調、Agent 工作流編排(可列為 Phase 2)

---

## 2. 方案選項評估

### 選項 A:直接部署 Open WebUI + Pipelines/Functions 擴充

| 面向 | 評估 |
|------|------|
| 優點 | R1、R6 幾乎開箱即用(Ollama/OpenAI-compatible 連線、reasoning parser、collapsible thinking block);社群活躍、更新快 |
| 缺點 | Word 預覽(R5)非原生功能,需寫 Function/自訂前端;技術棧為 Python + Svelte,與團隊 Java 主力不一致;客製 UI(產出物分頁、日誌面板)受框架限制;授權條款需審閱(Open WebUI License 對品牌與企業使用有限制,銀行客戶導入前須法遵確認) |
| 適用情境 | 內部 PoC、快速驗證 prompt 與模型效果 |

### 選項 B:自建輕量平台(建議)

| 面向 | 評估 |
|------|------|
| 優點 | 完全掌控 UI/UX(產出物三分頁:對話 / Artifacts / 日誌);後端可用 Spring Boot(團隊熟悉、易維運、易整合企業 SSO/稽核);授權乾淨,可交付客戶 |
| 缺點 | 開發工作量較高(估 40–60 人天,見 §9);Provider 相容性需自行維護 |
| 適用情境 | 要交付給銀行/企業客戶、需長期演進的產品 |

### 選項 C:Open WebUI 前端 fork + 自建後端

不建議。fork 維護成本高,upstream 更新頻繁,長期會分岔。

**結論:建議選項 B。** 若時程極短,可先以選項 A 做 2 週 PoC 驗證 prompt 與模型選型,再進入選項 B 開發。

---

## 3. 系統架構(選項 B)

```
┌─────────────────────────────────────────────────────┐
│  Frontend (React + TypeScript + Vite)               │
│  ├─ Chat Panel(SSE 串流渲染)                        │
│  ├─ Thinking / Log Panel(可摺疊、分色)              │
│  ├─ Artifact Panel(Markdown / Gherkin / Java)       │
│  └─ Word Preview(docx-preview / mammoth.js)         │
└───────────────▲─────────────────────────────────────┘
                │ SSE (text/event-stream) + REST
┌───────────────┴─────────────────────────────────────┐
│  Backend (Spring Boot 3.x + Spring AI)              │
│  ├─ ChatController(/api/chat/stream, SSE)           │
│  ├─ Provider Abstraction(ChatModel SPI)             │
│  │    ├─ OllamaChatModel(:11434, native protocol)   │
│  │    ├─ OpenAiCompatible(vLLM / LocalAI / GPT)     │
│  │    └─ AnthropicChatModel                         │
│  ├─ ThinkingParser(<think>...</think> 分流)          │
│  ├─ ArtifactService(Gherkin / Java 抽取與版本化)     │
│  ├─ DocxService(POI 產生 .docx;預覽轉換)            │
│  └─ AuditLogService(對話與工具呼叫日誌)              │
└───────────────▲─────────────────────────────────────┘
                │
   PostgreSQL(conversations / messages / artifacts)
   MinIO 或檔案系統(.docx、附件)
```

### 關鍵設計決策(ADR 摘要)

| ADR | 決策 | 理由 |
|-----|------|------|
| ADR-001 | Provider 抽象採 **Spring AI ChatModel** 介面 | 原生支援 Ollama / OpenAI / Anthropic,streaming 以 Reactor `Flux<ChatResponse>` 統一,免自造輪子 |
| ADR-002 | 串流採 **SSE** 而非 WebSocket | 單向串流場景 SSE 較簡單,天然支援斷線重連(Last-Event-ID),企業 Proxy 相容性佳 |
| ADR-003 | 思考過程分流採 **事件型別標記** | SSE event 分為 `thinking` / `content` / `tool_call` / `log` / `done` 五類,前端據此分別渲染至不同面板 |
| ADR-004 | Word 預覽採 **前端 docx-preview**,伺服器端不轉檔 | 免部署 LibreOffice/OnlyOffice,降低維運成本;渲染保真度足以應付一般文件(見 §5.3 比較) |
| ADR-005 | 產出物(Artifact)獨立資料表並版本化 | Gherkin / Java 可被後續 coding agent 直接消費,需可追溯、可下載為 `.feature` / `.java` / `.md` |
| ADR-006 | Agent Profile 與 system prompt 版本化 | 不同任務(BDD 規格 / Java 產碼 / Code Review)各自定義 system prompt、模型與參數;prompt append-only 版本化,產出物可追溯至 prompt 版本;prompt 版本庫可託管於 Langfuse Prompt Management |
| ADR-007 | 可觀測性採 Langfuse(經 OTel),不採 LangSmith | Spring AI 內建 Micrometer Observation → OTel → Langfuse OTLP endpoint;Langfuse 開源可自架,符合銀行資料落地要求;LangSmith 生態偏 LangChain/Python 且自架受限 |

> 各 ADR 完整內容(Context / Decision / Consequences)見 `adr/ADR-001` ~ `ADR-007` 獨立檔案。

---

## 4. 多模型串接設計(R1)

參考 Open WebUI 的 Protocol-Oriented 設計:「Ollama」指的是 Ollama API 協定(port 11434),而 OpenAI 標準的後端(vLLM、LocalAI 等)走 OpenAI-Compatible 路徑。

### Provider 設定模型

```yaml
providers:
  - id: local-ollama
    type: OLLAMA            # native protocol, :11434
    baseUrl: http://ollama:11434
    models: [qwen3:32b, deepseek-r1:14b]
  - id: vllm-cluster
    type: OPENAI_COMPATIBLE
    baseUrl: http://vllm:8000/v1
    apiKey: ${VLLM_KEY}
  - id: anthropic
    type: ANTHROPIC
    apiKey: ${ANTHROPIC_KEY}
```

### 功能點

- 模型清單動態拉取(Ollama `/api/tags`;OpenAI `/v1/models`)
- 每個對話可指定模型,亦可對話中切換(訊息層級記錄使用的 model id)
- 多 Ollama 實例負載分散(隨機選取,Phase 2 可加健康檢查與 failover;注意 Open WebUI 文件提及的 timeout 教訓,model list 拉取需設短 timeout)
- 連線測試按鈕(管理介面)
- 注意事項:reasoning 模型(DeepSeek-R1、Qwen3)在 Ollama 端需啟用 reasoning parser,或由本平台的 ThinkingParser 於後端自行解析 `<think>` 標籤 —— **建議後端解析**,避免依賴各 Provider 行為差異

### 4.5 Agent Profile 與 System Prompt 管理(ADR-006)

平台定位為 AI Agent 前台,對話不再只是「選模型」,而是「選 Agent」:

- 每個 **Agent Profile** 定義:名稱、system prompt、預設模型、temperature、允許工具清單
- System prompt 支援範本變數(`{project_name}`、`{gherkin_locale}` 等),以 Spring AI `PromptTemplate` 套用
- Prompt 修改即產生新版本(append-only);每則訊息記錄 `agent_profile_id + agent_profile_version`,產出物可完整追溯至生成當下的 prompt 版本
- Prompt 主版本庫可託管於 **Langfuse Prompt Management**(ADR-007),平台快取並記錄取用版本;Langfuse 不可用時退回 DB
- 初始內建三個 Profile:`BDD 規格 Agent`(產出 zh-TW Gherkin)、`Java 產碼 Agent`(Spring Boot + 測試碼)、`Code Review Agent`

此設計與 SDLC Agentic Platform 的 Orchestrator + 專職 sub-agent 架構對齊,本平台可作為該類 agent 的互動與追溯前台。

### 4.6 可觀測性:Langfuse via OpenTelemetry(ADR-007)

```
Spring AI ChatClient ──Micrometer Observation──▶ OTel SDK ──OTLP──▶ Langfuse(自架)
```

- Trace 層級:每次模型呼叫的 prompt、completion、token 用量、延遲、成本;以 OTel attributes 附掛 `conversation_id`、`agent_profile_version`
- 與平台內「日誌面板」分工:日誌面板為即時、面向使用者(SSE `log` 事件);Langfuse 為離線分析、面向團隊(prompt 版本比較、成本分析、evaluation)
- Langfuse 納入 docker compose 與 K8s 部署清單

---

## 5. 核心功能設計

### 5.1 思考過程與日誌即時呈現(R6)

SSE 事件協定:

```
event: thinking
data: {"delta": "使用者要求產生轉帳的 Gherkin..."}

event: content
data: {"delta": "以下是 feature file:\n```gherkin\n..."}

event: log
data: {"level":"INFO","source":"provider","msg":"model=qwen3:32b, ttft=820ms"}

event: done
data: {"usage":{"promptTokens":1250,"completionTokens":890},"elapsedMs":12400}
```

前端呈現:

- **Thinking 區塊**:訊息上方可摺疊灰底區,串流時即時展開,完成後自動收合(同 Open WebUI collapsible section 體驗)
- **日誌面板**:右側或底部抽屜,顯示 Provider 呼叫、token 用量、延遲、錯誤堆疊;分 INFO/WARN/ERROR 三色
- 日誌同步落地至 AuditLog 表,滿足銀行客戶稽核需求

### 5.2 產出物:Gherkin + Java(R2–R4)

- 後端於串流完成後以 fence parser 抽取 ` ```gherkin ` 與 ` ```java ` 區塊,建立 Artifact 記錄(type, language, content, version, message_id)
- Artifact Panel 提供:語法高亮(Shiki/Prism)、一鍵複製、下載為 `.feature` / `.java` / `.md`、版本比對(diff)
- System Prompt 範本強制模型輸出格式(例:feature file 一律 zh-TW Gherkin 關鍵字 `功能:/場景:` 或英文,依專案設定)
- Phase 2:Gherkin 語法驗證(gherkin parser)、Java 編譯檢查(可選)

### 5.3 Word 預覽(R5)— 技術選型比較

| 方案 | 原理 | 保真度 | 維運成本 | 建議 |
|------|------|--------|----------|------|
| **docx-preview(前端 JS)** | 直接解析 docx XML 渲染成 HTML/CSS | 中高(支援表格、圖片、頁首頁尾) | 零(純前端) | ✅ 首選 |
| mammoth.js(前端/後端) | docx → 語意化 HTML | 中(捨棄版面,重語意) | 零 | 備選(適合轉 Markdown 情境) |
| LibreOffice headless → PDF | 伺服器端轉 PDF 後以 pdf.js 預覽 | 高 | 中(需容器內裝 LO,轉檔佇列) | 複雜文件 fallback |
| OnlyOffice Document Server | 完整線上檢視/編輯 | 最高 | 高(獨立服務、授權審閱) | 若未來需線上「編輯」再導入 |

建議:**docx-preview 為主,LibreOffice→PDF 為複雜文件的伺服器端 fallback**(Phase 2)。

系統產生 Word(如將 Gherkin+Java 評估報告匯出為 .docx)則以 Apache POI 於後端產生,存 MinIO,並經同一預覽管線呈現。

---

## 6. 資料模型(簡要)

```
conversations(id, title, agent_profile_id, created_by, created_at)
messages(id, conversation_id, role, model_id, agent_profile_id,
         agent_profile_version, content, thinking, created_at)
artifacts(id, message_id, agent_profile_version, type[GHERKIN|JAVA|MARKDOWN|DOCX],
          language, content, storage_key, version, created_at)
providers(id, type, base_url, api_key_ref, enabled)
agent_profiles(id, name, description, system_prompt, default_model_id,
               temperature, tools, version, enabled, created_at)   -- append-only 版本化
audit_logs(id, conversation_id, level, source, payload, created_at)
```

---

## 7. 非功能需求

| 項目 | 目標 |
|------|------|
| 串流首字延遲(TTFT)顯示 | 於日誌面板呈現,便於模型選型比較 |
| 併發 | 初期 20 併發使用者;SSE 連線以 Reactor 非阻塞處理 |
| 安全 | API Key 存 Vault/K8s Secret;企業 SSO(OIDC);所有對話落 AuditLog |
| 部署 | 容器化(Podman/K8s);Ollama/vLLM 獨立 GPU 節點 |
| i18n | zh-TW 為主,介面雙語 |

---

## 8. 風險與對策

| 風險 | 影響 | 對策 |
|------|------|------|
| 各 Provider streaming 格式差異(thinking 標籤、tool call 格式) | 思考面板顯示異常 | 後端 ThinkingParser 統一解析;每個 Provider 寫相容性測試(Testcontainers + WireMock) |
| docx-preview 對複雜排版保真度不足 | 預覽走樣 | LibreOffice→PDF fallback;驗收前用客戶實際文件做保真度測試清單 |
| 模型輸出不遵循 code fence 格式 | Artifact 抽取失敗 | System prompt 硬約束 + 抽取失敗時整段降級為 Markdown artifact |
| 上下文長度設定不當(如 Ollama num_ctx 預設過小)導致截斷 | 空白回應或格式崩壞 | 平台層預設合理 num_ctx(≥16384)並於日誌面板警示接近上限 |
| Open WebUI 授權(若採選項 A) | 法遵風險 | 導入前完成 license 審閱 |

---

## 9. 工作量估算(選項 B,單位:人天)

| 工作包 | 內容 | 人天 |
|--------|------|------|
| WP1 基礎框架 | Spring Boot 骨架、SSE 管線、React 專案、CI | 6 |
| WP2 Provider + Agent Profile | Spring AI 整合三類 Provider、管理介面、連線測試、Agent Profile CRUD 與 prompt 版本化 | 11 |
| WP3 對話與串流 UI | Chat Panel、Markdown 渲染、語法高亮 | 7 |
| WP4 Thinking/日誌/可觀測性 | ThinkingParser、事件分流、日誌面板、AuditLog、Langfuse OTel 整合與 Prompt Management 對接 | 9 |
| WP5 Artifact 管理 | 抽取、版本化、下載(.feature/.java/.md)、diff | 7 |
| WP6 Word 預覽/產生 | docx-preview 整合、POI 匯出、MinIO 儲存 | 6 |
| WP7 安全與部署 | OIDC、Secret 管理、容器化、K8s manifests | 5 |
| WP8 測試與驗收 | BDD 驗收(自身 dogfooding)、相容性測試、負載測試 | 7 |
| **合計** | | **58** |

> 2 人團隊約 6–7 週;若先做選項 A PoC,另加 5–8 人天。
> 任務層級拆解(含依賴與驗收準則)見 `tasks/TASKS.md`。

---

## 10. 驗收準則(Gherkin 示例)

```gherkin
功能: 模型思考過程即時呈現
  場景: 使用 reasoning 模型送出訊息
    假設 使用者已選擇模型 "deepseek-r1:14b"
    當 使用者送出訊息 "產生轉帳功能的 feature file"
    那麼 畫面應在 3 秒內開始串流顯示思考區塊
    而且 思考區塊完成後應自動收合
    而且 日誌面板應顯示 model、TTFT 與 token 用量

功能: Word 檔預覽
  場景: 上傳 docx 並預覽
    假設 使用者上傳 "需求規格書.docx"
    當 使用者點擊該附件
    那麼 系統應於瀏覽器內渲染文件內容,不需下載
```

---

## 11. 建議與下一步

1. **決策**:確認採選項 B(自建)或先以選項 A 做 2 週 PoC
2. **模型選型測試**:以實際 Gherkin/Java 生成任務評測候選模型(Qwen3、DeepSeek-R1、Claude 等)之格式遵循度
3. **Word 保真度測試**:收集 3–5 份客戶實際 docx 驗證 docx-preview 效果
4. **細部設計**:確認後產出完整 ADR 集、API 規格(OpenAPI)與 Sprint 計畫
