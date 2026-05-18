package com.conceptclarity.controller;

import com.conceptclarity.dto.ChatRequest;
import com.conceptclarity.dto.ChatResponse;
import com.conceptclarity.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping({"/chat", "/api/chat"})
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
