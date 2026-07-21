import { useEffect, useMemo, useRef, useState } from "react";
import {
  createConversation,
  deleteConversation,
  fetchAgentProfiles,
  fetchModels,
  postMessage,
  streamMessage,
  uploadFile,
  type AgentProfile,
} from "./api";
import { useAutoScroll } from "./lib/useAutoScroll";
import { extractBrdFill } from "./lib/brdFill";
import { AgentProfilesModal } from "./components/AgentProfilesModal";
import { ProvidersModal } from "./components/ProvidersModal";
import { MessageView } from "./components/MessageView";
import { ArtifactPanel } from "./components/ArtifactPanel";
import { LogPanel } from "./components/LogPanel";
import { WordPreview } from "./components/WordPreview";
import { Modal } from "./components/Modal";
import { Markdown } from "./components/Markdown";
import { SettingsModal } from "./components/SettingsModal";
import type { ChatMessage, LogLine, ModelOption } from "./types";

type Tab = "artifacts" | "word" | "logs";

/** 由文件內容取標題:第一個 Markdown 標題,否則預設。 */
function docTitle(markdown: string): string {
  const m = markdown.match(/^#{1,3}\s+(.+)$/m);
  return m ? m[1].trim() : "業務需求文件";
}

/** 產出物浮動視窗標題:依內容為 Gherkin 或 Java 程式碼。 */
function artifactModalTitle(markdown: string): string {
  if (/```java/i.test(markdown)) return "產出程式碼(Java / Cucumber)";
  if (/```gherkin/i.test(markdown)) return "Gherkin 情境(.feature)";
  return "產出物內容";
}

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
  const [tab, setTab] = useState<Tab>("artifacts");
  const [modal, setModal] = useState<null | "word" | "code" | "settings" | "profiles" | "providers">(null);
  const [profiles, setProfiles] = useState<AgentProfile[]>([]);
  const [profileId, setProfileId] = useState<string>(""); // "" = 全域預設 prompt
  const [uploadedDoc, setUploadedDoc] = useState<{ url: string; name: string } | null>(null);
  const [attachments, setAttachments] = useState<{ fileId: string; filename: string }[]>([]);
  const [wordTemplate, setWordTemplate] = useState<{ fileId: string; filename: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const attachInputRef = useRef<HTMLInputElement>(null);
  const templateInputRef = useRef<HTMLInputElement>(null);
  const convId = useRef<string | null>(null);
  const convKey = useRef<string>(""); // model+profile;變更時開新對話
  const esRef = useRef<EventSource | null>(null);

  // Agent Profile 清單(WP2-T6)
  useEffect(() => {
    fetchAgentProfiles().then(setProfiles).catch(() => {});
  }, [modal]); // 管理視窗關閉後重新載入

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

  // 串流時自動捲至底部:對話區與側欄(產出物 / 日誌)各自跟隨(使用者上捲即暫停)
  const messagesRef = useAutoScroll<HTMLDivElement>([messages]);
  const tabBodyRef = useAutoScroll<HTMLDivElement>([lastAssistant?.content, logs.length, tab]);

  // BRD 套版資料(WP:BRD 套版):助理輸出 brdFill JSON 時,Word 預覽改走原模板填寫
  const brdFill = useMemo(
    () => (lastAssistant && !lastAssistant.streaming ? extractBrdFill(lastAssistant.content) : null),
    [lastAssistant],
  );
  const wordTitle =
    brdFill?.values?.DOC_TITLE ?? brdFill?.values?.PROJECT_NAME ??
    (uploadedDoc?.name ?? docTitle(lastAssistant?.content ?? ""));

  function patch(id: string, fn: (m: ChatMessage) => ChatMessage) {
    setMessages((prev) => prev.map((m) => (m.id === id ? fn(m) : m)));
  }

  async function send() {
    const text = input.trim();
    if (!text || sending) return;
    setSending(true);
    setInput("");

    try {
      const vars = { project_name: "llm-webapp", gherkin_locale: "zh-TW" };
      if (!convId.current) {
        convId.current = await createConversation(model, profileId || undefined, vars);
        convKey.current = `${model}|${profileId}`;
      }
      // 對話中切換模型 / Agent(WP3-T3):同一對話,覆寫隨訊息送出
      const key = `${model}|${profileId}`;
      const switched = convKey.current !== key;
      convKey.current = key;
      const attached = attachments;
      const display = attached.length === 0
        ? text
        : `${text}\n\n${attached.map((a) => `📎 ${a.filename}`).join("\n")}`;
      const userMsg: ChatMessage = {
        id: nextId(), role: "user", content: display, thinking: "", logs: [], streaming: false,
      };
      const assistantId = nextId();
      const assistant: ChatMessage = {
        id: assistantId, role: "assistant", content: "", thinking: "", logs: [],
        streaming: true, model,
      };
      setMessages((prev) => [...prev, userMsg, assistant]);

      const messageId = await postMessage(
        convId.current,
        text,
        switched ? model : undefined,
        switched && profileId ? profileId : undefined,
        switched && profileId ? vars : undefined,
        attached.length > 0 ? attached.map((a) => a.fileId) : undefined,
      );
      setAttachments([]);

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

  /** 清除對話與產出物(前端狀態 + 後端資料),重新開始。 */
  async function clearConversation() {
    if (!window.confirm("確定要清除對話與產出物?此動作無法復原。")) return;
    esRef.current?.close();
    esRef.current = null;
    const id = convId.current;
    convId.current = null;
    convKey.current = "";
    setMessages([]);
    setLogs([]);
    setUploadedDoc(null);
    setAttachments([]);
    setWordTemplate(null);
    setSending(false);
    if (id) {
      try {
        await deleteConversation(id);
      } catch {
        // 後端清除失敗不影響前端重新開始;下次送訊息會建立新對話
      }
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
          <label>Agent</label>
          <select value={profileId} onChange={(e) => setProfileId(e.target.value)} disabled={sending}>
            <option value="">（全域預設）</option>
            {profiles.map((p) => (
              <option key={p.id} value={p.id}>{p.name} v{p.version}</option>
            ))}
          </select>
          <button className="settings-btn" onClick={() => setModal("profiles")} title="Agent Profile 管理">
            管理
          </button>
          <label>模型</label>
          <select value={model} onChange={(e) => setModel(e.target.value)} disabled={sending}>
            {models.map((m) => (
              <option key={m.id} value={m.id}>{m.label}</option>
            ))}
          </select>
          <span className="provider-tag">ICA · {models.length} 模型</span>
          <button className="settings-btn" onClick={() => setModal("providers")} title="Provider 管理與連線測試">
            Provider
          </button>
          <button
            className="settings-btn"
            onClick={() => setModal("settings")}
            title="設定(System Prompt / API 連線)"
          >
            ⚙ 設定
          </button>
          <button
            className="settings-btn"
            onClick={clearConversation}
            disabled={messages.length === 0 && !convId.current}
            title="清除對話與產出物,重新開始"
          >
            🗑 清除對話
          </button>
        </div>
      </header>

      <main className="main">
        <section className="chat">
          <div className="messages" ref={messagesRef}>
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
            {attachments.length > 0 && (
              <div className="attachment-chips">
                {attachments.map((a) => (
                  <button
                    key={a.fileId}
                    className="expand-btn"
                    title="移除附件"
                    onClick={() =>
                      setAttachments((prev) => prev.filter((x) => x.fileId !== a.fileId))
                    }
                  >
                    📎 {a.filename} ✕
                  </button>
                ))}
              </div>
            )}
            <input
              ref={attachInputRef}
              type="file"
              accept=".docx,.txt,.md,.markdown,.feature,.csv,.json,.yaml,.yml,.xml"
              style={{ display: "none" }}
              onChange={async (e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                try {
                  const up = await uploadFile(f);
                  setAttachments((prev) => [...prev, { fileId: up.fileId, filename: up.filename }]);
                } catch {
                  setLogs((prev) => [...prev, { level: "ERROR", source: "client", msg: "附件上傳失敗", ts: "" }]);
                }
                e.target.value = "";
              }}
            />
            <button
              className="attach-btn"
              title="上傳附件(內容將提供給模型參考,支援 .docx / 純文字)"
              onClick={() => attachInputRef.current?.click()}
              disabled={sending}
            >
              📎
            </button>
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder="輸入訊息…(Cmd/Ctrl + Enter 送出;📎 可附加 .docx / 文字檔)"
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
            <button className={tab === "word" ? "active" : ""} onClick={() => setTab("word")}>
              Word 預覽
            </button>
            <button className={tab === "logs" ? "active" : ""} onClick={() => setTab("logs")}>
              日誌{logs.length > 0 ? ` (${logs.length})` : ""}
            </button>
          </div>
          <div className="tab-body" ref={tabBodyRef}>
            {tab === "artifacts" && (
              <ArtifactPanel
                sourceMarkdown={lastAssistant?.content ?? ""}
                conversationId={convId.current}
                onExpand={() => setModal("code")}
              />
            )}
            {tab === "word" && (
              <>
                <div className="word-toolbar">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".docx"
                    style={{ display: "none" }}
                    onChange={async (e) => {
                      const f = e.target.files?.[0];
                      if (!f) return;
                      try {
                        const up = await uploadFile(f);
                        setUploadedDoc({ url: up.previewUrl, name: up.filename });
                      } catch {
                        setLogs((prev) => [...prev, { level: "ERROR", source: "client", msg: "上傳失敗", ts: "" }]);
                      }
                      e.target.value = "";
                    }}
                  />
                  <button className="expand-btn" onClick={() => fileInputRef.current?.click()}>
                    ⬆ 上傳 .docx 預覽
                  </button>
                  {uploadedDoc && (
                    <button className="expand-btn" onClick={() => setUploadedDoc(null)}>
                      ✕ {uploadedDoc.name}
                    </button>
                  )}
                  <input
                    ref={templateInputRef}
                    type="file"
                    accept=".docx"
                    style={{ display: "none" }}
                    onChange={async (e) => {
                      const f = e.target.files?.[0];
                      if (!f) return;
                      try {
                        const up = await uploadFile(f);
                        setWordTemplate({ fileId: up.fileId, filename: up.filename });
                      } catch {
                        setLogs((prev) => [...prev, { level: "ERROR", source: "client", msg: "範本上傳失敗", ts: "" }]);
                      }
                      e.target.value = "";
                    }}
                  />
                  <button
                    className="expand-btn"
                    title="上傳 Word 範本套版(支援 {{title}} / {{content}} 佔位)"
                    onClick={() => templateInputRef.current?.click()}
                  >
                    📄 上傳範本
                  </button>
                  {wordTemplate && (
                    <button
                      className="expand-btn"
                      title="移除範本"
                      onClick={() => setWordTemplate(null)}
                    >
                      ✕ 範本:{wordTemplate.filename}
                    </button>
                  )}
                </div>
                <WordPreview
                  markdown={lastAssistant?.streaming ? "" : (lastAssistant?.content ?? "")}
                  title={wordTitle}
                  fileUrl={uploadedDoc?.url}
                  templateFileId={wordTemplate?.fileId}
                  brdFill={brdFill}
                  onExpand={() => setModal("word")}
                />
              </>
            )}
            {tab === "logs" && <LogPanel logs={logs} />}
          </div>
        </aside>
      </main>

      {modal === "word" && (
        <Modal title={uploadedDoc ? `上傳文件 · ${uploadedDoc.name}` : "業務需求文件 · Word 預覽"} onClose={() => setModal(null)}>
          <WordPreview
            variant="modal"
            markdown={lastAssistant?.content ?? ""}
            title={wordTitle}
            fileUrl={uploadedDoc?.url}
            templateFileId={wordTemplate?.fileId}
            brdFill={brdFill}
          />
        </Modal>
      )}
      {modal === "code" && (
        <Modal title={artifactModalTitle(lastAssistant?.content ?? "")} onClose={() => setModal(null)}>
          <Markdown>{lastAssistant?.content ?? ""}</Markdown>
        </Modal>
      )}
      {modal === "settings" && <SettingsModal onClose={() => setModal(null)} />}
      {modal === "profiles" && <AgentProfilesModal onClose={() => setModal(null)} />}
      {modal === "providers" && <ProvidersModal onClose={() => setModal(null)} />}
    </div>
  );
}
