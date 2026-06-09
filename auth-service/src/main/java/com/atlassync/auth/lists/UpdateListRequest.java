package com.atlassync.auth.lists;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Partial-update payload for {@code PATCH /api/lists/{id}}. Both fields are
 * optional — null means "leave unchanged".
 */
public record UpdateListRequest(
        @Size(max = 80)
        String name,

        @Valid
        List<ListItemDto> items
) {}
