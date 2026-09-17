package com.cpclub.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The request is valid, but the current state of the resource forbids it.
 *
 * <p>Distinct from {@link BadRequestException}: nothing is wrong with what was
 * sent. Deleting an event that still has attendance is a well-formed request
 * the server declines until the caller confirms it means it. Answered as 409 so
 * a client can tell "fix your input" from "confirm, or do something else".</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
