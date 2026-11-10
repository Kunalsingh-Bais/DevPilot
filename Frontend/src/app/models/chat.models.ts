// The JSON we send to POST /api/chat/ask
export interface AskRequest {
    githubOwner: string;
    repoName: string;
    question: string;
}

// A cited-location
export interface Citation {
    label: string;
    filePath: string;
    startLine: number;
    endLine: number;
    githubUrl: string;
}

// One chunk the search found.
export interface Source {
    label: string;
    filePath: string;
    startLine: number;
    endLine: number;
    githubUrl: string;
    cited: boolean;
}

// The full answer from chat-service
export interface AskResponse {
    answer: string;
    commitSha: string;
    citations: Citation[];
    sources: Source[];
}