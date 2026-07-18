# ADR-003: 思考過程/日誌以 SSE 事件型別分流

## Status
Accepted

## Context
需在畫面同時呈現 thinking、正文、工具呼叫與系統日誌,前端須能分別渲染。

## Decision
SSE event 固定五型:`thinking`、`content`、`tool_call`、`log`、`done`。schema 定義於 `specs/openapi.yaml` components。前端據 event type 分流:thinking → 可摺疊灰底區(完成自動收合);log → 日誌抽屜(INFO/WARN/ERROR 三色);done → usage 與延遲統計。日誌同步落 `audit_logs` 表。

## Consequences
- (+) 前後端契約清晰;新增事件型別為向後相容擴充
- (−) 事件協定為跨端契約,修改需同步 spec 與雙端
