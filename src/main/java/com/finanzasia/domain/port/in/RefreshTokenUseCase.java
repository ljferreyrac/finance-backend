package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.AuthTokens;

public interface RefreshTokenUseCase {

    /** The supplied refresh token is invalidated immediately after validation (rotation). */
    AuthTokens refresh(String refreshToken);
}
