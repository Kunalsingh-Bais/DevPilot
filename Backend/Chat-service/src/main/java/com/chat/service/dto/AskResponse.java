package com.chat.service.dto;

import java.util.List;

// The JSON we return
public record AskResponse(
        String answer,
        String commitSha,
        List<Citation> citations,
        List<Source> sources
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

    // Every chunk the search returned. "cited" is true only when the answer used this label
    public record Source(
            String label,
            String filePath,
            int startLine,
            int endLine,
            String githubUrl,
            boolean cited
    ) {
    }
}
