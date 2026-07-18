# 富邦銀行 ATM 提款 — 三步驟 Agent 產出物

使用本 **llm-webapp**(透過 IBM ICA 呼叫 `claude-opus-4-8`)執行三步驟 prompt 的完整產出物,
全程以 **Playwright 螢幕錄影**(MP4,含步驟字幕)。

## 三步驟 Prompt(於同一對話,具上下文)

1. **Step 1 — Gherkin**:將 ATM 提款情境轉為 Gherkin,加入正反向條件。
2. **Step 2 — 業務需求文件**:根據 Step 1,撰寫完整業務需求文件(Word 格式)。
3. **Step 3 — 程式碼**:開發 Java 21 + Cucumber 的 test/production code,符合 DDD 與 SOLID,涵蓋率 ≥ 80%。

## 影片呈現方式

- **亮色主題**,錄影清晰易讀。
- **三種產出物皆以浮動視窗(modal)呈現並由上而下緩慢捲動**:Step 1 Gherkin、Step 2 Word 文件、Step 3 Java 程式碼。
- **串流時顯示日誌**:送出後切「日誌」分頁,呈現 provider 串流、TTFT、token 用量等即時事件。
- **範圍精準**:Step 1 只輸出 Gherkin(不再夾帶 Java/建置檔),由 system prompt 的範圍限制達成。

## 產出物

| # | 檔案 | 說明 |
|---|------|------|
| 🎬 | `recording/atm-webapp-demo.mp4` | 全程操作錄影(H.264,約 3:48,含字幕) |
| 1 | `01-gherkin/atm_withdrawal.feature` | zh-TW feature(正向/密碼錯誤/鎖卡/餘額不足/場景大綱) |
| 1 | `01-gherkin/step1-full-response.md` | Step 1 完整回覆(僅 Gherkin) |
| 2 | `02-requirements/business-requirements.docx` | 業務需求文件(Word,Apache POI 產生) |
| 2 | `02-requirements/business-requirements.md` | 需求文件原始 Markdown |
| 3 | `03-java/java-project/` | 可建置 Maven 專案(DDD + Cucumber + JUnit 5 + JaCoCo) |
| 3 | `03-java/coverage-summary.md` / `coverage-jacoco.csv` | 涵蓋率摘要與原始資料 |
| 3 | `03-java/step3-full-response.md` | Step 3 完整回覆 |
| — | `harness/` | Playwright 錄影 + 字幕 + 後製腳本 |

## 驗證結果(實際執行)

- ✅ **測試全數通過:27 個測試**(含 **7 個 Cucumber BDD 場景**)
- ✅ **JaCoCo 涵蓋率:全項 100%**,遠超 80%
- ✅ **Cucumber 英文 annotation**;feature 檔為繁體中文
- ✅ Java 21、DDD 分層、SOLID

```bash
cd 03-java/java-project && mvn clean test   # JDK 21
```

## 產製說明(透明度)

- 三步驟均透過 webapp 對話介面送出並串流回覆(即影片內容)。
- `.docx` 由 Step 2 原始 Markdown 經後端 `POST /api/docx`(Apache POI)產生。
- 本次生成的 step definition 有一組重複定義(@When 與 @And 同一表達式),已移除重複者以通過 Cucumber(見 git 記錄)。
- 送出時附加不可見 zero-width space 以避開 ICA 相同 prompt 快取,使串流真實呈現。
