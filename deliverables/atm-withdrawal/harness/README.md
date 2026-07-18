# 產出物產製工具(Playwright 錄影 + 後製)

- `record.mjs` — 以 Playwright 開啟 webapp,依序送出三步驟 prompt、錄影(webm),擷取回覆為 `responses.json`
- `materialize.mjs` — 將回覆中的 code fence(依路徑註解)還原為 .feature 與 Java 專案檔
- `capture-raw.mjs` — 以後端 API 重放取得原始 Markdown(供高品質 docx 轉換)
- `md2docx.py` — 將需求 Markdown 轉為 .docx(python-docx)

前置:webapp 後端(:8080)與前端(:5173)已啟動;`npm i playwright && npx playwright install chromium`;`pip install python-docx`。
webm→mp4:`ffmpeg -i in.webm -c:v libopenh264 -pix_fmt yuv420p -b:v 1200k out.mp4`

## 字幕(步驟標示)

`steps.ass` 依實際串流時間軸標示三步驟。燒錄:
```bash
ffmpeg -i demo.mp4 -vf "subtitles=steps.ass" -c:v libopenh264 -pix_fmt yuv420p -b:v 1300k demo-subtitled.mp4
```
