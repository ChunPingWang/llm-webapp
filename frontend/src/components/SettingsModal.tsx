import { useEffect, useState } from "react";
import { fetchSettings, updateSettings, type Settings } from "../api";
import { Modal } from "./Modal";

/**
 * 設定視窗:System Prompt 與 LLM API 連線(base URL / token)。
 * Token 欄留空表示維持不變;儲存後僅存於後端記憶體,重啟還原為環境變數值。
 */
export function SettingsModal({ onClose }: { onClose: () => void }) {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">("idle");

  useEffect(() => {
    fetchSettings()
      .then((s) => {
        setSettings(s);
        setSystemPrompt(s.systemPrompt);
        setBaseUrl(s.baseUrl);
      })
      .catch(() => setStatus("error"));
  }, []);

  async function save() {
    setStatus("saving");
    try {
      const s = await updateSettings({
        systemPrompt,
        baseUrl,
        ...(apiKey.trim() ? { apiKey: apiKey.trim() } : {}),
      });
      setSettings(s);
      setSystemPrompt(s.systemPrompt);
      setBaseUrl(s.baseUrl);
      setApiKey("");
      setStatus("saved");
      setTimeout(() => setStatus("idle"), 2000);
    } catch {
      setStatus("error");
    }
  }

  return (
    <Modal
      title="設定"
      onClose={onClose}
      actions={
        <button onClick={save} disabled={status === "saving" || !settings}>
          {status === "saving" ? "儲存中…" : status === "saved" ? "已儲存 ✓" : "儲存"}
        </button>
      }
    >
      <div className="settings-form">
        {!settings && status !== "error" && <p className="empty">載入設定中…</p>}
        {status === "error" && <p className="empty">設定載入或儲存失敗,請稍後再試。</p>}
        {settings && (
          <>
            <section>
              <h3>System Prompt</h3>
              <p className="hint">
                控制 Agent 的行為與產出規範(BDD / DDD / SOLID、範圍限制等)。修改後立即生效於新對話。
              </p>
              <textarea
                className="settings-prompt"
                value={systemPrompt}
                onChange={(e) => setSystemPrompt(e.target.value)}
                rows={16}
                spellCheck={false}
              />
            </section>

            <section>
              <h3>LLM API 連線</h3>
              <p className="hint">
                OpenAI-Compatible Gateway(如 IBM ICA)。修改後下一次呼叫即用新連線;
                設定僅存於伺服器記憶體,重啟還原為環境變數值。
              </p>
              <label className="field">
                <span>API Base URL</span>
                <input
                  type="url"
                  value={baseUrl}
                  onChange={(e) => setBaseUrl(e.target.value)}
                  placeholder="https://api.nextgen-beta.ica.ibm.com/ica"
                  spellCheck={false}
                />
              </label>
              <label className="field">
                <span>API Token(目前:{settings.apiKeyMasked};留空 = 不變更)</span>
                <input
                  type="password"
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  placeholder="輸入新 token 以更換"
                  autoComplete="off"
                />
              </label>
              <p className="hint">預設模型:{settings.defaultModelId}</p>
            </section>
          </>
        )}
      </div>
    </Modal>
  );
}
