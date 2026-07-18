package com.example.llmagent.application;

import org.junit.jupiter.api.Test;

import com.example.llmagent.adapter.out.persistence.InMemoryArtifactStore;
import com.example.llmagent.domain.artifact.Artifact.ArtifactType;

import static org.assertj.core.api.Assertions.assertThat;

/** WP5-T1:版本以「對話 × 型別」遞增;WP5-T3:versions 供 diff。 */
class ArtifactServiceTest {

    private final ArtifactService service = new ArtifactService(new InMemoryArtifactStore());

    @Test
    void versionsIncrementPerConversationAndType() {
        String conv = "conv-1";
        service.extractAndStore(conv, "m1", "```gherkin\n功能: v1\n```");
        service.extractAndStore(conv, "m2", "```gherkin\n功能: v2\n```\n```java\nclass A {}\n```");
        // 另一對話不影響版本
        service.extractAndStore("conv-other", "m9", "```gherkin\nx\n```");

        var gherkins = service.versions(conv, ArtifactType.GHERKIN);
        assertThat(gherkins).extracting(a -> a.version()).containsExactly(1, 2);
        assertThat(gherkins.get(1).content()).isEqualTo("功能: v2");

        var javas = service.versions(conv, ArtifactType.JAVA);
        assertThat(javas).extracting(a -> a.version()).containsExactly(1);
    }

    @Test
    void degradedContentStoredAsMarkdownWithFlag() {
        var result = service.extractAndStore("c", "m", "```java\nbroken");
        assertThat(result.degraded()).isTrue();
        assertThat(result.artifacts()).hasSize(1);
        assertThat(result.artifacts().get(0).type()).isEqualTo(ArtifactType.MARKDOWN);
    }
}
