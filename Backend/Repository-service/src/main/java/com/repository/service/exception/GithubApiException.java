package com.repository.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
// An error caused by GitHub.
public class GithubApiException extends RuntimeException{

    private final HttpStatus status;

    public GithubApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
