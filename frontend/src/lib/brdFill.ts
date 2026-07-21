/** BRD 套版資料抽取:自助理回覆的 ```json fence 找出 brdFill 物件(WP:BRD 套版)。 */

export interface BrdFillData {
  brdFill: boolean;
  values?: Record<string, string>;
  scenarios?: Record<string, string>[];
}

/** 回傳最後一個含 brdFill: true 的 JSON 物件;無則 null。 */
export function extractBrdFill(markdown: string): BrdFillData | null {
  let found: BrdFillData | null = null;
  const fences = markdown.matchAll(/```json\s*([\s\S]*?)```/g);
  for (const m of fences) {
    try {
      const obj = JSON.parse(m[1]);
      if (obj && obj.brdFill === true) found = obj as BrdFillData;
    } catch {
      /* 非合法 JSON fence,略過 */
    }
  }
  return found;
}
