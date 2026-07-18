import type { LogLine } from "../types";

/** 日誌面板:INFO / WARN / ERROR 三色(規劃書 §5.1)。 */
export function LogPanel({ logs }: { logs: LogLine[] }) {
  if (logs.length === 0) {
    return <p className="empty">尚無日誌。送出訊息後,Provider 呼叫、TTFT、token 用量會顯示於此。</p>;
  }
  return (
    <ul className="logs">
      {logs.map((l, i) => (
        <li key={i} className={`log-${l.level.toLowerCase()}`}>
          <span className="log-level">{l.level}</span>
          <span className="log-source">{l.source}</span>
          <span className="log-msg">{l.msg}</span>
        </li>
      ))}
    </ul>
  );
}
