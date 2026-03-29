package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.AuthTokens;

public interface RefreshTokenUseCase {

    /**
     * Rotates the refresh token and issues a fresh token pair.
     * The supplied refresh token is invalidated immediately after validation.
     *
     * @param refreshToken the current refresh JWT
     * @return a new token pair (access + refresh)
     */
    AuthTokens refresh(String refreshToken);
}
