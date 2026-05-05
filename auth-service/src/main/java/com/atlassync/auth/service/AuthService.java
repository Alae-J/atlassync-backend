package com.atlassync.auth.service;

import com.atlassync.auth.dto.AuthResponse;
import com.atlassync.auth.dto.LoginRequest;
import com.atlassync.auth.dto.RefreshRequest;
import com.atlassync.auth.dto.RegisterRequest;
import com.atlassync.auth.entity.RefreshToken;
import com.atlassync.auth.entity.RevocationReason;
import com.atlassync.auth.entity.User;
import com.atlassync.auth.exception.DuplicateResourceException;
import com.atlassync.auth.exception.InvalidTokenException;
import com.atlassync.auth.exception.TokenReuseException;
import com.atlassync.auth.repository.RefreshTokenRepository;
import com.atlassync.auth.repository.RoleRepository;
import com.atlassync.auth.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }

        var customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_CUSTOMER not found"));

        var user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(customerRole));
        user = userRepository.save(user);

        return createTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return createTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = JwtService.hashToken(request.refreshToken());

        var storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // Reuse detection: if the token was already revoked, the family is compromised
        if (storedToken.isRevoked()) {
            refreshTokenRepository.revokeByFamilyId(storedToken.getFamilyId(), RevocationReason.REUSE_DETECTED);
            throw new TokenReuseException("Refresh token reuse detected -- entire family revoked");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        // Rotate: mark old token as ROTATED
        storedToken.setRevoked(true);
        storedToken.setRevocationReason(RevocationReason.ROTATED);
        refreshTokenRepository.save(storedToken);

        // Create new token in the same family
        var user = storedToken.getUser();
        String rawRefreshToken = jwtService.generateRefreshToken();
        persistRefreshToken(rawRefreshToken, storedToken.getFamilyId(), user);

        String accessToken = jwtService.generateAccessToken(user);
        String roleName = user.getRoles().iterator().next().getName();

        return new AuthResponse(accessToken, rawRefreshToken, user.getId(), user.getEmail(), roleName);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = JwtService.hashToken(rawRefreshToken);

        var storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        refreshTokenRepository.revokeByFamilyId(storedToken.getFamilyId(), RevocationReason.LOGOUT);
    }

    /**
     * Mints a fresh access + refresh token pair for the given user. Used by both
     * password login/register and the phone-OTP verify flow.
     */
    @Transactional
    public AuthResponse issueTokensFor(User user) {
        return createTokenPair(user);
    }

    private AuthResponse createTokenPair(User user) {
        UUID familyId = UUID.randomUUID();
        String rawRefreshToken = jwtService.generateRefreshToken();
        persistRefreshToken(rawRefreshToken, familyId, user);

        String accessToken = jwtService.generateAccessToken(user);
        String roleName = user.getRoles().iterator().next().getName();

        return new AuthResponse(accessToken, rawRefreshToken, user.getId(), user.getEmail(), roleName);
    }

    private void persistRefreshToken(String rawToken, UUID familyId, User user) {
        var rt = new RefreshToken();
        rt.setTokenHash(JwtService.hashToken(rawToken));
        rt.setFamilyId(familyId);
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshTokenRepository.save(rt);
    }
}
