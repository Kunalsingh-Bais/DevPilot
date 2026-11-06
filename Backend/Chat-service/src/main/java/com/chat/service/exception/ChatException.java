package com.chat.service.exception;

import org.springframework.http.HttpStatus;

// An error with the HTTP status we want to return to the caller.
public class ChatException extends RuntimeException {

    private final HttpStatus status;

    public ChatException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
