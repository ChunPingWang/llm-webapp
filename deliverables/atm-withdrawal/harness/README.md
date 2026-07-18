# 產製工具(Playwright 錄影 + 後製)

- `record.mjs` — 依序送出三步驟;串流時切「日誌」;完成後每步皆開浮動視窗(Step1 Gherkin / Step2 Word / Step3 Java Code)並緩慢下滑;輸出 responses.json / marks.json
- `materialize.mjs` — 將回覆 code fence 還原為 .feature 與 Java 專案
- `capture-raw.mjs` — 重放取得原始 Markdown 並呼叫 POST /api/docx 產生 .docx
- `gen-subs.mjs` — 依 marks.json 產生對齊字幕 steps.ass
- `md2docx.py` — 備用 python-docx 轉檔

流程:webm→mp4(`-c:v libopenh264`)→ gen-subs → 燒字幕(`-vf subtitles=steps.ass`)。
