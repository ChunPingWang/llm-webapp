# 富邦銀行 ATM 提款 — 三步驟 Agent 產出物

本目錄為使用本 **llm-webapp**(透過 IBM ICA 呼叫 `claude-opus-4-8`)執行下列三步驟 prompt 的完整產出物,
整個操作過程以 **Playwright 螢幕錄影**(MP4)。

## 執行的三步驟 Prompt(於同一對話,具上下文)

1. **Step 1 — Gherkin**:將 ATM 提款情境撰寫成 Gherkin 語法,並加入正反向條件。
2. **Step 2 — 業務需求文件**:根據 Step 1 分析結果,撰寫完整業務需求文件(Word 格式)。
3. **Step 3 — 程式碼**:開發 Java 21 + Cucumber 的 test code 與 production code,符合 DDD 與 SOLID,測試涵蓋率 ≥ 80%。

## 產出物

| # | 檔案 | 說明 |
|---|------|------|
| 🎬 | `recording/atm-webapp-demo.mp4` | 全程操作螢幕錄影(Playwright,H.264,約 5 分鐘) |
| 1 | `01-gherkin/atm_withdrawal.feature` | 最終 feature(zh-TW,3 情境:正向 + 密碼錯誤 + 餘額不足),供 Cucumber 執行 |
| 1 | `01-gherkin/step1-full-response.md` | Step 1 模型完整回覆 |
| 2 | `02-requirements/business-requirements.docx` | **業務需求文件(Word)** — 15 表格、完整章節 |
| 2 | `02-requirements/business-requirements.md` | 需求文件原始 Markdown(docx 來源) |
| 3 | `03-java/java-project/` | **可建置的 Maven 專案**(DDD 分層 + Cucumber + JUnit 5 + JaCoCo) |
| 3 | `03-java/coverage-summary.md` | 涵蓋率摘要 |
| 3 | `03-java/coverage-jacoco.csv` | JaCoCo 原始涵蓋率資料 |
| 3 | `03-java/step3-full-response.md` | Step 3 模型完整回覆 |

## 驗證結果(實際執行)

- ✅ **測試全數通過:49 個測試**(含 **7 個 Cucumber BDD 場景**,正向/反向)
- ✅ **JaCoCo 涵蓋率遠超 80%**:指令 **99.1%**、分支 **100%**、行 **98.1%**、方法 **97.5%**
- ✅ Java 21、DDD 分層(`domain` / `application` / `infrastructure`)、SOLID(值物件、Port/Adapter、單一職責)

### 重現方式

```bash
cd 03-java/java-project
mvn clean test        # JDK 21;產生 target/site/jacoco/index.html 涵蓋率報告
```

## 產製說明(透明度)

- 三步驟均**透過 webapp 對話介面**送出並串流回覆,過程即 `recording/` 影片內容。
- `.feature` 與 Java 檔案由回覆中的 code fence(標註相對路徑)還原;`atm_withdrawal.feature` 置於
  `src/test/resources/features/` 供 `RunCucumberTest` 載入。
- `.docx` 由 Step 2 的原始 Markdown 以 `python-docx` 轉換(保留標題階層與表格)。
- 為提高 SDLC 產出準確度,平台預設 system prompt 已調整為 BDD/DDD/SOLID 導向,並將輸出上限提高至 32000 tokens
  以避免完整程式碼被截斷(見 `backend/src/main/resources/application.yaml`)。
