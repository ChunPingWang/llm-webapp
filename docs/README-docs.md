# llm-webapp-docs — Claude Code 消費指南

將本資料夾放入專案根目錄後,依下列順序使用:

1. `CLAUDE.md` → 複製至專案根目錄(Claude Code 每次啟動自動讀取)
2. `tasks/TASKS.md` → 指派任務給 Claude Code 的來源,例:
   `> 請完成 TASKS.md 中的 WP1-T1,遵守 CLAUDE.md 的架構約束`
3. `adr/` → 任務引用之架構決策,Claude Code 實作前應先讀對應 ADR
4. `specs/openapi.yaml` → API 契約先行,實作端點前先核對/更新本檔
5. `00-評估規劃書.md` → 完整背景與設計脈絡(人閱讀為主)

建議首個指令:
```
請讀取 CLAUDE.md 與 tasks/TASKS.md,確認理解後從 Phase 0 開始,
每完成一個任務即勾選並回報,不要一次跨多個任務。
```
