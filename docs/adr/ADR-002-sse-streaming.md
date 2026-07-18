# ADR-002: 串流採 SSE 而非 WebSocket

## Status
Accepted

## Context
對話回應為伺服器→客戶端單向串流;企業環境 Proxy/WAF 對 WebSocket 支援不一。

## Decision
`POST /api/chat` 建立訊息後,以 `GET /api/chat/{id}/stream` 回傳 `text/event-stream`(Reactor `Flux<ServerSentEvent>`)。支援 `Last-Event-ID` 斷線重連。

## Consequences
- (+) 實作簡單、Proxy 相容性佳、天然重連
- (−) 單向;若未來需雙向(如中途注入指令)再評估 WebSocket
