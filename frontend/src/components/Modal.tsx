import type { ReactNode } from "react";

/** 浮動視窗(overlay)。.modal-body 為捲動容器,供錄影緩慢下滑展示內容。 */
export function Modal({
  title,
  onClose,
  actions,
  children,
}: {
  title: string;
  onClose: () => void;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">{title}</span>
          <div className="modal-actions">
            {actions}
            <button className="modal-close" onClick={onClose} aria-label="關閉">
              ✕
            </button>
          </div>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
