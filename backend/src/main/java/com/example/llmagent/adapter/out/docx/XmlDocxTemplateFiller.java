package com.example.llmagent.adapter.out.docx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import com.example.llmagent.application.port.out.DocxTemplateFiller;

/**
 * 「LLM 填寫式」Word 模板套版引擎(BRD 套版)。直接操作 docx zip 內的 XML 字串,
 * 不重建版面,原模板樣式(字型、封面、表格、頁首頁尾、TOC)完全保留。支援:
 * <ul>
 *   <li>{@code {{KEY}}} 佔位符置換(值內換行轉 {@code <w:br/>})</li>
 *   <li>索引族展開:{@code X_1} 明列、{@code X_N} 樣板列/段依資料筆數複製
 *       (同列字面 {@code BR-0n}/{@code n.} 一併改為實際編號)</li>
 *   <li>◤重複區塊開始:名稱◢ … ◤重複區塊結束◢ 依資料展開(區塊內可再含索引族)</li>
 *   <li>刪除【填寫指引】段落、「模板使用說明」整章、未填寫之殘留佔位段/列</li>
 * </ul>
 * 前提(與內建 BRD 模板相符):佔位符完整位於單一 {@code <w:t>} 內;無巢狀表格。
 */
@Component
public class XmlDocxTemplateFiller implements DocxTemplateFiller {

    private static final Pattern INDEXED_N =
            Pattern.compile("\\{\\{([A-Z0-9_]+?)_N((?:_[A-Z0-9_]+)?)}}");
    private static final Pattern P_STYLE = Pattern.compile("<w:pStyle w:val=\"([^\"]+)\"");
    private static final String GUIDE_PREFIX = "【填寫指引】";
    private static final String BLOCK_START = "◤重複區塊開始";
    private static final String BLOCK_END = "◤重複區塊結束";

