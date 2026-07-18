import { useState } from "react";

interface LogLine {
  event: string;
  data: string;
}

/**
 * Step 1 scaffold 首頁:驗證前端 → Vite proxy → 後端 SSE 管線(WP1-T4)。
 * Chat / Thinking / Artifact 面板於後續 step 實作。
 */
export function App() {
  const [lines, setLines] = useState<LogLine[]>([]);
  const [running, setRunning] = useState(false);

  function runPing() {
    setLines([]);
    setRunning(true);
    const es = new EventSource("/api/ping/stream");
    const push = (event: string) => (e: MessageEvent) =>
      setLines((prev) => [...prev, { event, data: e.data }]);
    es.addEventListener("log", push("log"));
    es.addEventListener("done", (e) => {
      push("done")(e as MessageEvent);
      es.close();
      setRunning(false);
    });
    es.onerror = () => {
      es.close();
      setRunning(false);
    };
  }

  return (
    <main className="app">
      <header>
        <h1>LLM Agent 平台</h1>
        <p className="subtitle">
          多模型串接 · Gherkin / Java 產出物 · 思考過程即時串流
        </p>
      </header>

      <section className="card">
        <h2>SSE 管線自我檢測</h2>
        <p>
          點擊按鈕向後端 <code>/api/ping/stream</code> 建立 event-stream 連線,
          驗證串流管線(前端 → Vite proxy → Spring WebFlux)。
        </p>
        <button onClick={runPing} disabled={running}>
          {running ? "串流中…" : "執行 Ping 串流"}
        </button>
        <ul className="log">
          {lines.map((l, i) => (
            <li key={i}>
              <span className={`tag tag-${l.event}`}>{l.event}</span>
              <code>{l.data}</code>
            </li>
          ))}
        </ul>
      </section>

      <footer>
        <span>ICA 最新 Claude 模型 · 預設 claude-opus-4-8</span>
      </footer>
    </main>
  );
}
