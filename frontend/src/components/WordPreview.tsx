import { useEffect, useRef, useState } from "react";
import { renderAsync } from "docx-preview";
import { renderDocx } from "../api";

/**
 * Word 預覽面板(WP6-T2,ADR-004)。將助理產出的需求文件 Markdown 送後端轉為 .docx,
 * 以 docx-preview 內嵌渲染成 Word 版面,並提供下載。
 */
export function WordPreview({ markdown, title }: { markdown: string; title: string }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<"idle" | "loading" | "ready" | "error">("idle");
  const [blob, setBlob] = useState<Blob | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!markdown.trim()) {
      setStatus("idle");
      setBlob(null);
      return;
    }
    setStatus("loading");
    renderDocx(markdown, title)
      .then(async (b) => {
        if (cancelled) return;
        setBlob(b);
        const container = containerRef.current;
        if (container) {
          container.innerHTML = "";
          await renderAsync(b, container, undefined, {
            className: "docx",
            inWrapper: true,
            ignoreWidth: true, // 於側欄寬度內回流,避免 A4 溢出
            ignoreHeight: true,
            breakPages: false,
          });
        }
        if (!cancelled) setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [markdown, title]);

  function download() {
    if (!blob) return;
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${title}.docx`;
    a.click();
    URL.revokeObjectURL(url);
  }

  if (status === "idle") {
    return <p className="empty">尚無可預覽的文件。當助理回覆一份文件(如業務需求文件)時,會在此以 Word 版面呈現。</p>;
  }

  return (
    <div className="word-preview">
      <div className="word-toolbar">
        <span className="word-status">
          {status === "loading" && "產生 Word 中…"}
          {status === "ready" && "Word 預覽(docx-preview)"}
          {status === "error" && "產生失敗"}
        </span>
        <button onClick={download} disabled={!blob}>下載 .docx</button>
      </div>
      <div className="docx-host" ref={containerRef} />
    </div>
  );
}
