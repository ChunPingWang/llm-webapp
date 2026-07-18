package com.example.llmagent.application;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.llmagent.adapter.out.persistence.InMemoryConversationStore;
import com.example.llmagent.application.event.StreamEvent;
import com.example.llmagent.application.event.StreamEvent.ContentDelta;
import com.example.llmagent.application.event.StreamEvent.DoneInfo;
import com.example.llmagent.application.event.StreamEvent.ThinkingDelta;
import com.example.llmagent.application.port.out.ChatCall;
import com.example.llmagent.application.port.out.ChatModelPort;
import com.example.llmagent.domain.chat.ChatChunk;
import com.example.llmagent.domain.chat.Conversation;
import com.example.llmagent.domain.chat.Usage;
import com.example.llmagent.domain.sse.SseEventType;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

    private final RuntimeSettingsService settings = new RuntimeSettingsService(
            new ChatProperties("claude-opus-4-8", "system"), "http://localhost", "test-key");

    @Test
    void streamsThinkingContentAndDone() {
        ChatModelPort fakePort = (ChatCall call) -> Flux.just(
                ChatChunk.text("<think>reasoning</think>"),
                ChatChunk.text("Hello, "),
                ChatChunk.text("world"),
                ChatChunk.finalUsage(new Usage(12, 7)));

        InMemoryConversationStore store = new InMemoryConversationStore();
        ChatService service = new ChatService(fakePort, store, settings,
                new AgentProfileService(new com.example.llmagent.adapter.out.persistence.InMemoryAgentProfileStore(), null),
                new ArtifactService(new com.example.llmagent.adapter.out.persistence.InMemoryArtifactStore()),
                new com.example.llmagent.adapter.out.persistence.InMemoryAuditLogStore(),
                io.micrometer.observation.ObservationRegistry.create());

        Conversation c = service.createConversation("t", null, null, null, null, null);
        String messageId = service.addUserMessage(c.id(), "hi");

        List<StreamEvent> events = service.streamAssistant(messageId).collectList().block();
        assertThat(events).isNotNull();

        String thinking = events.stream()
                .filter(e -> e.type() == SseEventType.THINKING)
                .map(e -> ((ThinkingDelta) e.payload()).delta())
                .reduce("", String::concat);
        String content = events.stream()
                .filter(e -> e.type() == SseEventType.CONTENT)
                .map(e -> ((ContentDelta) e.payload()).delta())
                .reduce("", String::concat);

        assertThat(thinking).isEqualTo("reasoning");
        assertThat(content).isEqualTo("Hello, world");

        StreamEvent done = events.get(events.size() - 1);
        assertThat(done.type()).isEqualTo(SseEventType.DONE);
        DoneInfo info = (DoneInfo) done.payload();
        assertThat(info.usage().promptTokens()).isEqualTo(12);
        assertThat(info.usage().completionTokens()).isEqualTo(7);

        // assistant 訊息應寫回對話
        Conversation reloaded = store.findById(c.id()).orElseThrow();
        assertThat(reloaded.messages()).anyMatch(m -> "Hello, world".equals(m.content()));
    }
}
