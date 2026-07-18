import { chromium } from "playwright";
import { mkdirSync, writeFileSync } from "node:fs";

const APP_URL = "http://localhost:5173/";
const OUT = process.env.OUT_DIR || "./out";
const MODEL = "claude-opus-4-8";
mkdirSync(OUT, { recursive: true });
mkdirSync(`${OUT}/video`, { recursive: true });

const PROMPTS = [
  `下列將下列情境撰寫成 Gherkin 語法，並加入正反向條件:

1. 使用者 A 到 富邦銀行的ATM 插入提款卡後
2. 輸入密碼 密碼正確 可以在畫面上選擇服務
3. 使用者 A 選擇提款 1000 元
4. 帳戶餘額充足時，提款機提供 1000 元現金
5. 並在使用者 A 帳戶上扣除 1000 元
6. 使用者 A 選擇結束服務，退出提款卡`,
  `根據上一個步驟分析結果，撰寫一份完整的業務需求文件，以 word 檔格式呈現`,
  `根據前面步驟，開發出 Java 21 + Cucumber 的 test code 與 production code，需符合DDD與SOLID，測試涵蓋率要達到 80%`,
];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  recordVideo: { dir: `${OUT}/video`, size: { width: 1440, height: 900 } },
});
const page = await context.newPage();

const results = [];
try {
  await page.goto(APP_URL, { waitUntil: "networkidle" });
  await sleep(1500);

  // 確保模型為 claude-opus-4-8
  try {
    await page.selectOption(".model-picker select", MODEL, { timeout: 5000 });
  } catch {
    console.log("model select skipped (option not present yet)");
  }
  await sleep(800);

  for (let i = 0; i < PROMPTS.length; i++) {
    const stepNo = i + 1;
    console.log(`=== Step ${stepNo}: sending prompt ===`);
    await page.fill(".composer textarea", PROMPTS[i]);
    await sleep(500);
    await page.click(".composer button");

    // 等待第 stepNo 則 assistant 訊息出現 .msg-meta(done 才會渲染)
    await page.waitForFunction(
      (n) => document.querySelectorAll(".msg-assistant .msg-meta").length >= n,
      stepNo,
      { timeout: 600000 },
    );
    await sleep(1500); // 讓串流游標收尾、Artifact 面板更新

    // 擷取該則 assistant 的文字與 code blocks
    const data = await page.evaluate(() => {
      const msgs = document.querySelectorAll(".msg-assistant");
      const last = msgs[msgs.length - 1];
      const text = last.querySelector(".msg-body")?.innerText ?? "";
      const meta = last.querySelector(".msg-meta")?.innerText ?? "";
      const blocks = Array.from(last.querySelectorAll("pre code")).map((c) => {
        const cls = c.className || "";
        const m = cls.match(/language-([\w+-]+)/);
        return { lang: m ? m[1] : "text", code: c.textContent ?? "" };
      });
      return { text, meta, blocks };
    });
    results.push({ step: stepNo, prompt: PROMPTS[i], ...data });
    console.log(`Step ${stepNo} done. meta="${data.meta}" blocks=${data.blocks.length}`);

    // 若有 Artifacts,切到 Artifacts 分頁展示於影片
    if (stepNo === 1 || stepNo === 3) {
      try { await page.click('.tabs button:has-text("Artifacts")'); } catch {}
      await sleep(1500);
    }
  }
  await sleep(1500);
} catch (err) {
  console.error("HARNESS ERROR:", err.message);
  results.push({ error: err.message });
} finally {
  writeFileSync(`${OUT}/responses.json`, JSON.stringify(results, null, 2));
  await context.close(); // flush video
  await browser.close();
  console.log("closed; video + responses written to", OUT);
}
