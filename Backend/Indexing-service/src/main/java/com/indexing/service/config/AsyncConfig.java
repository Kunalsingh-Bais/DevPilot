package com.indexing.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync turns on support for @Async methods.
// Methods marked @Async run in a background thread, so the HTTP request returns quickly.
@Configuration
@EnableAsync
public class AsyncConfig {
}
