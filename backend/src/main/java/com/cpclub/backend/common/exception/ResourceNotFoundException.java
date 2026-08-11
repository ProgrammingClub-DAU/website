package com.cpclub.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals that a requested domain resource does not exist.
 * The global handler converts it to a consistent HTTP 404 response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    /**
     * Creates a not-found exception.
     *
     * @param message explanation safe to return to the API caller
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
