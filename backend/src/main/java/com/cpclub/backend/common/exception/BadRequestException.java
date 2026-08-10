package com.cpclub.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Signals a syntactically valid request that violates a business rule.
 * The global handler converts it to a consistent HTTP 400 response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    /**
     * Creates a business-rule validation exception.
     *
     * @param message explanation safe to return to the API caller
     */
    public BadRequestException(String message) {
        super(message);
    }
}
