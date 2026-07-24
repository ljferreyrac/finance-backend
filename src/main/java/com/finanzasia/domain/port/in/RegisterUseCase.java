package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.User;

public interface RegisterUseCase {

    /**
     * @param email       must be unique
     * @param rawPassword plain-text password, hashed before storage
     * @param currency    preferred currency: "PEN" or "USD"
     * @param timezone    IANA timezone string, e.g. "America/Lima"
     * @return the persisted domain {@link User}; password hash is present but must not be exposed
     */
    User register(String email, String rawPassword, String fullName, String currency, String timezone);
}
