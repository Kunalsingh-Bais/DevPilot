package com.indexing.service.repoClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RepositoryServiceConfig {

    // Builds the HTTP client used for every call to repository-service.
    @Bean
    RestClient repositoryServiceRestClient(@Value("${devpilot.repository-service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Connect timeout: how long to wait for repository-service to accept the connection.
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));

        // Read timeout: how long to wait for repository-service to send its answer.
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
