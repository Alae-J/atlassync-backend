package com.atlassync.auth.lists;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for {@code POST /api/lists}. Items are optional — an empty list
 * is created when omitted.
 */
public record CreateListRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @Valid
        List<ListItemDto> items
) {}
