package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import com.finanzasia.domain.model.AuthTokens;
import com.finanzasia.domain.model.AuthenticatedUser;
import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.LoginUseCase;
import com.finanzasia.domain.port.in.LogoutUseCase;
import com.finanzasia.domain.port.in.RefreshTokenUseCase;
import com.finanzasia.domain.port.in.RegisterUseCase;
import com.finanzasia.domain.port.out.TokenProvider;
import com.finanzasia.domain.port.out.TokenStore;
import com.finanzasia.domain.port.out.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Registration, login, refresh, and logout. Email uniqueness is checked before hashing to
 * avoid wasted BCrypt cycles; soft-deleted accounts are treated as non-existent at login;
 * refresh tokens rotate on every use.
 */
@Service
public class AuthService
        implements RegisterUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase,
        AuthenticateAccessTokenUseCase {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    /** Compared against when the email is unknown, so login timing does not leak whether an account exists. */
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            TokenStore tokenStore,
            TokenProvider tokenProvider,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(
                "timing-equalizer-" + UUID.randomUUID());
    }

    @Override
    public User register(
            String email,
            String rawPassword,
            String fullName,
            String currency,
            String timezone) {

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        String hash = passwordEncoder.encode(rawPassword);
        Instant now = Instant.now();

        User newUser = new User(
                UUID.randomUUID(),
                email,
                hash,
                fullName,
                currency != null ? currency : "PEN",
                timezone != null ? timezone : "America/Lima",
                now,
                now,
                null);

        return userRepository.save(newUser);
    }

    @Override
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);

        // Always run exactly one BCrypt comparison so "unknown email" and "wrong password" take the same time.
        String hashToCheck = user != null ? user.getPasswordHash() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hashToCheck);

        if (user == null || user.isDeleted() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return issueTokenPair(user);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        String jti = tokenProvider.extractJti(refreshToken);

        UUID userId = tokenStore.getUserIdForRefreshToken(jti)
                .orElseThrow(() -> {
                    // Valid signature but unknown jti means the token was already rotated/revoked,
                    // possibly reused after theft. Treat as compromised and revoke all sessions.
                    UUID suspectUserId = tokenProvider.extractUserId(refreshToken);
                    tokenStore.revokeAllForUser(suspectUserId);
                    return new InvalidTokenException("token has been revoked or does not exist");
                });

        tokenStore.invalidateRefreshToken(jti);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("user no longer exists"));

        return issueTokenPair(user);
    }

    @Override
    public void logout(String refreshToken) {
        try {
            String jti = tokenProvider.extractJti(refreshToken);
            tokenStore.invalidateRefreshToken(jti);
        } catch (InvalidTokenException ignored) {
            // Silently ignore: if the token is already invalid there is nothing to revoke.
        }
    }

    @Override
    public Optional<AuthenticatedUser> authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(tokenProvider.parseAccessToken(accessToken));
        } catch (InvalidTokenException | IllegalArgumentException ex) {
            // An unusable token is an unauthenticated request, not a failure:
            // Spring Security turns the empty context into a 401.
            return Optional.empty();
        }
    }

    private AuthTokens issueTokenPair(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        String jti = tokenProvider.extractJti(refreshToken);
        tokenStore.storeRefreshToken(jti, user.getId(), tokenProvider.refreshTokenDuration());

        return new AuthTokens(accessToken, refreshToken, tokenProvider.accessExpiresInSeconds());
    }
}
