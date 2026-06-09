package com.atlassync.auth.service;

import com.atlassync.auth.dto.UpdatePreferencesRequest;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.InvalidTokenException;
import com.atlassync.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Patch-style updates to the user's preference columns. Each field is
 * applied only when present on the request — nulls leave the existing
 * value alone, empty arrays/maps explicitly clear.
 */
@Slf4j
@Service
public class UserPreferencesService {

    private final UserRepository userRepository;

    public UserPreferencesService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User update(Long userId, UpdatePreferencesRequest patch) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        if (patch.defaultStoreId() != null) {
            user.setDefaultStoreId(patch.defaultStoreId());
        }
        if (patch.currencyCode() != null) {
            user.setCurrencyCode(patch.currencyCode().toUpperCase());
        }
        if (patch.dietaryPrefs() != null) {
            // De-dupe while preserving the order the client sent us.
            user.setDietaryPrefs(new ArrayList<>(new LinkedHashSet<>(patch.dietaryPrefs())));
        }
        if (patch.allergens() != null) {
            user.setAllergens(new ArrayList<>(new LinkedHashSet<>(patch.allergens())));
        }
        if (patch.notificationPrefs() != null) {
            user.setNotificationPrefs(new HashMap<>(patch.notificationPrefs()));
        }

        log.info("[me] preferences updated userId={}", userId);
        return userRepository.save(user);
    }
}
