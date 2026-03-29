package com.finanzasia.domain.model;

import java.util.UUID;

/**
 * Pure domain value representing a user-defined label that can be attached
 * to any number of transactions. No JPA or framework annotations allowed here.
 */
public record Tag(UUID id, UUID userId, String name, String color) {}
