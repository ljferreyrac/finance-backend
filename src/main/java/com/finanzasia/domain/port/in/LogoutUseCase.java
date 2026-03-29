package com.finanzasia.domain.port.in;

public interface LogoutUseCase {

    /**
     * Invalidates the refresh token associated with the current session.
     * If the token is already invalid or expired this call is silently ignored.
     *
     * @param refreshToken the refresh JWT to revoke
     */
    void logout(String refreshToken);
}
