package com.example.llmagent.adapter.out.docx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.example.llmagent.application.port.out.DocxRenderer;

/**
 * Apache POI 版 Word 產生器(WP6-T3)。將需求文件 Markdown 子集轉為 .docx:
 * 標題({@code #}~{@code ####})、表格({@code | a | b |})、清單、粗體、引用、程式碼區塊、段落。
 */
@Component
public class PoiDocxRenderer implements DocxRenderer {

    private static final String CJK_FONT = "Noto Sans CJK TC";
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*]\\s+(.*)");
    private static final Pattern NUMBERED = Pattern.compile("^\\s*\\d+\\.\\s+(.*)");

    @Override
    public byte[] render(String title, String markdown) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            addFormatted(titleP, title, 24, true);

            List<String> lines = markdown.replace("\r\n", "\n").lines().toList();
            int i = 0;
            while (i < lines.size()) {
                String line = stripTrailing(lines.get(i));
                if (line.isBlank()) { i++; continue; }

                if (line.strip().startsWith("```")) {
                    i = renderCodeBlock(doc, lines, i);
                    continue;
                }
                if (line.strip().startsWith("|") && line.indexOf('|', 1) > 0) {
                    i = renderTable(doc, lines, i);
                    continue;
                }
                if (line.strip().equals("---") || line.strip().equals("***")) { i++; continue; }

                Matcher h = HEADING.matcher(line);
                if (h.matches()) {
                    addHeading(doc, h.group(1).length(), h.group(2));
                    i++;
                    continue;
                }
                if (line.stripLeading().startsWith(">")) {
                    XWPFParagraph q = doc.createParagraph();
                    q.setIndentationLeft(360);
                    addFormatted(q, line.stripLeading().substring(1).strip(), 11, false).setItalic(true);
                    i++;
                    continue;
                }
                Matcher b = BULLET.matcher(line);
                if (b.matches()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(360);
                    addFormatted(p, "• " + b.group(1), 11, false);
                    i++;
                    continue;
                }
                Matcher n = NUMBERED.matcher(line);
                if (n.matches()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(360);
                    addFormatted(p, line.strip(), 11, false);
                    i++;
                    continue;
                }
                addFormatted(doc.createParagraph(), line, 11, false);
                i++;
            }

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("docx 產生失敗", e);
        }
    }

    private void addHeading(XWPFDocument doc, int level, String text) {
        int size = switch (Math.min(level, 4)) {
            case 1 -> 18;
            case 2 -> 15;
            case 3 -> 13;
            default -> 12;
        };
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        p.setSpacingAfter(80);
        addFormatted(p, text, size, true);
    }

    private int renderCodeBlock(XWPFDocument doc, List<String> lines, int start) {
        int i = start + 1;
        List<String> code = new ArrayList<>();
        while (i < lines.size() && !lines.get(i).strip().startsWith("```")) {
            code.add(lines.get(i));
            i++;
        }
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(240);
        XWPFRun r = p.createRun();
        r.setFontFamily("Consolas");
        r.setFontSize(10);
        for (int k = 0; k < code.size(); k++) {
            if (k > 0) r.addBreak();
            r.setText(code.get(k));
        }
        return i < lines.size() ? i + 1 : i; // 跳過結尾 ```
    }

    private int renderTable(XWPFDocument doc, List<String> lines, int start) {
        List<String[]> rows = new ArrayList<>();
        int i = start;
        while (i < lines.size() && lines.get(i).strip().startsWith("|")) {
            String raw = lines.get(i).strip();
            raw = raw.substring(1, raw.endsWith("|") ? raw.length() - 1 : raw.length());
            String[] cells = raw.split("\\|", -1);
            for (int c = 0; c < cells.length; c++) cells[c] = cells[c].strip();
            if (!String.join("", cells).replaceAll("[\\s:\\-]", "").isEmpty()) {
                rows.add(cells); // 略過 |---|---| 分隔列
            }
            i++;
        }
        if (rows.isEmpty()) return i;
        int cols = rows.stream().mapToInt(r -> r.length).max().orElse(1);

        XWPFTable table = doc.createTable(rows.size(), cols);
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "999999");
        for (int r = 0; r < rows.size(); r++) {
            XWPFTableRow row = table.getRow(r);
            for (int c = 0; c < cols; c++) {
                XWPFTableCell cell = row.getCell(c);
                XWPFParagraph p = cell.getParagraphs().get(0);
                addFormatted(p, c < rows.get(r).length ? rows.get(r)[c] : "", 10, r == 0);
            }
        }
        return i;
    }

    /** 依 **粗體** 切分為多個 run;回傳最後一個 run(供額外設定)。 */
    private XWPFRun addFormatted(XWPFParagraph p, String text, int size, boolean bold) {
        XWPFRun last = null;
        Matcher m = BOLD.matcher(text);
        int idx = 0;
        while (m.find()) {
            if (m.start() > idx) last = run(p, text.substring(idx, m.start()), size, bold);
            last = run(p, m.group(1), size, true);
            idx = m.end();
        }
        if (idx < text.length() || last == null) last = run(p, text.substring(idx), size, bold);
        return last;
    }

    private XWPFRun run(XWPFParagraph p, String text, int size, boolean bold) {
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(size);
        r.setFontFamily(CJK_FONT);
        r.setFontFamily(CJK_FONT, XWPFRun.FontCharRange.eastAsia);
        return r;
    }

    private static String stripTrailing(String s) {
        return s.replaceAll("\\s+$", "");
    }
}
