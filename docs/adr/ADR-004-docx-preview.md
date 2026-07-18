# ADR-004: Word 預覽採前端 docx-preview,伺服器端不轉檔

## Status
Accepted

## Context
.docx 需瀏覽器內預覽。候選:docx-preview(前端)、mammoth.js、LibreOffice headless→PDF、OnlyOffice。

## Decision
首選前端 `docx-preview` 直接渲染(保真度中高、零維運)。複雜文件保真度不足時,Phase 2 加 LibreOffice headless → PDF + pdf.js 作伺服器端 fallback。系統產生之 Word 以 Apache POI 產出、存 MinIO,走同一預覽管線。

## Consequences
- (+) 無額外服務、無授權疑慮
- (−) 極複雜排版可能走樣;驗收前以客戶實際文件做保真度測試
