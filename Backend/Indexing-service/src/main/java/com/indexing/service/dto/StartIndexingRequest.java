package com.indexing.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// JSON body the client sends to start an indexing job.
public record StartIndexingRequest (

        @NotBlank(message = "Github owner is required")
        @Size(max = 100, message = "Github owner must be 100 characters or fewer")
        String githubOwner,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "repo name must be 100 characters or fewer")
        String repoName
)
{

}

