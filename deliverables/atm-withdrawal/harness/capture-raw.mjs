// 透過後端 API 重放 Step1→Step2(→可選 Step3),擷取「原始 markdown」(SSE content deltas)。
// 用於產生高品質 .docx(DOM innerText 會遺失 # 與表格結構)。
import { writeFileSync } from "node:fs";

const B = "http://localhost:8080";
const OUT = process.env.OUT_DIR || "./out";

const STEP1 = `下列將下列情境撰寫成 Gherkin 語法，並加入正反向條件:

1. 使用者 A 到 富邦銀行的ATM 插入提款卡後
2. 輸入密碼 密碼正確 可以在畫面上選擇服務
3. 使用者 A 選擇提款 1000 元
4. 帳戶餘額充足時，提款機提供 1000 元現金
5. 並在使用者 A 帳戶上扣除 1000 元
6. 使用者 A 選擇結束服務，退出提款卡`;
const STEP2 = `根據上一個步驟分析結果，撰寫一份完整的業務需求文件，以 word 檔格式呈現`;

async function post(url, body) {
  const r = await fetch(url, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
  return r.json();
}

// 讀 SSE,回傳完整 content(raw markdown)
async function streamContent(messageId) {
  const res = await fetch(`${B}/api/messages/${messageId}/stream`);
  const reader = res.body.getReader();
  const dec = new TextDecoder();
  let buf = "", content = "", event = null;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buf += dec.decode(value, { stream: true });
    const lines = buf.split("\n");
    buf = lines.pop();
    for (const line of lines) {
      if (line.startsWith("event:")) event = line.slice(6).trim();
      else if (line.startsWith("data:")) {
        const data = line.slice(5).trim();
        if (event === "content") content += JSON.parse(data).delta;
        if (event === "done") return content;
      }
    }
  }
  return content;
}

const { conversationId } = await post(`${B}/api/conversations`, { title: "raw-capture", modelId: "claude-opus-4-8" });
console.log("conv", conversationId);

const m1 = await post(`${B}/api/conversations/${conversationId}/messages`, { content: STEP1 });
console.log("step1 streaming...");
const c1 = await streamContent(m1.messageId);
console.log("step1 len", c1.length);
writeFileSync(`${OUT}/step1-raw.md`, c1);

const m2 = await post(`${B}/api/conversations/${conversationId}/messages`, { content: STEP2 });
console.log("step2 streaming...");
const c2 = await streamContent(m2.messageId);
console.log("step2 len", c2.length);
writeFileSync(`${OUT}/step2-raw.md`, c2);

console.log("done");
