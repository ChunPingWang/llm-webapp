package com.example.llmagent.adapter.out.docx;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import com.example.llmagent.application.port.out.DocumentTextExtractor;

/** 附件文字抽取 adapter:.docx 以 POI,其餘純文字類以 UTF-8 解碼。 */
@Component
public class PoiDocumentTextExtractor implements DocumentTextExtractor {

    private static final Set<String> TEXT_EXTENSIONS =
            Set.of("txt", "md", "markdown", "feature", "csv", "json", "yaml", "yml", "xml");

    @Override
    public boolean supports(String filename) {
        String ext = extension(filename);
        return ext.equals("docx") || TEXT_EXTENSIONS.contains(ext);
    }

    @Override
    public String extractText(String filename, byte[] content) {
        String ext = extension(filename);
        if (ext.equals("docx")) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            } catch (Exception e) {
                throw new IllegalArgumentException("無法解析 Word 附件: " + filename, e);
            }
        }
        if (TEXT_EXTENSIONS.contains(ext)) {
            return new String(content, StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("不支援的附件格式: " + filename);
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
