package com.repository.service.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GithubProperties (
        String apiBaseUrl,
        String token
){
}
