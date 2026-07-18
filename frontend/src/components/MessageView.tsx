import type { ChatMessage } from "../types";
import { Markdown } from "./Markdown";
import { ThinkingBlock } from "./ThinkingBlock";

export function MessageView({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <div className={`msg ${isUser ? "msg-user" : "msg-assistant"}`}>
      <div className="msg-role">
        {isUser ? "你" : `助理${message.model ? ` · ${message.model}` : ""}`}
      </div>
      {!isUser && <ThinkingBlock text={message.thinking} streaming={message.streaming} />}
      <div className="msg-body">
        {isUser ? (
          <p className="user-text">{message.content}</p>
        ) : (
          <>
            <Markdown>{message.content || (message.streaming ? "…" : "")}</Markdown>
            {message.streaming && <span className="cursor">▍</span>}
          </>
        )}
      </div>
      {message.done && (
        <div className="msg-meta">
          TTFT {message.done.ttftMs}ms · {message.done.elapsedMs}ms · tokens{" "}
          {message.done.usage.promptTokens}/{message.done.usage.completionTokens}
        </div>
      )}
    </div>
  );
}
