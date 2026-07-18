export type ArtifactType = "GHERKIN" | "JAVA" | "MARKDOWN";

export interface Artifact {
  index: number;
  lang: string;
  type: ArtifactType;
  ext: "feature" | "java" | "md";
  content: string;
}

const FENCE = /```([\w+-]*)\n([\s\S]*?)```/g;

function classify(lang: string): { type: ArtifactType; ext: Artifact["ext"] } {
  const l = lang.toLowerCase();
  if (l === "gherkin" || l === "feature" || l === "cucumber") {
    return { type: "GHERKIN", ext: "feature" };
  }
  if (l === "java") {
    return { type: "JAVA", ext: "java" };
  }
  return { type: "MARKDOWN", ext: "md" };
}

/**
 * 從 assistant 的 Markdown 內容抽取產出物(R2–R4)。目前收集 Gherkin 與 Java code fence,
 * 其餘語言歸為 MARKDOWN。與後端 ArtifactService(WP5-T1)邏輯對齊,前端先行呈現。
 */
export function extractArtifacts(markdown: string): Artifact[] {
  const out: Artifact[] = [];
  let m: RegExpExecArray | null;
  let i = 0;
  FENCE.lastIndex = 0;
  while ((m = FENCE.exec(markdown)) !== null) {
    const lang = m[1] || "text";
    const content = m[2].replace(/\n$/, "");
    const { type, ext } = classify(lang);
    if (type === "MARKDOWN") continue; // 僅將 Gherkin / Java 視為產出物
    out.push({ index: i++, lang, type, ext, content });
  }
  return out;
}

export function downloadArtifact(a: Artifact): void {
  const blob = new Blob([a.content], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `artifact-${a.index + 1}.${a.ext}`;
  link.click();
  URL.revokeObjectURL(url);
}
