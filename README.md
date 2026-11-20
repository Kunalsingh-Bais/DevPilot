# DevPilot

DevPilot is a local AI assistant for understanding a GitHub repository. You sign in with GitHub, index a public repository, and ask questions in plain English.
Each answer is based on the code that was retrieved, and each citation links to the excat lines on GitHub at the commit that was indexed.

This is a learning and portfolio project. It is not production software. The limits are listed below.

## Architecture

```
Browser (Angular, localhost:4200)
   |  relative URLs: /api/..., /login/...  (Angular dev proxy)
   ▼ 
API Gateway (8095)
   |  1. removes any X-DevPilot-User header the browser sent
   |  2. checks the session cookie with auth-service
   |  3. adds X-DevPilot-User: <github login>, or returns 401
   |--- /api/auth/**, /login/outh2/**  ─► Auth Service (8084)  GitHub OAuth login, session, /me, logout
   |--- /api/repos/**                  ─► Repository Service (8081)  GitHub REST API (public repos only)
   |--- /api/indexing/**               ─► Indexing Service (8082)    jobs, chunking, embeddings, search
   |--- /api/chat/**                   ─► Chat Service (8083)        prompt, chat model, citations
                                             │
                    ┌────────────────────────┴───────────────┐
                    ▼                                        ▼
         PostgreSQL 16 + pgvector (Docker, 5433)       Ollama (11434)
         repositories, code_chunks, vectors            nomic-embed-text (embeddings)
                                                       qwen2.5-coder:1.5b (chat)
```

### What each service does

| Service            | Port | Responsibility                                                                                                           |
|--------------------|------|--------------------------------------------------------------------------------------------------------------------------|
| Angular frontend   | 4200 | Login page, dashboard, chat screen, sign-out                                                                             |
| API Gateway        | 8095 | Single entry point. Checks the session and sets the trusted user header                                                  |
| Auth Service       | 8084 | GitHub OAuth login (OAuth App, `read:user` scope only), session cookie, logout with CSRF protection                      |
| Repository Service | 8081 | The only service that calls GitHub. Lists repositories, file lists, and file contents. Enforce size and binary-file rule |
| Indexing Service   | 8082 | Background indexing jobs, file filtering, chunking with line numbers, embeddings, pgvector storage, similarity search    |
| Chat Service       | 8083 | Gets the top code chunks, builds a prompt with labelled excerpts, validates citation labels, builds GitHub links         |

##  Key decisions

- **Microservices with clear boundaries.** Only repo-service talks to GITHUB. Only indexing-service touches the database. Only chat-service talks to the chat model. Each service can be tested alone.
- **Local models through Ollama.** No paid API. Embeddings and chat run on the developer's machine. Data never leaves it.
- **PostgreSQL with pgvector** for both normal data and vectors. One database instead of two.
- **Flyway migrations** for every schema change (`V1` creates the tables, `V2` adds per-user ownership). Existing migration files are never edited.
- **Replace, don't append, on reindex.** A reindex deletes the repository's old chunks and inserts new ones in one transaction, so old code never mixes with new.
- **Citations are checked by code.** The model may only cite labels that were given to it. Any other label is removed before the answer is returned.
- **Pinned commits in links.** Citations point to the commit that was indexed, so a link always shows the code that was actually retrieved.
- **Fail closed.** If auth-service is down, the gateway returns 401. If the model doesn't cite a real chunk, the citation is dropped.

## Ownership and isolation

Each indexed repository belongs to one GitHub user (`user_login`). The gateway decides who the user is, and the services filter every query by that user:

- The browser's own `X-DevPilot-User` header is always removed by the gateway.
- The gateway asks auth-service who owns the session cookie. No valid session means no data.
- Indexing-service looks up repositories by user, login, owner, and name. Another user's copy looks like a missing repository (404).
- Search only returns chunks that belong to the signed-in user's copy.

## Scores and experiments

These numbers were measured on `spring-projects/spring-petclinic` with a 10-question set. The questions are in `Indexing-service/src/test/resources/retrieval/eval-questions.txt`.

| Measure          | Score   | What it checks                                                               |
|------------------|---------|------------------------------------------------------------------------------|
| Retrieval, top 5 | 8 of 10 | The expected file appears in the top 5 search results                        |
| Chat citations   | 5 of 10 | The answer cites the expected file (strict: other citations count as misses) |

