package com.example.llmagent.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.llmagent.adapter.in.web.dto.ChatDtos.CreateConversationRequest;
import com.example.llmagent.adapter.in.web.dto.ChatDtos.CreateConversationResponse;
import com.example.llmagent.adapter.in.web.dto.ChatDtos.PostMessageRequest;
import com.example.llmagent.adapter.in.web.dto.ChatDtos.PostMessageResponse;
import com.example.llmagent.application.ChatService;
import com.example.llmagent.application.event.StreamEvent;
import com.example.llmagent.domain.chat.Conversation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

/**
 * Chat REST + SSE 端點(WP3-T1)。契約見 specs/openapi.yaml。
 * 串流事件五型(ADR-003):thinking / content / tool_call / log / done。
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateConversationResponse createConversation(@RequestBody CreateConversationRequest req) {
        Conversation c = chatService.createConversation(
                req.title(), req.modelId(), req.systemPrompt(), req.temperature(),
                req.agentProfileId(), req.promptVariables());
        return new CreateConversationResponse(c.id(), c.defaultModelId(), c.title());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public PostMessageResponse postMessage(@PathVariable String conversationId,
                                           @Valid @RequestBody PostMessageRequest req) {
        String messageId = chatService.addUserMessage(conversationId, req.content(),
                req.modelId(), req.agentProfileId(), req.promptVariables());
        return new PostMessageResponse(messageId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable String conversationId) {
        chatService.deleteConversation(conversationId);
    }

    @GetMapping(path = "/messages/{messageId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable String messageId) {
        return chatService.streamAssistant(messageId).map(this::toSse);
    }

    private ServerSentEvent<String> toSse(StreamEvent event) {
        return ServerSentEvent.<String>builder()
                .event(event.type().wireName())
                .data(writeJson(event.payload()))
                .build();
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization\"}";
        }
    }
}
