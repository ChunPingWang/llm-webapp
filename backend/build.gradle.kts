plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

extra["springAiVersion"] = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

dependencies {
    // Web / SSE (reactive per ADR-002: Reactor Flux<ServerSentEvent>)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // AI 抽象:Spring AI OpenAI ChatModel(ADR-001)。ICA 為 OpenAI-Compatible Gateway。
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Word (.docx) 產生(WP6-T3,ADR-004):後端以 Apache POI 產生,前端 docx-preview 預覽。
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
