// 依 marks.json(錄影時間戳)與影片總長,產生對齊的 steps.ass 字幕。
import { readFileSync, writeFileSync } from "node:fs";
import { execSync } from "node:child_process";

const OUT = process.env.OUT_DIR || "./out";
const VIDEO = process.env.VIDEO || `${OUT}/demo.mp4`;
const marks = JSON.parse(readFileSync(`${OUT}/marks.json`, "utf-8"));
const at = (label) => marks.find((m) => m.label === label)?.t ?? null;

const dur = Number(execSync(`ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "${VIDEO}"`).toString().trim());

const s1 = at("step1 send"), s2 = at("step2 send"), s3 = at("step3 send");
const b1 = s1 ?? 2.5, b2 = s2 ?? dur * 0.33, b3 = s3 ?? dur * 0.66;

function tc(sec) {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = (sec % 60).toFixed(2).padStart(5, "0");
  return `${h}:${String(m).padStart(2, "0")}:${s}`;
}

const captions = [
  [0, b1, "啟動 LLM Agent 平台(透過 IBM ICA 呼叫 claude-opus-4-8)"],
  [b1, b2, "Step 1｜將富邦銀行 ATM 提款情境轉為 Gherkin(含正向 / 反向條件),完成後緩慢捲動檢視"],
  [b2, b3, "Step 2｜產生業務需求文件,以浮動視窗「Word 預覽」呈現並緩慢捲動"],
  [b3, dur, "Step 3｜產生 Java 21 + Cucumber 程式碼(DDD/SOLID,英文 annotation),浮動視窗檢視,涵蓋率 100%"],
];
const tags = [
  [0, b1, "● 準備中"],
  [b1, b2, "● Step 1 / 3　Gherkin"],
  [b2, b3, "● Step 2 / 3　業務需求文件 · Word 預覽"],
  [b3, dur, "● Step 3 / 3　Java + Cucumber"],
];

const head = `[Script Info]
ScriptType: v4.00+
PlayResX: 1440
PlayResY: 900
WrapStyle: 0
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Caption, Noto Sans CJK TC, 28, &H00FFFFFF, &H00FFFFFF, &H00202020, &HB0202020, -1, 0, 0, 0, 100, 100, 0, 0, 3, 6, 0, 2, 50, 50, 40, 1
Style: Tag, Noto Sans CJK TC, 26, &H0033D6FF, &H0033D6FF, &H00202020, &HB0202020, -1, 0, 0, 0, 100, 100, 0, 0, 3, 6, 0, 8, 50, 50, 34, 1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
`;

let body = "";
for (const [a, b, t] of captions) body += `Dialogue: 0, ${tc(a)}, ${tc(b)}, Caption, , 0, 0, 0, , ${t}\n`;
for (const [a, b, t] of tags) body += `Dialogue: 0, ${tc(a)}, ${tc(b)}, Tag, , 0, 0, 0, , ${t}\n`;

writeFileSync(`${OUT}/steps.ass`, head + body);
console.log("steps.ass written. boundaries:", { b1, b2, b3, dur });
