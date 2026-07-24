package com.finanzasia.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * No JPA or framework annotations are allowed here.
 * The password hash is carried in this object only for the authentication
 * flow; it must never be serialized to API responses.
 */
public final class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final String currency;
    private final String timezone;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    public User(
            UUID id,
            String email,
            String passwordHash,
            String fullName,
            String currency,
            String timezone,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.currency = currency;
        this.timezone = timezone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getCurrency() { return currency; }
    public String getTimezone() { return timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    /** True once the user has been soft-deleted, never hard-removed. */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
