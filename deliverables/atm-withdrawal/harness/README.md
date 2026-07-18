# 產製工具(Playwright 錄影 + 後製)

- `record.mjs` — Playwright 依序送出三步驟;串流時切「日誌」;完成後 Step1 於對話區緩慢捲動、
  Step2 開「Word 預覽」浮動視窗緩慢捲動、Step3 開「產出程式碼」浮動視窗緩慢捲動;錄影並輸出 responses.json / marks.json
- `materialize.mjs` — 將回覆 code fence(依路徑註解)還原為 .feature 與 Java 專案
- `capture-raw.mjs` — 以後端 API 重放取得原始 Markdown,並呼叫 POST /api/docx 產生 .docx
- `gen-subs.mjs` — 依 marks.json 時間戳產生對齊的 steps.ass 字幕
- `md2docx.py` — (備用)python-docx 轉檔

流程:webm→mp4(`-c:v libopenh264`)→ gen-subs → 燒字幕(`-vf subtitles=steps.ass`)。
