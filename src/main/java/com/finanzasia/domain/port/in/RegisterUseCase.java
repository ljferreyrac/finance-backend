package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.User;

public interface RegisterUseCase {

    /**
     * Registers a new user account.
     *
     * @param email       the user's unique email address
     * @param rawPassword the plain-text password (will be hashed before storage)
     * @param fullName    the user's display name
     * @param currency    preferred currency: "PEN" or "USD"
     * @param timezone    IANA timezone string, e.g. "America/Lima"
     * @return the persisted domain {@link User} (password hash is present but must not be exposed)
     */
    User register(String email, String rawPassword, String fullName, String currency, String timezone);
}
