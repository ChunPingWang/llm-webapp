import type { DoneInfo, LogLine } from "./types";

/** 建立對話,回傳 conversationId。 */
export async function createConversation(modelId: string): Promise<string> {
  const res = await fetch("/api/conversations", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ title: "對話", modelId }),
  });
  if (!res.ok) throw new Error(`createConversation failed: ${res.status}`);
  return (await res.json()).conversationId;
}

/** 送出使用者訊息,回傳 messageId(串流另走 stream)。 */
export async function postMessage(conversationId: string, content: string): Promise<string> {
  const res = await fetch(`/api/conversations/${conversationId}/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error(`postMessage failed: ${res.status}`);
  return (await res.json()).messageId;
}

export interface StreamHandlers {
  onThinking: (delta: string) => void;
  onContent: (delta: string) => void;
  onLog: (line: LogLine) => void;
  onDone: (info: DoneInfo) => void;
  onError: (err: unknown) => void;
}

/** 開啟 SSE 串流。回傳 EventSource,呼叫端可於卸載時 close()。 */
export function streamMessage(messageId: string, h: StreamHandlers): EventSource {
  const es = new EventSource(`/api/messages/${messageId}/stream`);
  es.addEventListener("thinking", (e) => h.onThinking(JSON.parse((e as MessageEvent).data).delta));
  es.addEventListener("content", (e) => h.onContent(JSON.parse((e as MessageEvent).data).delta));
  es.addEventListener("log", (e) => h.onLog(JSON.parse((e as MessageEvent).data) as LogLine));
  es.addEventListener("done", (e) => {
    h.onDone(JSON.parse((e as MessageEvent).data) as DoneInfo);
    es.close();
  });
  es.onerror = (e) => {
    // done 事件後由伺服器關閉連線,EventSource 會觸發 error;僅在仍開啟時視為錯誤。
    if (es.readyState !== EventSource.CLOSED) {
      h.onError(e);
    }
    es.close();
  };
  return es;
}
