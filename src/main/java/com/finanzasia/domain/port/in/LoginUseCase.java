package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.AuthTokens;

public interface LoginUseCase {

    /**
     * Authenticates a user by email and password.
     *
     * @param email       the user's email address
     * @param rawPassword the plain-text password to verify
     * @return a token pair (access + refresh) on successful authentication
     */
    AuthTokens login(String email, String rawPassword);
}
