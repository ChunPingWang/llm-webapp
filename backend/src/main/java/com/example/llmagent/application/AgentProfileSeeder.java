package com.example.llmagent.application;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 內建三個 Agent Profile(規劃書 §4.5):BDD 規格 / Java 產碼 / Code Review。
 * 僅在儲存為空時種子化(冪等)。
 */
@Configuration
public class AgentProfileSeeder {

    private static final String COMMON_RULES = """
            【最重要 — 範圍限制】只輸出使用者「當前這則訊息」明確要求的產物,不要主動附加未被要求的內容。
            多個步驟請分次進行,勿在前一步驟就預先產生後續步驟的產物。回答精確、聚焦。""";

    @Bean
    ApplicationRunner seedAgentProfiles(AgentProfileService service) {
        return args -> {
            if (!service.listLatest().isEmpty()) {
                return;
            }
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
        };
    }
}