    @Override
    public byte[] fill(byte[] templateDocx, Map<String, String> values,
                       Map<String, List<Map<String, String>>> repeats) {
        Map<String, String> vals = values == null ? Map.of() : values;
        Map<String, List<Map<String, String>>> reps = repeats == null ? Map.of() : repeats;
        try {
            Map<String, byte[]> entries = readZip(templateDocx);
            byte[] doc = entries.get("word/document.xml");
            if (doc == null) {
                throw new IllegalArgumentException("非有效 docx:缺少 word/document.xml");
            }
            entries.put("word/document.xml",
                    processDocument(new String(doc, StandardCharsets.UTF_8), vals, reps)
                            .getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (e.getKey().matches("word/(header|footer)\\d*\\.xml")) {
                    e.setValue(substitute(new String(e.getValue(), StandardCharsets.UTF_8), vals)
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
            return writeZip(entries);
        } catch (IOException e) {
            throw new UncheckedIOException("BRD 模板套版失敗", e);
        }
    }

    // ---------- document pipeline ----------

    private String processDocument(String xml, Map<String, String> values,
                                   Map<String, List<Map<String, String>>> repeats) {
        int bodyStart = xml.indexOf("<w:body>");
        int bodyEnd = xml.lastIndexOf("</w:body>");
        if (bodyStart < 0 || bodyEnd < 0) {
            throw new IllegalArgumentException("document.xml 缺少 <w:body>");
        }
        bodyStart += "<w:body>".length();
        List<String> els = tokenize(xml.substring(bodyStart, bodyEnd));
        els = removeChapter(els, "模板使用說明");
        els = expandRepeatBlocks(els, repeats);
        els = expandIndexed(els, values);
        els.replaceAll(el -> substitute(el, values));
        els = cleanup(els);
        return xml.substring(0, bodyStart) + String.join("", els) + xml.substring(bodyEnd);
    }

    /** body 頂層元素切分(w:p / w:tbl / w:sdt / 其他),原樣保序重組。 */
    private List<String> tokenize(String body) {
        List<String> out = new ArrayList<>();
        int pos = 0;
        int n = body.length();
        while (pos < n) {
            int lt = body.indexOf('<', pos);
            if (lt < 0) {
                break;
            }
            if (lt > pos) {
                out.add(body.substring(pos, lt));
            }
            int nameEnd = lt + 1;
            while (nameEnd < n && (Character.isLetterOrDigit(body.charAt(nameEnd)) || body.charAt(nameEnd) == ':')) {
                nameEnd++;
            }
            String tag = body.substring(lt + 1, nameEnd);
            int gt = body.indexOf('>', lt);
            int end;
            if (body.charAt(gt - 1) == '/') {
                end = gt + 1;
            } else if (tag.equals("w:sdt")) {
                end = findMatchingClose(body, lt, "w:sdt");
            } else {
                end = body.indexOf("</" + tag + ">", lt);
                if (end < 0) {
                    throw new IllegalArgumentException("XML 結構異常:未閉合 <" + tag + ">");
                }
                end += tag.length() + 3;
            }
            out.add(body.substring(lt, end));
            pos = end;
        }
        return out;
    }

    /** 巢狀感知搜尋閉合標籤(w:sdt 可巢狀;同字首標籤如 sdtContent 須跳過)。 */
    private int findMatchingClose(String s, int start, String tag) {
        String openTok = "<" + tag;
        String closeTok = "</" + tag + ">";
        int depth = 0;
        int pos = start;
        while (pos < s.length()) {
            int open = s.indexOf(openTok, pos);
            while (open >= 0 && open + openTok.length() < s.length()) {
                char c = s.charAt(open + openTok.length());
                if (c == ' ' || c == '>' || c == '/') {
                    break;
                }
                open = s.indexOf(openTok, open + openTok.length());
            }
            int close = s.indexOf(closeTok, pos);
            if (close < 0) {
                throw new IllegalArgumentException("XML 結構異常:未閉合 <" + tag + ">");
            }
            if (open >= 0 && open < close) {
                depth++;
                pos = open + openTok.length();
            } else {
                depth--;
                pos = close + closeTok.length();
                if (depth == 0) {
                    return pos;
                }
            }
        }
        throw new IllegalArgumentException("XML 結構異常:未閉合 <" + tag + ">");
    }

    /** 刪除指定標題起至下一個 Heading1 前的整章。 */
    private List<String> removeChapter(List<String> els, String chapterTitle) {
        int from = -1;
        int to = els.size();
        for (int i = 0; i < els.size(); i++) {
            String el = els.get(i);
            if (!isParagraph(el)) {
                continue;
            }
            boolean h1 = "Heading1".equals(styleOf(el));
            if (from < 0 && h1 && textOf(el).contains(chapterTitle)) {
                from = i;
            } else if (from >= 0 && h1) {
                to = i;
                break;
            }
        }
        if (from < 0) {
            return els;
        }
        List<String> out = new ArrayList<>(els.subList(0, from));
        out.addAll(els.subList(to, els.size()));
        return out;
    }

    // ---------- repeat blocks ----------

    private List<String> expandRepeatBlocks(List<String> els, Map<String, List<Map<String, String>>> repeats) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < els.size()) {
            String el = els.get(i);
            if (!(isParagraph(el) && textOf(el).contains(BLOCK_START))) {
                out.add(el);
                i++;
                continue;
            }
            int end = -1;
            for (int j = i + 1; j < els.size(); j++) {
                if (isParagraph(els.get(j)) && textOf(els.get(j)).contains(BLOCK_END)) {
                    end = j;
                    break;
                }
            }
            if (end < 0) {
                out.add(el);
                i++;
                continue;
            }
            List<String> chunk = new ArrayList<>(els.subList(i + 1, end));
            for (Map<String, String> item : resolveRepeatItems(textOf(el), repeats)) {
                List<String> copy = expandIndexed(new ArrayList<>(chunk), item);
                copy.replaceAll(c -> substitute(c, item));
                out.addAll(copy);
            }
            i = end + 1; // 略過起訖標記與原樣板
        }
        return out;
    }

    /** 由起始標記文字解析區塊名稱(「開始:名稱(說明)◢」),對應 repeats 資料。 */
    private List<Map<String, String>> resolveRepeatItems(
            String markerText, Map<String, List<Map<String, String>>> repeats) {
        String name = markerText.substring(markerText.indexOf(BLOCK_START) + BLOCK_START.length());
        name = name.replaceFirst("^[::]", "");
        for (String cut : new String[]{"(", "(", "◢"}) {
            int idx = name.indexOf(cut);
            if (idx >= 0) {
                name = name.substring(0, idx);
            }
        }
        name = name.strip();
        List<Map<String, String>> items = repeats.get(name);
        if (items == null && repeats.size() == 1) {
            items = repeats.values().iterator().next();
        }
        return items == null ? List.of() : items;
    }

    // ---------- indexed family expansion ----------

    private List<String> expandIndexed(List<String> els, Map<String, String> values) {
        String context = String.join("", els);
        List<String> out = new ArrayList<>();
        for (String el : els) {
            if (isTable(el)) {
                out.add(expandIndexedInTable(el, values));
            } else if (isParagraph(el) && INDEXED_N.matcher(el).find()) {
                out.addAll(expandElement(el, values, context));
            } else {
                out.add(el);
            }
        }
        return out;
    }

    /** _N 樣板元素 → 依缺漏索引複製(無缺漏則移除;殘留佔位由 cleanup 收尾)。 */
    private List<String> expandElement(String el, Map<String, String> values, String explicitScope) {
        Set<String> families = familiesOf(el);
        int maxData = maxDataIndex(values, families);
        int maxExplicit = maxExplicitIndex(explicitScope, families);
        List<String> out = new ArrayList<>();
        for (int idx = maxExplicit + 1; idx <= maxData; idx++) {
            out.add(withIndex(el, idx));
        }
        return out;
    }

    private String expandIndexedInTable(String tbl, Map<String, String> values) {
        int firstRow = tbl.indexOf("<w:tr");
        if (firstRow < 0 || !INDEXED_N.matcher(tbl).find()) {
            return tbl;
        }
        StringBuilder sb = new StringBuilder(tbl.substring(0, firstRow));
        int pos = firstRow;
        String tail = "";
        while (pos < tbl.length()) {
            int start = tbl.indexOf("<w:tr", pos);
            if (start < 0) {
                tail = tbl.substring(pos);
                break;
            }
            int end = tbl.indexOf("</w:tr>", start) + "</w:tr>".length();
            String row = tbl.substring(start, end);
            pos = end;

            if (!INDEXED_N.matcher(row).find()) {
                sb.append(row);
                continue;
            }
            // 同列已含 _1(如 SC_STEP):段落為複製單位;否則整列為複製單位(如 TERM)
            boolean paragraphUnit = familiesOf(row).stream()
                    .anyMatch(f -> Pattern.compile("\\{\\{" + Pattern.quote(f) + "_\\d").matcher(row).find());
            if (paragraphUnit) {
                sb.append(expandParagraphsInRow(row, values));
            } else {
                for (String expanded : expandElement(row, values, tbl)) {
                    sb.append(expanded);
                }
            }
        }
        return sb.append(tail).toString();
    }

    /** 列內段落級展開(同儲存格 _1…_N 條列,如前置條件/主要流程/預期結果)。 */
    private String expandParagraphsInRow(String row, Map<String, String> values) {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (pos < row.length()) {
            int start = indexOfParagraph(row, pos);
            if (start < 0) {
                sb.append(row.substring(pos));
                break;
            }
            int end = row.indexOf("</w:p>", start);
            end = end < 0 ? row.length() : end + "</w:p>".length();
            sb.append(row, pos, start);
            String p = row.substring(start, end);
            if (INDEXED_N.matcher(p).find()) {
                for (String expanded : expandElement(p, values, row)) {
                    sb.append(expanded);
                }
            } else {
                sb.append(p);
            }
            pos = end;
        }
        return sb.toString();
    }

    private Set<String> familiesOf(String el) {
        Set<String> fams = new LinkedHashSet<>();
        Matcher m = INDEXED_N.matcher(el);
        while (m.find()) {
            fams.add(m.group(1));
        }
        return fams;
    }

    private int maxDataIndex(Map<String, String> values, Set<String> families) {
        int max = 0;
        for (String fam : families) {
            Pattern key = Pattern.compile("^" + Pattern.quote(fam) + "_(\\d+)(?:_[A-Z0-9_]+)?$");
            for (String k : values.keySet()) {
                Matcher m = key.matcher(k);
                if (m.matches()) {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                }
            }
        }
        return max;
    }

    private int maxExplicitIndex(String scope, Set<String> families) {
        int max = 0;
        for (String fam : families) {
            Matcher m = Pattern.compile("\\{\\{" + Pattern.quote(fam) + "_(\\d+)(?:_[A-Z0-9_]+)?}}")
                    .matcher(scope);
            while (m.find()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        return max;
    }

    /** 產生第 idx 份複本:_N 佔位改為 _idx;同列字面 BR-0n / n. 改為實際編號。 */
    private String withIndex(String el, int idx) {
        String out = INDEXED_N.matcher(el).replaceAll("{{$1_" + idx + "$2}}");
        out = out.replaceAll("([A-Z]{2,10}-)0n", "$1" + String.format("%02d", idx));
        out = out.replaceAll("(<w:t[^>]*>)n\\.", "$1" + idx + ".");
        return out;
    }

    // ---------- substitution ----------

    private String substitute(String el, Map<String, String> values) {
        if (!el.contains("{{")) {
            return el;
        }
        String out = el;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String token = "{{" + e.getKey() + "}}";
            if (out.contains(token)) {
                out = out.replace(token, xmlValue(e.getValue()));
            }
        }
        return out;
    }

    /** XML 轉義;值內換行以 <w:br/> 呈現(於同一 run 內斷行)。 */
    private String xmlValue(String value) {
        String escaped = (value == null ? "" : value)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return escaped.replace("\r\n", "\n")
                .replace("\n", "</w:t><w:br/><w:t xml:space=\"preserve\">");
    }

    // ---------- cleanup ----------

    private List<String> cleanup(List<String> els) {
        List<String> out = new ArrayList<>();
        for (String el : els) {
            if (isParagraph(el)) {
                if (!isRemovableParagraph(el)) {
                    out.add(el);
                }
            } else if (isTable(el)) {
                out.add(cleanupTable(el));
            } else {
                out.add(el);
            }
        }
        return out;
    }

    private boolean isRemovableParagraph(String p) {
        String text = textOf(p);
        return text.contains("{{") || text.strip().startsWith(GUIDE_PREFIX) || text.contains("◤");
    }

    private String cleanupTable(String tbl) {
        int firstRow = tbl.indexOf("<w:tr");
        if (firstRow < 0) {
            return tbl;
        }
        StringBuilder sb = new StringBuilder(tbl.substring(0, firstRow));
        int pos = firstRow;
        String tail = "";
        int kept = 0;
        while (pos < tbl.length()) {
            int start = tbl.indexOf("<w:tr", pos);
            if (start < 0) {
                tail = tbl.substring(pos);
                break;
            }
            int end = tbl.indexOf("</w:tr>", start) + "</w:tr>".length();
            String row = tbl.substring(start, end);
            pos = end;

            row = removeRemovableCellParagraphs(row);
            if (!row.contains("{{")) {
                sb.append(row);
                kept++;
            }
            // 仍含佔位符 → 整列未填寫,捨棄
        }
        // 全部列被捨棄(如未填寫之場景大綱表)→ 連表格一併移除,避免空表格造成無效 OOXML
        return kept == 0 ? "" : sb.append(tail).toString();
    }

    /** 移除儲存格內的指引段與「混合儲存格」中的殘留佔位段;儲存格至少保留一個段落。 */
    private String removeRemovableCellParagraphs(String row) {
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (pos < row.length()) {
            int start = row.indexOf("<w:tc", pos);
            if (start < 0 || row.charAt(row.indexOf('>', start) - 1) == '/') {
                sb.append(row.substring(pos));
                break;
            }
            int end = row.indexOf("</w:tc>", start) + "</w:tc>".length();
            sb.append(row, pos, start);
            sb.append(cleanupCell(row.substring(start, end)));
            pos = end;
        }
        return sb.toString();
    }

    private String cleanupCell(String cell) {
        List<String> paragraphs = new ArrayList<>();
        List<int[]> spans = new ArrayList<>();
        int pos = 0;
        while (pos < cell.length()) {
            int start = indexOfParagraph(cell, pos);
            if (start < 0) {
                break;
            }
            int end = cell.indexOf("</w:p>", start);
            end = end < 0 ? cell.length() : end + "</w:p>".length();
            paragraphs.add(cell.substring(start, end));
            spans.add(new int[]{start, end});
            pos = end;
        }
        if (paragraphs.isEmpty()) {
            return cell;
        }
        boolean hasCleanText = paragraphs.stream()
                .anyMatch(p -> !textOf(p).isBlank() && !p.contains("{{") && !textOf(p).strip().startsWith(GUIDE_PREFIX));
        StringBuilder sb = new StringBuilder();
        int prev = 0;
        int kept = 0;
        for (int i = 0; i < paragraphs.size(); i++) {
            String p = paragraphs.get(i);
            String text = textOf(p).strip();
            boolean drop = text.startsWith(GUIDE_PREFIX) || (hasCleanText && p.contains("{{"));
            sb.append(cell, prev, spans.get(i)[0]);
            if (!drop) {
                sb.append(p);
                kept++;
            }
            prev = spans.get(i)[1];
        }
        sb.append(cell.substring(prev));
        if (kept == 0) {
            int close = sb.lastIndexOf("</w:tc>");
            sb.insert(close, "<w:p/>");
        }
        return sb.toString();
    }

    // ---------- helpers ----------

    /** 找下一個段落起點(排除 <w:pPr>、<w:pStyle> 等同字首標籤)。 */
    private int indexOfParagraph(String s, int from) {
        int pos = from;
        while (pos < s.length()) {
            int idx = s.indexOf("<w:p", pos);
            if (idx < 0) {
                return -1;
            }
            char c = s.charAt(idx + 4);
            if (c == ' ' || c == '>' || c == '/') {
                return idx;
            }
            pos = idx + 4;
        }
        return -1;
    }

    private boolean isParagraph(String el) {
        return el.startsWith("<w:p ") || el.startsWith("<w:p>") || el.startsWith("<w:p/>");
    }

    private boolean isTable(String el) {
        return el.startsWith("<w:tbl");
    }

    private String textOf(String el) {
        return el.replaceAll("<[^>]+>", "");
    }

    private String styleOf(String p) {
        Matcher m = P_STYLE.matcher(p);
        return m.find() ? m.group(1) : "";
    }

    // ---------- zip io ----------

    private Map<String, byte[]> readZip(byte[] bytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                entries.put(e.getName(), zin.readAllBytes());
            }
        }
        return entries;
    }

    private byte[] writeZip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zout.putNextEntry(new ZipEntry(e.getKey()));
                zout.write(e.getValue());
                zout.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
