package com.finanzasia.domain.model;

import java.util.UUID;

/**
 * Identity carried by a valid access token.
 *
 * <p>Deliberately not the full {@link User}: the security filter only needs who
 * is calling, and loading the rest would mean a database hit on every request.
 */
public record AuthenticatedUser(UUID id, String email) {
}
