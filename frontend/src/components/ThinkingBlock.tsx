import { useEffect, useState } from "react";

/**
 * 思考過程可摺疊區塊(規劃書 §5.1)。串流時自動展開,完成後自動收合。
 */
export function ThinkingBlock({ text, streaming }: { text: string; streaming: boolean }) {
  const [open, setOpen] = useState(true);

  useEffect(() => {
    // 串流結束後自動收合
    if (!streaming) setOpen(false);
  }, [streaming]);

  if (!text) return null;

  return (
    <div className="thinking">
      <button className="thinking-toggle" onClick={() => setOpen((o) => !o)}>
        <span className={`chevron ${open ? "open" : ""}`}>▸</span>
        思考過程{streaming ? "(進行中…)" : ""}
      </button>
      {open && <pre className="thinking-body">{text}</pre>}
    </div>
  );
}
