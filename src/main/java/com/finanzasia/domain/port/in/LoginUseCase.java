package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.AuthTokens;

public interface LoginUseCase {

    /** @param rawPassword the plain-text password to verify against the stored hash */
    AuthTokens login(String email, String rawPassword);
}
