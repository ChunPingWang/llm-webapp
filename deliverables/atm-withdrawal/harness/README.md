# 產製工具(Playwright 錄影 + 後製)

- `record.mjs` — Playwright 開啟 webapp,依序送出三步驟 prompt;串流時切到「日誌」,完成後切回 Artifacts / Word 預覽;錄影(webm)並擷取回覆為 responses.json
- `materialize.mjs` — 將回覆 code fence(依路徑註解)還原為 .feature 與 Java 專案
- `capture-raw.mjs` — 以後端 API 重放取得原始 Markdown,並呼叫 POST /api/docx 產生 .docx
- `md2docx.py` — (備用)以 python-docx 轉 .docx
- `steps.ass` — 依實際時間軸的步驟字幕

前置:webapp 後端(:8080)+ 前端(:5173)已啟動;`npm i playwright && npx playwright install chromium`。
webm→mp4:`ffmpeg -i in.webm -c:v libopenh264 -pix_fmt yuv420p -b:v 1200k out.mp4`
燒字幕:`ffmpeg -i demo.mp4 -vf "subtitles=steps.ass" -c:v libopenh264 -b:v 1300k demo-subtitled.mp4`
