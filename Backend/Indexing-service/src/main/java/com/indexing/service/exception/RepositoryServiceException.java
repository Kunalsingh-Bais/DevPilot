package com.indexing.service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// An error that happened while calling repository-service.
public class RepositoryServiceException extends RuntimeException {

    @Getter
    private final HttpStatus status;

    public RepositoryServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
