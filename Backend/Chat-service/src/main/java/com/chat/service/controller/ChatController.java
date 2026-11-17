package com.chat.service.controller;

import com.chat.service.dto.AskRequest;
import com.chat.service.dto.AskResponse;
import com.chat.service.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

// Receives questions and hands them to AnswerService
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    static final String USER_HEADER = "X-DevPilot-User";

    private final AnswerService answerService;

    public ChatController(AnswerService answerService) {
        this.answerService = answerService;
    }

// ------ POST -> /api/chat/ask
    @PostMapping("/ask")
    public AskResponse ask(@RequestHeader(USER_HEADER) String userLogin, @Valid @RequestBody AskRequest request) {
        return answerService.ask(userLogin, request.githubOwner(), request.repoName(), request.question());
    }
}
