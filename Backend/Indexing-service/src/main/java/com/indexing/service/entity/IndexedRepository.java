package com.indexing.service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Data
@Entity
@Table(name = "repositories")
public class IndexedRepository {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_Owner", nullable = false, length = 100)
    private String githubOwner;

    @Column(name = "repo_Name", nullable = false, length = 100)
    private String repoName;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndexingStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructor
    public IndexedRepository(String githubOwner, String repoName) {
        this.githubOwner = githubOwner;
        this.repoName = repoName;
        this.status = IndexingStatus.PENDING;
        this.createdAt = Instant.now();
    }
}
