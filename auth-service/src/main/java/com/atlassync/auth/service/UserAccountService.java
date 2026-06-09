package com.atlassync.auth.service;

import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.DuplicateResourceException;
import com.atlassync.auth.exception.InvalidTokenException;
import com.atlassync.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile-edit operations the user can run on their own account from the
 * Personal Details screen: change username, change password. Kept separate
 * from {@link AuthService} (which mints session tokens) and {@link
 * EmailVerificationService} / {@link PhoneLinkService} (which own their own
 * OTP lifecycles).
 */
@Slf4j
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User load(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("User not found"));
    }

    @Transactional
    public User updateUsername(Long userId, String newUsername) {
        User user = load(userId);
        if (newUsername.equals(user.getUsername())) {
            return user;
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new DuplicateResourceException("Username already taken");
        }
        user.setUsername(newUsername);
        log.info("[me] username updated userId={}", userId);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = load(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("[me] password change rejected — bad current password userId={}", userId);
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadCredentialsException("New password must differ from the current one");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("[me] password changed userId={}", userId);
    }
}
