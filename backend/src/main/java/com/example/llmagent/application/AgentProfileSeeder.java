package com.example.llmagent.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.llmagent.application.port.out.DocumentTextExtractor;

/**
 * 內建 Agent Profile(規劃書 §4.5):BDD 規格 / Java 產碼 / Code Review(儲存為空時種子化),
 * 以及步驟二「BRD 業務文件 Agent」(以名稱冪等種子化:System Prompt = BRD 填寫 Prompt +
 * BRD Word 模板全文,資源見 {@code resources/seed/})。
 */
@Configuration
public class AgentProfileSeeder {

    /** 步驟二(Gherkin 轉業務文件)內建 Profile 名稱;以名稱判斷是否已種子化。 */
    public static final String BRD_PROFILE_NAME = "BRD 業務文件 Agent";

    private static final String COMMON_RULES = """
            【最重要 — 範圍限制】只輸出使用者「當前這則訊息」明確要求的產物,不要主動附加未被要求的內容。
            多個步驟請分次進行,勿在前一步驟就預先產生後續步驟的產物。回答精確、聚焦。""";

    @Bean
    public ApplicationRunner seedAgentProfiles(AgentProfileService service, DocumentTextExtractor extractor) {
        return args -> {
            if (service.listLatest().isEmpty()) {
                seedBaseProfiles(service);
            }
            seedBrdProfile(service, extractor);
        };
    }

    private void seedBaseProfiles(AgentProfileService service) {
        service.create("BDD 規格 Agent",
                "將需求情境轉為 zh-TW Gherkin(正向 + 反向)",
                COMMON_RULES + """

                        你專精 BDD。產生 Gherkin 時使用 {gherkin_locale} 關鍵字(功能:、場景:、場景大綱:、假設、當、那麼、而且、但是),
                        同時涵蓋正向與反向情境;以 ```gherkin 區塊呈現,第一行以註解標明 # language: zh-TW 與檔名。
                        專案名稱:{project_name}。""",
                "claude-opus-4-8", 1.0, List.of());
        service.create("Java 產碼 Agent",
                "依 Gherkin/需求產生 Java 21 + Cucumber(DDD/SOLID,涵蓋率 ≥ 80%)",
                COMMON_RULES + """

                        你專精 Java 21、Cucumber、DDD 與 SOLID。產生程式碼時每個檔案獨立一個 code fence,
                        首行以註解標明相對路徑;遵循 DDD 分層(domain / application / infrastructure)與 SOLID;
                        Cucumber step definitions 使用英文 annotation(@Given/@When/@Then/@And,io.cucumber.java.en.*),
                        feature 檔保留繁體中文;提供 JUnit 5 runner 與單元測試,涵蓋率 ≥ 80%;實作完整不省略。""",
                "claude-opus-4-8", 1.0, List.of());
        service.create("Code Review Agent",
                "審查程式碼:正確性、DDD/SOLID、測試涵蓋",
                COMMON_RULES + """

                        你是資深 reviewer。針對提供的程式碼指出:正確性問題(附重現情境)、
                        DDD/SOLID 違反、缺漏的測試;以嚴重度排序,每項附具體修正建議與檔案位置。""",
                "claude-opus-4-8", 1.0, List.of());
    }

    /** 步驟二 BRD Profile:BRD 填寫 Prompt + 模板全文(模板 {{大寫}} 佔位由 LLM 填寫,非範本變數)。 */
    void seedBrdProfile(AgentProfileService service, DocumentTextExtractor extractor) {
        boolean exists = service.listLatest().stream()
                .anyMatch(p -> BRD_PROFILE_NAME.equals(p.name()));
        if (exists) {
            return;
        }
        String prompt = readResource("/seed/brd-prompt.md");
        String templateText = extractor.extractText("brd-template.docx", readResourceBytes("/seed/brd-template.docx"));
        String systemPrompt = prompt
                + "\n\n---\n\n## BRD 模板全文(依此結構與規約填寫;以 Markdown 輸出完整內容)\n\n"
                + templateText;
        service.create(BRD_PROFILE_NAME,
                "步驟二:依 Gherkin 填寫 BRD 模板,產出業務需求文件(Markdown → Word 預覽)",
                systemPrompt, "claude-opus-4-8", 1.0, List.of());
    }

    private String readResource(String path) {
        return new String(readResourceBytes(path), StandardCharsets.UTF_8);
    }

    private byte[] readResourceBytes(String path) {
        try (InputStream in = AgentProfileSeeder.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("找不到內建資源: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("讀取內建資源失敗: " + path, e);
        }
    }
}
