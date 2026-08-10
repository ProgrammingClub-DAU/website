package com.cpclub.backend.common.dto;

import java.util.List;

/**
 * Immutable pagination metadata and page content returned by collection endpoints.
 *
 * @param <T> item type in the requested page
 * @param content items for this page
 * @param page zero-based page index
 * @param size configured page size
 * @param totalElements total matching records across all pages
 * @param totalPages number of available pages
 * @param last whether this is the final available page
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
