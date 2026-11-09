package com.chat.service.controller;

import com.chat.service.dto.AskRequest;
import com.chat.service.dto.AskResponse;
import com.chat.service.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Receives questions and hands them to AnswerService
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AnswerService answerService;

    public ChatController(AnswerService answerService) {
        this.answerService = answerService;
    }

// ------ POST -> /api/chat/ask
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return answerService.ask(request.githubOwner(), request.repoName(), request.question());
    }
}
