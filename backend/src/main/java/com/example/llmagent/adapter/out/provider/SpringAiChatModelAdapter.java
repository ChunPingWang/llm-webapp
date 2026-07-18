package com.example.llmagent.adapter.out.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import com.example.llmagent.application.RuntimeSettingsService;
import com.example.llmagent.application.port.out.ChatCall;
import com.example.llmagent.application.port.out.ChatModelPort;
import com.example.llmagent.domain.chat.ChatChunk;
import com.example.llmagent.domain.chat.Message;

import reactor.core.publisher.Flux;

/**
 * ICA(OpenAI-Compatible)Provider adapter,透過 Spring AI {@link OpenAiChatModel}
 * ({@code ChatModel} 介面)串流呼叫(ADR-001、CLAUDE.md #3)。
 *
 * <p>連線參數(base URL / API key)來自 {@link RuntimeSettingsService},可於執行期修改;
 * 設定版本變更時延遲重建底層 client。
 */
@Component
public class SpringAiChatModelAdapter implements ChatModelPort {

    private final RuntimeSettingsService settings;

    private volatile OpenAiChatModel chatModel;
    private volatile long builtVersion = -1;

    public SpringAiChatModelAdapter(RuntimeSettingsService settings) {
        this.settings = settings;
    }

    private OpenAiChatModel model() {
        long v = settings.version();
        OpenAiChatModel m = chatModel;
        if (m == null || builtVersion != v) {
            synchronized (this) {
                if (chatModel == null || builtVersion != settings.version()) {
                    OpenAiApi api = OpenAiApi.builder()
                            .baseUrl(settings.baseUrl())
                            .apiKey(settings.apiKey())
                            .build();
                    chatModel = OpenAiChatModel.builder()
                            .openAiApi(api)
                            .defaultOptions(OpenAiChatOptions.builder()
                                    .model(settings.defaultModelId())
                                    // ICA(litellm)對 Claude 僅接受 temperature=1
                                    .temperature(1.0)
                                    // 避免完整程式碼/文件被預設 4096 截斷
                                    .maxTokens(32000)
                                    .build())
                            .build();
                    builtVersion = settings.version();
                }
                m = chatModel;
            }
        }
        return m;
    }

    @Override
    public Flux<ChatChunk> stream(ChatCall call) {
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(call.model())
                .streamUsage(true);
        if (call.temperature() != null) {
            options.temperature(call.temperature());
        }

        Prompt prompt = new Prompt(toSpringMessages(call), options.build());
        return model().stream(prompt).map(this::toChunk);
    }

    private List<org.springframework.ai.chat.messages.Message> toSpringMessages(ChatCall call) {
        List<org.springframework.ai.chat.messages.Message> msgs = new ArrayList<>();
        if (call.systemPrompt() != null && !call.systemPrompt().isBlank()) {
            msgs.add(new SystemMessage(call.systemPrompt()));
        }
        for (Message m : call.history()) {
            switch (m.role()) {
                case USER -> msgs.add(new UserMessage(m.content()));
                case ASSISTANT -> msgs.add(new AssistantMessage(m.content()));
                case SYSTEM -> msgs.add(new SystemMessage(m.content()));
            }
        }
        return msgs;
    }

    private ChatChunk toChunk(ChatResponse response) {
        String text = "";
        if (response.getResult() != null && response.getResult().getOutput() != null) {
            String t = response.getResult().getOutput().getText();
            if (t != null) {
                text = t;
            }
        }
        com.example.llmagent.domain.chat.Usage usage = null;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            Usage u = response.getMetadata().getUsage();
            int total = intValue(u.getTotalTokens());
            if (total > 0) {
                usage = new com.example.llmagent.domain.chat.Usage(
                        intValue(u.getPromptTokens()), intValue(u.getCompletionTokens()));
            }
        }
        return new ChatChunk(text, usage);
    }

    private static int intValue(Number v) {
        return v == null ? 0 : v.intValue();
    }
}
