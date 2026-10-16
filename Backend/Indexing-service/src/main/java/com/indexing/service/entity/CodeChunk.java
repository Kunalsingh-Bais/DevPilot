package com.indexing.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "code_chunks")
public class CodeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "start_line", nullable = false)
    private int startLine;

    @Column(name = "end_Line", nullable = false)
    private int endLine;

    @Column(nullable = false)
    private String content;

    @Column(name = "commit_sha", nullable = false, length = 40)
    private String commitSha;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructor
    public CodeChunk(Long repositoryId, String filePath, int startLine, int endLine, String content, String commitSha) {
        if (startLine < 1 || endLine < startLine) {
            throw new IllegalArgumentException("Invalid line range: " + startLine + " to " + endLine);
        }

        this.repositoryId = repositoryId;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.commitSha = commitSha;
        this.createdAt = Instant.now();
    }

}
