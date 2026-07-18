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

const t0 = Date.now();
const ts = () => ((Date.now() - t0) / 1000).toFixed(1);
const marks = [];
const mark = (label) => { const t = ts(); marks.push({ label, t: Number(t) }); console.log(`[t=${t}s] ${label}`); };

// 於指定容器由上而下緩慢捲動
async function slowScroll(sel, durationMs) {
  await page.evaluate(({ sel, dur }) => new Promise((resolve) => {
    const el = document.querySelector(sel);
    if (!el) return resolve();
    const dist = el.scrollHeight - el.clientHeight;
    if (dist <= 4) return resolve();
    const start = performance.now();
    function step(now) {
      const t = Math.min(1, (now - start) / dur);
      el.scrollTop = dist * t;
      if (t < 1) requestAnimationFrame(step); else resolve();
    }
    el.scrollTop = 0;
    requestAnimationFrame(step);
  }), { sel, dur: durationMs });
}

const results = [];
try {
  await page.goto(APP_URL, { waitUntil: "networkidle" });
  mark("app loaded");
  await sleep(1500);
  try { await page.selectOption(".model-picker select", MODEL, { timeout: 5000 }); } catch {}
  await sleep(800);

  for (let i = 0; i < PROMPTS.length; i++) {
    const stepNo = i + 1;
    // 附加不可見的 zero-width space,避免 ICA 完全相同 prompt 的快取命中(使串流真實呈現)
    await page.fill(".composer textarea", PROMPTS[i] + "​");
    await sleep(500);
    await page.click(".composer button");
    mark(`step${stepNo} send`);

    // 串流期間切到「日誌」,展示背景即時事件
    await sleep(700);
    try { await page.click('.tabs button:has-text("日誌")'); } catch {}

    await page.waitForFunction(
      (n) => document.querySelectorAll(".msg-assistant .msg-meta").length >= n,
      stepNo, { timeout: 600000 },
    );
    mark(`step${stepNo} done`);
    await sleep(1000);

    // 先擷取回覆(與 modal 無關)
    const data = await page.evaluate(() => {
      const msgs = document.querySelectorAll(".msg-assistant");
      const last = msgs[msgs.length - 1];
      const text = last.querySelector(".msg-body")?.innerText ?? "";
      const meta = last.querySelector(".msg-meta")?.innerText ?? "";
      const blocks = Array.from(last.querySelectorAll("pre code")).map((c) => {
        const m = (c.className || "").match(/language-([\w+-]+)/);
        return { lang: m ? m[1] : "text", code: c.textContent ?? "" };
      });
      return { text, meta, blocks };
    });
    results.push({ step: stepNo, prompt: PROMPTS[i], ...data });
    console.log(`Step ${stepNo} done. meta="${data.meta}" blocks=${data.blocks.length}`);

    // === 呈現階段 ===
    if (stepNo === 1) {
      // Gherkin:於對話區由上而下緩慢捲動
      await sleep(600);
      mark("step1 scroll start");
      await slowScroll(".messages", 9000);
      mark("step1 scroll end");
      await sleep(600);
    } else if (stepNo === 2) {
      // Word:切分頁 → 放大浮動視窗 → 緩慢下滑
      try {
        await page.click('.tabs button:has-text("Word 預覽")');
        await page.waitForSelector(".docx-host section.docx", { timeout: 30000 });
        await sleep(1000);
        await page.click('button:has-text("放大")');
        await page.waitForSelector(".modal-body section.docx", { timeout: 30000 });
        await sleep(1200);
        mark("step2 modal scroll start");
        await slowScroll(".modal-body", 14000);
        mark("step2 modal scroll end");
        await sleep(800);
        await page.click(".modal-close");
        await sleep(600);
      } catch (e) { console.log("step2 present failed:", e.message); }
    } else if (stepNo === 3) {
      // Java Code:切 Artifacts → 放大浮動視窗 → 緩慢下滑
      try {
        await page.click('.tabs button:has-text("Artifacts")');
        await sleep(900);
        await page.click('button:has-text("放大")');
        await page.waitForSelector(".modal-body .markdown", { timeout: 20000 });
        await sleep(1200);
        mark("step3 modal scroll start");
        await slowScroll(".modal-body", 18000);
        mark("step3 modal scroll end");
        await sleep(800);
        await page.click(".modal-close");
        await sleep(600);
      } catch (e) { console.log("step3 present failed:", e.message); }
    }
  }
  mark("finished");
  await sleep(1200);
} catch (err) {
  console.error("HARNESS ERROR:", err.message);
  results.push({ error: err.message });
} finally {
  writeFileSync(`${OUT}/responses.json`, JSON.stringify(results, null, 2));
  writeFileSync(`${OUT}/marks.json`, JSON.stringify(marks, null, 2));
  await context.close();
  await browser.close();
  console.log("closed; video + responses + marks written to", OUT);
}
