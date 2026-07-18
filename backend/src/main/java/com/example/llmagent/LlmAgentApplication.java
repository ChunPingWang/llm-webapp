package com.example.llmagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * LLM Agent Web Platform — 應用進入點。
 *
 * <p>架構採 Hexagonal(見 CLAUDE.md):{@code domain} 不依賴 Spring,
 * Provider / DB / MinIO 皆為 adapter。本類別僅負責啟動 Spring 容器。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LlmAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmAgentApplication.class, args);
    }
}
