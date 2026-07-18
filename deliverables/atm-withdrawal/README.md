# 富邦銀行 ATM 提款 — 三步驟 Agent 產出物

使用本 **llm-webapp**(透過 IBM ICA 呼叫 `claude-opus-4-8`)執行三步驟 prompt 的完整產出物,
全程以 **Playwright 螢幕錄影**(MP4,含步驟字幕)。

## 三步驟 Prompt(於同一對話,具上下文)

1. **Step 1 — Gherkin**:將 ATM 提款情境轉為 Gherkin,加入正反向條件。
2. **Step 2 — 業務需求文件**:根據 Step 1,撰寫完整業務需求文件(Word 格式)。
3. **Step 3 — 程式碼**:開發 Java 21 + Cucumber 的 test/production code,符合 DDD 與 SOLID,涵蓋率 ≥ 80%。

## 影片呈現方式(本版新增)

- **亮色主題**:介面改為亮色背景,錄影更清晰易讀。
- **Gherkin 緩慢捲動**:Step 1 完成後,於對話區由上而下緩慢捲動,讓觀眾看清內容。
- **Word / Java Code 浮動視窗**:Step 2 的 Word 文件與 Step 3 的 Java 程式碼以**浮動視窗(modal)**彈出,並由上而下緩慢捲動。
- **串流時顯示日誌**:送出後切到「日誌」分頁,呈現 provider 串流、TTFT、token 用量等即時事件。

## 產出物

| # | 檔案 | 說明 |
|---|------|------|
| 🎬 | `recording/atm-webapp-demo.mp4` | 全程操作錄影(H.264,約 4:02,含字幕與 ● Step N/3 標籤) |
| 1 | `01-gherkin/atm_withdrawal.feature` | zh-TW feature(正向/密碼錯誤/餘額不足/場景大綱) |
| 1 | `01-gherkin/step1-full-response.md` | Step 1 完整回覆 |
| 2 | `02-requirements/business-requirements.docx` | 業務需求文件(Word,19 表格,後端 Apache POI 產生) |
| 2 | `02-requirements/business-requirements.md` | 需求文件原始 Markdown |
| 3 | `03-java/java-project/` | 可建置 Maven 專案(DDD + Cucumber + JUnit 5 + JaCoCo) |
| 3 | `03-java/coverage-summary.md` / `coverage-jacoco.csv` | 涵蓋率摘要與原始資料 |
| 3 | `03-java/step3-full-response.md` | Step 3 完整回覆 |
| — | `harness/` | Playwright 錄影 + 字幕 + 後製腳本 |

## 驗證結果(實際執行)

- ✅ **測試全數通過:41 個測試**(含 **7 個 Cucumber BDD 場景**,正向/反向/場景大綱)
- ✅ **JaCoCo 涵蓋率:指令 99.0% / 分支 92.3% / 行 98.3%**,遠超 80%
- ✅ **Cucumber 英文 annotation**(`@Given/@When/@Then/@And`);feature 檔為繁體中文
- ✅ Java 21、DDD 分層、SOLID

```bash
cd 03-java/java-project && mvn clean test   # JDK 21;產生 target/site/jacoco/index.html
```

## 產製說明(透明度)

- 三步驟均透過 webapp 對話介面送出並串流回覆(即影片內容)。
- `.docx` 由 Step 2 原始 Markdown 經後端 `POST /api/docx`(Apache POI)產生 —— 與 app 內 Word 預覽相同來源。
- 本次生成的 step definition 在「插卡後才設定餘額前置條件」的場景有載入時機問題,
  已於 `AtmWithdrawalSteps.resetBalance` 加一行重新載入卡片修正,使所有場景正確通過(見 git 記錄)。
- 為讓串流真實呈現(非快取),送出時附加不可見的 zero-width space 以避開 ICA 相同 prompt 快取。
