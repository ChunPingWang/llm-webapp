# 富邦銀行 ATM 提款 — 三步驟 Agent 產出物

使用本 **llm-webapp**(透過 IBM ICA 呼叫 `claude-opus-4-8`)執行三步驟 prompt 的完整產出物,
全程以 **Playwright 螢幕錄影**(MP4,含步驟字幕)。

## 三步驟 Prompt(於同一對話,具上下文)

1. **Step 1 — Gherkin**:將 ATM 提款情境撰寫成 Gherkin,並加入正反向條件。
2. **Step 2 — 業務需求文件**:根據 Step 1,撰寫完整業務需求文件(Word 格式)。
3. **Step 3 — 程式碼**:開發 Java 21 + Cucumber 的 test/production code,符合 DDD 與 SOLID,涵蓋率 ≥ 80%。

## 產出物

| # | 檔案 | 說明 |
|---|------|------|
| 🎬 | `recording/atm-webapp-demo.mp4` | 全程操作錄影(H.264,約 5:11,含步驟字幕與 ● Step N/3 標籤) |
| 1 | `01-gherkin/atm_withdrawal.feature` | zh-TW feature(正向 + 密碼錯誤 + 餘額不足),供 Cucumber 執行 |
| 1 | `01-gherkin/step1-full-response.md` | Step 1 完整回覆 |
| 2 | `02-requirements/business-requirements.docx` | **業務需求文件(Word)** — 18 表格,由後端 Apache POI 產生 |
| 2 | `02-requirements/business-requirements.md` | 需求文件原始 Markdown |
| 3 | `03-java/java-project/` | **可建置 Maven 專案**(DDD 分層 + Cucumber + JUnit 5 + JaCoCo) |
| 3 | `03-java/coverage-summary.md` / `coverage-jacoco.csv` | 涵蓋率摘要與原始資料 |
| 3 | `03-java/step3-full-response.md` | Step 3 完整回覆 |
| — | `harness/` | Playwright 錄影 + 字幕 + 後製腳本 |

## 驗證結果(實際執行)

- ✅ **測試全數通過:43 個測試**(含 **7 個 Cucumber BDD 場景**,正向/反向)
- ✅ **JaCoCo 涵蓋率:全項 100%**(指令/分支/行/方法/類別),遠超 80% 要求
- ✅ **Cucumber 使用英文 annotation**(`@Given/@When/@Then/@And`,`io.cucumber.java.en.*`);feature 檔為繁體中文
- ✅ Java 21、DDD 分層(`domain` / `application` / `infrastructure`)、SOLID(值物件、Port/Adapter)

```bash
cd 03-java/java-project && mvn clean test   # JDK 21;產生 target/site/jacoco/index.html
```

## 影片重點(對應使用者回饋)

- **Step 2 顯示 Word 內容**:右側「Word 預覽」分頁以 docx-preview 內嵌渲染後端產生的 .docx(標題/表格/版面),不再只有聊天文字。
- **串流等待時顯示日誌**:每步送出後自動切到「日誌」分頁,呈現 provider 串流開始、TTFT、token 用量等即時事件,證明背景確實在執行;完成後切回產出物分頁。
- 下方字幕標示各步驟目標,頂部標籤顯示 ● Step N/3 進度。

## 產製說明(透明度)

- 三步驟均透過 webapp 對話介面送出並串流回覆(即影片內容)。
- `.feature` 與 Java 檔由回覆的 code fence(標註相對路徑)還原;feature 置於 `src/test/resources/features/`。
- `.docx` 由 Step 2 原始 Markdown 經後端 `POST /api/docx`(Apache POI)產生 —— 與 app 內 Word 預覽相同來源。
- 平台預設 system prompt 為 BDD/DDD/SOLID 導向,`max_tokens=32000` 避免截斷(見 `backend/.../application.yaml`)。
