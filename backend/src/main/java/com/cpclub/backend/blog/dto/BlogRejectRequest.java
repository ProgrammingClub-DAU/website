package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Why a post was sent back. Required: a rejection the author cannot act on is useless. */
public record BlogRejectRequest(
        @NotBlank(message = "A note for the author is required")
        @Size(max = 500, message = "The note must be at most 500 characters")
        String note
) {
}
