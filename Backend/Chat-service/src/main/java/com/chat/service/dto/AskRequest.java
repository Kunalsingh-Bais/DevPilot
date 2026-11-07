package com.chat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The JSON the client sends to ask a question.
public record AskRequest(

        @NotBlank(message = "github owner is required")
        @Size(max = 100, message = "github owner must be 100 characters or fewer")
        String githubOwner,

        @NotBlank(message = "repo name is required")
        @Size(max = 100, message = "repo name must be 100 characters or fewer")
        String repoName,

        @NotBlank(message = "question is required")
        @Size(max = 1000, message = "question must be 1000 characters or fewer")
        String question
) {
}
