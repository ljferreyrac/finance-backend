package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.InvalidCredentialsException;
import com.finanzasia.domain.exceptions.InvalidTokenException;
import com.finanzasia.domain.exceptions.UserAlreadyExistsException;
import com.finanzasia.domain.model.AuthTokens;
import com.finanzasia.domain.model.User;
import com.finanzasia.domain.port.in.LoginUseCase;
import com.finanzasia.domain.port.in.LogoutUseCase;
import com.finanzasia.domain.port.in.RefreshTokenUseCase;
import com.finanzasia.domain.port.in.RegisterUseCase;
import com.finanzasia.domain.port.out.TokenStore;
import com.finanzasia.domain.port.out.UserRepository;
import com.finanzasia.infrastructure.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Orchestrates all authentication use cases:
 * registration, login, token refresh, and logout.
 *
 * <p>Business invariants enforced here:
 * <ul>
 *   <li>Email uniqueness is checked before hashing to avoid wasted BCrypt cycles.</li>
 *   <li>Soft-deleted accounts are treated as non-existent during login.</li>
 *   <li>Refresh tokens are rotated on every use (old token invalidated, new token issued).</li>
 * </ul>
 */
@Service
public class AuthService implements RegisterUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            TokenStore tokenStore,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokenPair(user);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        String jti = jwtService.extractJti(refreshToken);

        UUID userId = tokenStore.getUserIdForRefreshToken(jti)
                .orElseThrow(() -> new InvalidTokenException("token has been revoked or does not exist"));

        tokenStore.invalidateRefreshToken(jti);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("user no longer exists"));

        return issueTokenPair(user);
    }

    @Override
    public void logout(String refreshToken) {
        try {
            String jti = jwtService.extractJti(refreshToken);
            tokenStore.invalidateRefreshToken(jti);
        } catch (InvalidTokenException ignored) {
            // Silently ignore: if the token is already invalid there is nothing to revoke.
        }
    }

    // --- private helpers ---

    private AuthTokens issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        String jti = jwtService.extractJti(refreshToken);
        tokenStore.storeRefreshToken(jti, user.getId(), jwtService.refreshTokenDuration());

        return new AuthTokens(accessToken, refreshToken, jwtService.accessExpiresInSeconds());
    }
}
