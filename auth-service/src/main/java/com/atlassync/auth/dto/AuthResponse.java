package com.atlassync.auth.dto;

import java.util.List;
import java.util.Map;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String email,
        String username,
        String role,
        boolean emailVerified,
        String phone,
        UserPreferences preferences
) {
    public record UserPreferences(
            Long defaultStoreId,
            String currencyCode,
            List<String> dietaryPrefs,
            List<String> allergens,
            Map<String, Boolean> notificationPrefs
    ) {}
}
