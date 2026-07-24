package com.finanzasia.domain.model;

import java.util.UUID;

/**
 * No JPA or framework annotations allowed here.
 */
public record Tag(UUID id, UUID userId, String name, String color) {}
