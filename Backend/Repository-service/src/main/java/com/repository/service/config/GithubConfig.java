package com.repository.service.config;

import com.repository.service.github.GithubProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubConfig {

    // Builds one HTTP client for all GitHub calls.
    @Bean
    RestClient githubRestClient(GithubProperties props) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.apiBaseUrl())
                // Asks GitHub for JSON, and names the API version
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "devpilot-repo-service");

        if (props.token() != null && props.token().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + props.token());
        }
        return builder.build();
    }
}
