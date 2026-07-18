package com.example.llmagent.domain.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.llmagent.domain.artifact.Artifact.ArtifactType;

/**
 * Code fence 抽取(WP5-T1,ADR-005)。
 *
 * <p>規則:
 * <ul>
 *   <li>```gherkin → GHERKIN;```java → JAVA;其他語言不視為產出物</li>
 *   <li>格式偏差(未閉合 fence)→ 抽取失敗,整段降級為單一 MARKDOWN 產出物並回報 degraded</li>
 *   <li>無任何 fence → 一般對話,無產出物</li>
 * </ul>
 */
public final class ArtifactExtractor {

    private static final Pattern FENCE = Pattern.compile("```([\\w+-]*)\\n(.*?)```", Pattern.DOTALL);
    private static final Pattern FENCE_MARK = Pattern.compile("```");

    private ArtifactExtractor() {
    }

    /** 抽取結果:各片段(型別 + 語言 + 內容)與是否降級。 */
    public record Extraction(List<Piece> pieces, boolean degraded) {
        public record Piece(ArtifactType type, String language, String content) {
        }
    }

    public static Extraction extract(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new Extraction(List.of(), false);
        }
        // 奇數個 ``` 表示 fence 未閉合 → 格式偏差,整段降級(ADR-005 fallback)
        Matcher marks = FENCE_MARK.matcher(markdown);
        int count = 0;
        while (marks.find()) {
            count++;
        }
        if (count % 2 != 0) {
            return new Extraction(
                    List.of(new Extraction.Piece(ArtifactType.MARKDOWN, "markdown", markdown)), true);
        }

        List<Extraction.Piece> pieces = new ArrayList<>();
        Matcher m = FENCE.matcher(markdown);
        while (m.find()) {
            String lang = m.group(1) == null ? "" : m.group(1).toLowerCase();
            String content = m.group(2).replaceAll("\\n$", "");
            switch (lang) {
                case "gherkin", "feature", "cucumber" ->
                        pieces.add(new Extraction.Piece(ArtifactType.GHERKIN, "gherkin", content));
                case "java" ->
                        pieces.add(new Extraction.Piece(ArtifactType.JAVA, "java", content));
                default -> { /* 其他語言不入庫 */ }
            }
        }
        return new Extraction(pieces, false);
    }
}
