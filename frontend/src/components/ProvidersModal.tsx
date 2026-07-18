import { useEffect, useState } from "react";
import {
  fetchProviders,
  registerProvider,
  testProvider,
  type ProviderInfo,
  type ProviderTestResult,
} from "../api";
import { Modal } from "./Modal";

/**
 * Provider 管理視窗(WP2-T1/T5):註冊 OLLAMA / OPENAI_COMPATIBLE / ANTHROPIC,
 * apiKeyRef 僅存環境變數「名稱」(不落地金鑰);每個 Provider 可做連線測試。
 */
export function ProvidersModal({ onClose }: { onClose: () => void }) {
  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [tests, setTests] = useState<Record<string, ProviderTestResult | "testing">>({});
  const [id, setId] = useState("");
  const [type, setType] = useState("OPENAI_COMPATIBLE");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKeyRef, setApiKeyRef] = useState("");
  const [err, setErr] = useState("");

  async function reload() {
    setProviders(await fetchProviders());
  }

  useEffect(() => {
    reload().catch(() => setErr("載入失敗"));
  }, []);

  async function add() {
    setErr("");
    try {
      await registerProvider({ id, type, baseUrl, apiKeyRef: apiKeyRef || undefined });
      setId("");
      setBaseUrl("");
      setApiKeyRef("");
      await reload();
    } catch {
      setErr("新增失敗(id 重複或欄位不合法?)");
    }
  }

  async function runTest(pid: string) {
    setTests((t) => ({ ...t, [pid]: "testing" }));
    try {
      const r = await testProvider(pid);
      setTests((t) => ({ ...t, [pid]: r }));
    } catch {
      setTests((t) => ({ ...t, [pid]: { ok: false, latencyMs: 0, models: 0, error: "請求失敗" } }));
    }
  }

  return (
    <Modal title="Provider 管理" onClose={onClose}>
      <div className="settings-form">
        <section>
          <h3>已註冊 Provider</h3>
          <table className="providers-table">
            <thead>
              <tr><th>id</th><th>型別</th><th>Base URL</th><th>Key 參照</th><th>連線測試</th></tr>
            </thead>
            <tbody>
              {providers.map((p) => {
                const t = tests[p.id];
                return (
                  <tr key={p.id}>
                    <td>{p.id}</td>
                    <td>{p.type}</td>
                    <td className="mono">{p.baseUrl}</td>
                    <td className="mono">{p.apiKeyRef ?? "—"}</td>
                    <td>
                      <button className="expand-btn" onClick={() => runTest(p.id)}>測試</button>{" "}
                      {t === "testing" && <span className="word-status">測試中…</span>}
                      {t && t !== "testing" && (
                        <span className={t.ok ? "test-ok" : "test-fail"}>
                          {t.ok ? `✓ ${t.latencyMs}ms · ${t.models} 模型` : `✗ ${t.error}`}
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </section>

        <section>
          <h3>新增 Provider</h3>
          <p className="hint">API Key 一律填「環境變數名稱」(如 ICA_CLAUDE_KEY),不儲存明碼金鑰。</p>
          <div className="provider-form">
            <input placeholder="id(如 local-ollama)" value={id} onChange={(e) => setId(e.target.value)} />
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="OPENAI_COMPATIBLE">OPENAI_COMPATIBLE</option>
              <option value="OLLAMA">OLLAMA</option>
              <option value="ANTHROPIC">ANTHROPIC</option>
            </select>
            <input placeholder="Base URL" value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} />
            <input placeholder="API Key 環境變數名(選填)" value={apiKeyRef} onChange={(e) => setApiKeyRef(e.target.value)} />
            <button onClick={add} disabled={!id.trim() || !baseUrl.trim()}>新增</button>
          </div>
          {err && <p className="empty">{err}</p>}
        </section>
      </div>
    </Modal>
  );
}
