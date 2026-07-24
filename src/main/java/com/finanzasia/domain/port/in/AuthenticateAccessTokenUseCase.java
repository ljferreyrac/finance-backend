package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.AuthenticatedUser;

import java.util.Optional;

/**
 * Input port: resolves the caller behind an access token.
 *
 * <p>Exists so the HTTP security filter can authenticate a request by calling a
 * service, the same way a controller does, instead of reaching for the token
 * library itself.
 */
public interface AuthenticateAccessTokenUseCase {

    /**
     * @return the caller, or empty when the token is missing, expired, malformed
     *         or otherwise unusable. Rejection is not an error here: Spring
     *         Security's standard 401 path handles unauthenticated requests.
     */
    Optional<AuthenticatedUser> authenticate(String accessToken);
}
