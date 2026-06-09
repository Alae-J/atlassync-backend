package com.atlassync.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Partial-update payload for {@code PATCH /api/auth/me/preferences}.
 * Every field is optional — null means "leave this preference unchanged".
 */
public record UpdatePreferencesRequest(
        Long defaultStoreId,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Use a 3-letter ISO currency code (e.g. USD, MAD, EUR)")
        String currencyCode,

        @Size(max = 32, message = "Up to 32 dietary tags")
        List<String> dietaryPrefs,

        @Size(max = 32, message = "Up to 32 allergens")
        List<String> allergens,

        Map<String, Boolean> notificationPrefs
) {}
