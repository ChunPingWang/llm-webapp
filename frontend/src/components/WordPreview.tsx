import { useEffect, useRef, useState } from "react";
import { renderAsync } from "docx-preview";
import { renderDocx } from "../api";

/**
 * Word 預覽面板(WP6-T2,ADR-004)。將助理產出的需求文件 Markdown 送後端轉為 .docx,
 * 以 docx-preview 內嵌渲染成 Word 版面,並提供下載。
 *
 * variant="modal" 時內容以自然高度呈現,由外層 .modal-body 捲動(供錄影緩慢下滑)。
 */
export function WordPreview({
  markdown,
  title,
  fileUrl,
  templateFileId,
  variant = "panel",
  onExpand,
}: {
  markdown: string;
  title: string;
  /** 直接預覽既有 .docx(如上傳檔,WP6-T2);設定時優先於 markdown 轉檔。 */
  fileUrl?: string | null;
  /** Word 範本 fileId:markdown 轉檔時以範本套版({{title}} / {{content}} 佔位)。 */
  templateFileId?: string | null;
  variant?: "panel" | "modal";
  onExpand?: () => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState<"idle" | "loading" | "ready" | "error">("idle");
  const [blob, setBlob] = useState<Blob | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (!fileUrl && !markdown.trim()) {
      setStatus("idle");
      setBlob(null);
      return;
    }
    setStatus("loading");
    const source: Promise<Blob> = fileUrl
      ? fetch(fileUrl).then((r) => {
          if (!r.ok) throw new Error(`fetch docx failed: ${r.status}`);
          return r.blob();
        })
      : renderDocx(markdown, title, templateFileId ?? undefined);
    source
      .then(async (b) => {
        if (cancelled) return;
        setBlob(b);
        const container = containerRef.current;
        if (container) {
          container.innerHTML = "";
          await renderAsync(b, container, undefined, {
            className: "docx",
            inWrapper: true,
            ignoreWidth: true,
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
  }, [markdown, title, fileUrl, templateFileId]);

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
        <div style={{ display: "flex", gap: "0.4rem" }}>
          {onExpand && (
            <button className="expand-btn" onClick={onExpand}>⤢ 放大</button>
          )}
          <button onClick={download} disabled={!blob}>下載 .docx</button>
        </div>
      </div>
      <div className={`docx-host${variant === "modal" ? " docx-host--flow" : ""}`} ref={containerRef} />
    </div>
  );
}
