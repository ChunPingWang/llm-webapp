import { useEffect, useMemo, useRef, useState } from "react";
import { createConversation, fetchModels, postMessage, streamMessage } from "./api";
import { MessageView } from "./components/MessageView";
import { ArtifactPanel } from "./components/ArtifactPanel";
import { LogPanel } from "./components/LogPanel";
import type { ChatMessage, LogLine, ModelOption } from "./types";

// 後端 /api/providers/ica/models 不可用時的後備清單。
const FALLBACK_MODELS: ModelOption[] = [
  { id: "claude-opus-4-8", label: "claude-opus-4-8" },
  { id: "claude-sonnet-5", label: "claude-sonnet-5" },
];
const PREFERRED_DEFAULT = "claude-opus-4-8";

let uid = 0;
const nextId = () => `local-${uid++}`;

export function App() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [input, setInput] = useState("");
  const [models, setModels] = useState<ModelOption[]>(FALLBACK_MODELS);
  const [model, setModel] = useState(PREFERRED_DEFAULT);
  const [sending, setSending] = useState(false);
  const [tab, setTab] = useState<"artifacts" | "logs">("artifacts");
  const convId = useRef<string | null>(null);
  const esRef = useRef<EventSource | null>(null);

  // 開啟時動態拉取 ICA 模型清單(WP2-T2);失敗則沿用後備清單。
  useEffect(() => {
    fetchModels("ica")
      .then((list) => {
        if (list.length === 0) return;
        setModels(list);
        setModel((cur) =>
          list.some((m) => m.id === cur)
            ? cur
            : (list.find((m) => m.id === PREFERRED_DEFAULT)?.id ?? list[0].id),
        );
      })
      .catch(() => {
        /* 保留後備清單 */
      });
  }, []);

  const lastAssistant = useMemo(
    () => [...messages].reverse().find((m) => m.role === "assistant"),
    [messages],
  );

  function patch(id: string, fn: (m: ChatMessage) => ChatMessage) {
    setMessages((prev) => prev.map((m) => (m.id === id ? fn(m) : m)));
  }

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    setSending(true);
    setInput("");

    try {
      if (!convId.current) {
        convId.current = await createConversation(model);
      }
      const userMsg: ChatMessage = {
        id: nextId(), role: "user", content: text, thinking: "", logs: [], streaming: false,
      };
      const assistantId = nextId();
      const assistant: ChatMessage = {
        id: assistantId, role: "assistant", content: "", thinking: "", logs: [],
        streaming: true, model,
      };
      setMessages((prev) => [...prev, userMsg, assistant]);

      const messageId = await postMessage(convId.current, text);

      esRef.current = streamMessage(messageId, {
        onThinking: (d) => patch(assistantId, (m) => ({ ...m, thinking: m.thinking + d })),
        onContent: (d) => patch(assistantId, (m) => ({ ...m, content: m.content + d })),
        onLog: (line) => {
          setLogs((prev) => [...prev, line]);
          patch(assistantId, (m) => ({ ...m, logs: [...m.logs, line] }));
        },
        onDone: (info) => {
          patch(assistantId, (m) => ({ ...m, done: info, streaming: false }));
          setSending(false);
        },
        onError: () => {
          patch(assistantId, (m) => ({ ...m, streaming: false }));
          setLogs((prev) => [...prev, { level: "ERROR", source: "client", msg: "串流連線中斷", ts: "" }]);
          setSending(false);
        },
      });
    } catch (err) {
      setLogs((prev) => [...prev, { level: "ERROR", source: "client", msg: String(err), ts: "" }]);
      setSending(false);
    }
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      send();
    }
  }

  return (
    <div className="layout">
      <header className="topbar">
        <h1>LLM Agent 平台</h1>
        <div className="model-picker">
          <label>模型</label>
          <select value={model} onChange={(e) => setModel(e.target.value)} disabled={sending}>
            {models.map((m) => (
              <option key={m.id} value={m.id}>{m.label}</option>
            ))}
          </select>
          <span className="provider-tag">ICA · {models.length} 模型</span>
        </div>
      </header>

      <main className="main">
        <section className="chat">
          <div className="messages">
            {messages.length === 0 && (
              <div className="welcome">
                <h2>開始對話</h2>
                <p>透過 IBM ICA 呼叫最新 Claude 模型。試著請它產生一段繁體中文 Gherkin 或 Java 程式碼,
                  產出物會出現在右側 Artifacts 分頁。</p>
              </div>
            )}
            {messages.map((m) => (
              <MessageView key={m.id} message={m} />
            ))}
          </div>
          <div className="composer">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder="輸入訊息…(Cmd/Ctrl + Enter 送出)"
              rows={3}
              disabled={sending}
            />
            <button onClick={send} disabled={sending || !input.trim()}>
              {sending ? "串流中…" : "送出"}
            </button>
          </div>
        </section>

        <aside className="sidebar">
          <div className="tabs">
            <button className={tab === "artifacts" ? "active" : ""} onClick={() => setTab("artifacts")}>
              Artifacts
            </button>
            <button className={tab === "logs" ? "active" : ""} onClick={() => setTab("logs")}>
              日誌{logs.length > 0 ? ` (${logs.length})` : ""}
            </button>
          </div>
          <div className="tab-body">
            {tab === "artifacts" ? (
              <ArtifactPanel sourceMarkdown={lastAssistant?.content ?? ""} />
            ) : (
              <LogPanel logs={logs} />
            )}
          </div>
        </aside>
      </main>
    </div>
  );
}
