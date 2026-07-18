package com.example.llmagent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import com.example.llmagent.application.event.StreamEvent;
import com.example.llmagent.application.event.StreamEvent.DoneInfo;
import com.example.llmagent.application.event.StreamEvent.UsageInfo;
import com.example.llmagent.application.port.out.ChatCall;
import com.example.llmagent.application.port.out.ChatModelPort;
import com.example.llmagent.application.port.out.ConversationStore;
import com.example.llmagent.domain.chat.ChatChunk;
import com.example.llmagent.domain.chat.Conversation;
import com.example.llmagent.domain.chat.Message;
import com.example.llmagent.domain.chat.Usage;
import com.example.llmagent.domain.thinking.Segment;
import com.example.llmagent.domain.thinking.ThinkingParser;

import reactor.core.publisher.Flux;

/**
 * 對話應用服務:建立對話、記錄使用者訊息、串流模型回應。
 *
 * <p>串流時將 Provider 片段經 {@link ThinkingParser} 分流為 thinking / content 事件,
 * 並於首個 token 記錄 TTFT、結束時記錄 token 用量與耗時(規劃書 §5.1、ADR-003)。
 */
@Service
public class ChatService {

    private final ChatModelPort chatModelPort;
    private final ConversationStore store;
    private final RuntimeSettingsService settings;
    private final AgentProfileService agentProfiles;

    public ChatService(ChatModelPort chatModelPort,
                       ConversationStore store,
                       RuntimeSettingsService settings,
                       AgentProfileService agentProfiles) {
        this.chatModelPort = chatModelPort;
        this.store = store;
        this.settings = settings;
        this.agentProfiles = agentProfiles;
    }

    /**
     * 建立對話。優先序:明示 systemPrompt > Agent Profile(套範本變數)> 全域預設。
     * 指定 agentProfileId 時,模型預設亦取自該 Profile(ADR-006)。
     */
    public Conversation createConversation(String title, String modelId, String systemPrompt,
                                           Double temperature, String agentProfileId,
                                           java.util.Map<String, String> promptVariables) {
        String prompt = systemPrompt;
        String model = modelId;
        if ((prompt == null || prompt.isBlank()) && agentProfileId != null && !agentProfileId.isBlank()) {
            prompt = agentProfiles.renderPrompt(agentProfileId, promptVariables);
            if (model == null || model.isBlank()) {
                model = agentProfiles.findLatest(agentProfileId)
                        .map(p -> p.defaultModelId()).orElse(null);
            }
        }
        Conversation c = new Conversation(
                UUID.randomUUID().toString(),
                title == null || title.isBlank() ? "新對話" : title,
                prompt == null || prompt.isBlank() ? settings.systemPrompt() : prompt,
                model == null || model.isBlank() ? settings.defaultModelId() : model,
                temperature,
                Instant.now());
        store.save(c);
        return c;
    }

    /** 記錄使用者訊息,回傳 messageId(供 /stream 消費)。 */
    public String addUserMessage(String conversationId, String content) {
        Conversation c = store.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("conversation not found: " + conversationId));
        Message m = Message.user(UUID.randomUUID().toString(), content, Instant.now());
        c.addMessage(m);
        store.save(c);
        return m.id();
    }

    /**
     * 串流指定 user 訊息的 assistant 回應,產生五型事件序列。
     * 完成後將 assistant 訊息寫回對話。
     */
    public Flux<StreamEvent> streamAssistant(String userMessageId) {
        return Flux.defer(() -> {
            Conversation c = store.findByMessageId(userMessageId)
                    .orElseThrow(() -> new IllegalArgumentException("message not found: " + userMessageId));
            String model = c.defaultModelId();
            ChatCall call = new ChatCall(
                    model, c.temperature(), c.systemPrompt(),
                    c.historyUpToAndIncluding(userMessageId));

            long start = System.currentTimeMillis();
            AtomicLong ttft = new AtomicLong(-1);
            AtomicReference<Usage> usageRef = new AtomicReference<>(Usage.ZERO);
            ThinkingParser parser = new ThinkingParser();
            StringBuilder assistant = new StringBuilder();

            Flux<StreamEvent> startLog = Flux.just(
                    StreamEvent.log("INFO", "provider", "model=" + model + " 串流開始", ts()));

            Flux<StreamEvent> body = chatModelPort.stream(call).concatMap(chunk -> {
                List<StreamEvent> evs = new ArrayList<>();
                if (chunk.usage() != null) {
                    usageRef.set(chunk.usage());
                }
                String delta = chunk.textDelta();
                if (delta != null && !delta.isEmpty()) {
                    if (ttft.compareAndSet(-1, System.currentTimeMillis() - start)) {
                        evs.add(StreamEvent.log("INFO", "provider", "TTFT=" + ttft.get() + "ms", ts()));
                    }
                    for (Segment s : parser.accept(delta)) {
                        appendSegment(evs, assistant, s);
                    }
                }
                return Flux.fromIterable(evs);
            });

            Flux<StreamEvent> tail = Flux.defer(() -> {
                List<StreamEvent> evs = new ArrayList<>();
                for (Segment s : parser.flush()) {
                    appendSegment(evs, assistant, s);
                }
                c.addMessage(Message.assistant(
                        UUID.randomUUID().toString(), model, assistant.toString(), Instant.now()));
                store.save(c);

                Usage u = usageRef.get();
                long elapsed = System.currentTimeMillis() - start;
                evs.add(StreamEvent.log("INFO", "provider",
                        "完成 tokens=" + u.promptTokens() + "/" + u.completionTokens()
                                + " elapsed=" + elapsed + "ms", ts()));
                evs.add(StreamEvent.done(new DoneInfo(
                        new UsageInfo(u.promptTokens(), u.completionTokens()),
                        elapsed, Math.max(ttft.get(), 0))));
                return Flux.fromIterable(evs);
            });

            return Flux.concat(startLog, body, tail)
                    .onErrorResume(err -> Flux.just(
                            StreamEvent.log("ERROR", "provider",
                                    "串流失敗:" + err.getMessage(), ts()),
                            StreamEvent.done(new DoneInfo(new UsageInfo(0, 0),
                                    System.currentTimeMillis() - start, Math.max(ttft.get(), 0)))));
        });
    }

    private void appendSegment(List<StreamEvent> evs, StringBuilder assistant, Segment s) {
        switch (s.kind()) {
            case THINKING -> evs.add(StreamEvent.thinking(s.text()));
            case CONTENT -> {
                assistant.append(s.text());
                evs.add(StreamEvent.content(s.text()));
            }
            default -> { /* THINKING/CONTENT only from parser */ }
        }
    }

    private static String ts() {
        return Instant.now().toString();
    }
}