| Experiment                                                                        | Result                                       | Decision                            |
|-----------------------------------------------------------------------------------|----------------------------------------------|-------------------------------------|
| Query prefix `search query:` for nomic-embed-text                                 | No improvement                               | Removed                             |
| File path added before embedding each chunk                                       | Retrieval 7 -> 8 of 10                       | Kept                                |
| Strict citation format and a repeated citation rule in the prompt                 | Chat 0 -> 5 of 10                            | Kept                                |
| Hybrid search (vector + PostgreSQL full-text, merged with Reciprocal Rank Fusion) | Retrieval stayed 8 of 10, chat dropped 5 → 3 | Reverted                            |
| Chat context of 8 chunks instead of 5                                             | Used for the 5 of 10 result                  | See `TOP_K` in `AnswerService.java` |

The hybrid experiment is kept in git history, so the revert is visible. A 10-question set is small, so one question changes the score by 10 points. These numbers show direction, not a precise measure of quality.

## Limits

- **Trust between services is by header.** Any client that can reach a backend port can send `X-DevPilot-User`. The gateway protects this only for traffic that goes through it. Production would need services that accept only gateway traffic (a shared secret or signed tokens).
- **One shared GitHub token.** Repository calls use `GITHUB_TOKEN`, so all users share one rate limit (5,000 requests per hour). Each file costs one request, so a large repository can exhaust it.
- **Public repositories only.** The OAuth scope is `read:user`, and repo-service reads only public data. Private repositories need a GitHub App.
- **Size limits.** Max 500 files, 2,000 chunks, and 200 KB per file. Files that are binary, not UTF-8, or too large are skipped.
- **Small local model.** `qwen2.5-coder:1.5b` misses some citations. A 3B model is the next thing to measure, using the chat evaluation.
- **Indexing runs in the same process.** A job that was running during a crash is marked FAILED at the next startup, so it can be started again.
- **No streaming and no chat history.** Each answer is returned all at once, and each question stands alone.
- **Not tested with several real users.** Isolation is covered by automated tests and by calls with a different header, but not with a second GitHub account.

## Prerequisites

- Java 21, Node 24 (for Angular), Maven (the `mvnw` wrapper is included), Docker Desktop, and Ollama.
- A GitHub personal access token with public read access, and a GitHub OAuth App (callback URL `http://localhost:8095/login/oauth2/code/github`).

## Setup

1. **Database.** From `infra/`, run `docker compose up -d`. The database listens on host port 5433.
2. **Models.** Run `ollama pull nomic-embed-text` and `ollama pull qwen2.5-coder:1.5b`.
3. **Environment variables.** Set these in each service's run configuration. Never commit them.
    - `GITHUB_TOKEN` for repo-service
    - `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` for auth-service
4. **Start the services**, in this order: `repo-service` (8081), `indexing-service` (8082), `chat-service` (8083), `auth-service` (8084), `api-gateway` (8095).
5. **Start the frontend.** From `frontend/`, run `ng serve`, then open `http://localhost:4200`.
6. **Sign in**, then on the dashboard type an githubOwner and repository name click **Index**. Wait until the status shows Ready, then ask a question. Repositories you index stay in your list after a refresh.

## Testing

```powershell
# Unit and integration tests for each backend service (run inside each service folder).
.\mvnw.cmd test

# Retrieval evaluation: needs Ollama and an indexed petclinic repository.
.\mvnw.cmd test -Dtest=ChunkRetrieverEvalTest

# Chat evaluation: opt-in, needs all services running.
.\mvnw.cmd test -Dtest=ChatAnswerEvalTest -DchatEval=true

# Angular unit tests (run inside frontend/).
ng test --watch=false
```

Evaluation tests skip themselves when their services aren't running, so `mvn test` stays green on any machine.

## Repository layout

```
DevPilot/
├── Backend/
|   ├── infra/                 Docker Compose for PostgreSQL with pgvector
|   ├── Api-gateway/           Spring Cloud Gateway (MVC), session check, header
|   ├── Auth-service/          GitHub OAuth login, session, logout
|   ├── Repo-service/          GitHub client, file list, file content
|   ├── Indexing-service/      jobs, filter, chunker, embeddings, pgvector store, search
|   ├── Chat-service/          prompt builder, citation check, answer endpoint
└── Frontend/                  Angular app: login, dashboard, chat, sign-out
└── README.md
```

## What I learned building it

- Why each service exists, and where its boundary is. Splitting code into services is easy. Deciding what each one owns is the hard part.
- How pgvector stores and searches vectors, and why the embedding size (768) must match the column.
- Why a measured experiment beats a guess. Hybrid search looked promising and was reverted on evidence.
- How citations can be checked in code, even when the model's wording can't.
- How a gateway can enforce identity so that services don't have to handle GitHub login.

## Roadmap (not built)

- Per-user GitHub tokens, or a GitHub App for private repositories.
- Services that accept only gateway traffic, using a shared secret or signed JWTs.
- Streaming answers over Server-Sent Events.
- A 3B chat model, measured with the chat evaluation.
- Docker images for every service, and a full Docker Compose setup.
