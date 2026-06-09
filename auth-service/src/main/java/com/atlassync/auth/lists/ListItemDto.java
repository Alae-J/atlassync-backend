package com.atlassync.auth.lists;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A single line in a shopping list: barcode + quantity.
 */
public record ListItemDto(
        @NotBlank
        @Size(max = 64)
        String barcode,

        @Min(1)
        int qty
) {}
