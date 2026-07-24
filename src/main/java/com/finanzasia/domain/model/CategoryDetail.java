package com.finanzasia.domain.model;

/**
 * A category together with how many expenses reference it.
 *
 * <p>Exists so the count is resolved by the service that owns the repository,
 * rather than by the web layer decorating a {@link Category} after the fact.
 * The client uses it to decide whether deleting the category needs a
 * reassignment target.
 */
public record CategoryDetail(Category category, long expenseCount) {
}
