// 從 responses.json 取出各步驟產出物並落地到 deliverables 目錄。
// - Step 1: gherkin 區塊 → *.feature
// - Step 2: 全文 markdown(供 md2docx.py 轉 .docx)
// - Step 3: 各 code 區塊依首行 `// path` 或 `# path` 註解寫入 java-project/
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";

const OUT = process.env.OUT_DIR || "./out";
const DELIV = process.env.DELIV_DIR || "./deliverables";
const results = JSON.parse(readFileSync(`${OUT}/responses.json`, "utf-8"));

function write(p, content) {
  mkdirSync(dirname(p), { recursive: true });
  writeFileSync(p, content);
  console.log("wrote", p);
}

// 從區塊首行擷取路徑註解:// path、# path、<!-- path -->
function pathFromBlock(code) {
  const first = code.split("\n")[0].trim();
  let m =
    first.match(/^\/\/\s*(\S+\.\w+)/) ||
    first.match(/^#\s*(?:language:\s*\S+\s*)?(\S+\.\w+)/) ||
    first.match(/^<!--\s*(\S+\.\w+)\s*-->/);
  if (m) {
    // 去掉首行註解,回傳純內容
    const body = code.split("\n").slice(1).join("\n").replace(/^\n+/, "");
    return { path: m[1], body };
  }
  return null;
}

mkdirSync(DELIV, { recursive: true });

for (const r of results) {
  if (r.error) {
    console.log("STEP ERROR:", r.error);
    continue;
  }
  if (r.step === 1) {
    write(`${DELIV}/step1-gherkin-response.md`, r.text);
    const gherkin = r.blocks.filter((b) => /gherkin|feature|cucumber/i.test(b.lang));
    gherkin.forEach((b, i) => {
      const pf = pathFromBlock(b.code);
      const name = pf?.path?.split("/").pop() || `atm-withdrawal-${i + 1}.feature`;
      write(`${DELIV}/features/${name}`, (pf?.body ?? b.code).trimEnd() + "\n");
    });
    if (gherkin.length === 0) console.log("WARN: step1 no gherkin block");
  }
  if (r.step === 2) {
    write(`${DELIV}/step2-requirements.md`, r.text);
  }
  if (r.step === 3) {
    write(`${DELIV}/step3-code-response.md`, r.text);
    let placed = 0;
    for (const b of r.blocks) {
      const pf = pathFromBlock(b.code);
      if (pf) {
        write(join(`${DELIV}/java-project`, pf.path), pf.body.trimEnd() + "\n");
        placed++;
      }
    }
    console.log(`step3: placed ${placed}/${r.blocks.length} files with path comments`);
  }
}
console.log("materialize done.");
