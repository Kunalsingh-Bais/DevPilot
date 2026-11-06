package com.chat.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// Builds the HTTP client used for every call to indexing-service
@Configuration
public class IndexingServiceConfig {

    @Bean
    RestClient indexingServiceRestClient(@Value("${devpilot.indexing-service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Wait for indexing-service to accept the connection
        factory.setConnectTimeout(Duration.ofSeconds(5));

        // How long to wait for an answer.
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
