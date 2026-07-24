package com.finanzasia.domain.port.in;

public interface LogoutUseCase {

    /** If the token is already invalid or expired, this call is silently ignored. */
    void logout(String refreshToken);
}
