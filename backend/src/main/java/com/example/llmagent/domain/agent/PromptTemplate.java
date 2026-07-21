package com.example.llmagent.domain.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System prompt 範本變數套用(WP2-T4)。支援 {@code {project_name}}、{@code {gherkin_locale}} 等
 * 「小寫開頭」snake_case 形式變數;缺少變數時擲出明確錯誤(列出全部缺漏)。
 * 大寫佔位符(如 BRD 模板之 {@code {{PROJECT_NAME}}})不視為變數,原樣保留供 LLM 填寫。
 */
public final class PromptTemplate {

    private static final Pattern VAR = Pattern.compile("\\{([a-z][a-zA-Z0-9_]*)}");

    private PromptTemplate() {
    }

    /** 回傳範本中出現的變數名(依出現順序,去重)。 */
    public static List<String> variables(String template) {
        List<String> names = new ArrayList<>();
        Matcher m = VAR.matcher(template);
        while (m.find()) {
            if (!names.contains(m.group(1))) {
                names.add(m.group(1));
            }
        }
        return names;
    }

    /**
     * 套用變數。
     *
     * @throws MissingVariableException 有變數未提供時(訊息列出全部缺漏名稱)
     */
    public static String render(String template, Map<String, String> vars) {
        List<String> missing = variables(template).stream()
                .filter(name -> vars == null || vars.get(name) == null)
                .toList();
        if (!missing.isEmpty()) {
            throw new MissingVariableException(missing);
        }
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(vars.get(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 缺少範本變數。 */
    public static class MissingVariableException extends IllegalArgumentException {
        private final List<String> missing;

        public MissingVariableException(List<String> missing) {
            super("缺少 prompt 範本變數: " + String.join(", ", missing));
            this.missing = List.copyOf(missing);
        }

        public List<String> missing() {
            return missing;
        }
    }
}
