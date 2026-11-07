package com.chat.service.dto;

import java.util.List;

// The JSON we return
public record AskResponse(
        String answer,
        String commitSha,
        List<Citation> citations
) {

    // One cited location, with a link to the exact lines on GitHub.
    public record Citation(
            String label,             // for example "C1"
            String filePath,
            int startLine,
            int endLine,
            String githubUrl         // points at the same commit we answered from
    ) {
    }
}
