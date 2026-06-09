package com.atlassync.auth.lists;

import java.time.Instant;
import java.util.List;

/**
 * Full list payload returned by {@code GET /api/lists/{id}}, {@code POST /api/lists},
 * and {@code PATCH /api/lists/{id}}.
 */
public record ListDetail(
        Long id,
        String name,
        List<ListItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {}
