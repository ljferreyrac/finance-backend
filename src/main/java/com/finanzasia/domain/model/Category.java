package com.finanzasia.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain entity representing a user-defined expense category.
 * No JPA or framework annotations are allowed here.
 */
public final class Category {

    private final UUID id;
    private final UUID userId;
    private String name;
    private String color;
    private String icon;
    private boolean isDefault;
    private int position;
    private final Instant createdAt;
    private Instant updatedAt;

    public Category(
            UUID id,
            UUID userId,
            String name,
            String color,
            String icon,
            boolean isDefault,
            int position,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.isDefault = isDefault;
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public boolean isDefault() { return isDefault; }
    public int getPosition() { return position; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Returns true when this category is owned by the given user. */
    public boolean belongsTo(UUID ownerId) {
        return userId.equals(ownerId);
    }

    /** Sets this category as the default, updating the timestamp. */
    public void markAsDefault(Instant now) {
        this.isDefault = true;
        this.updatedAt = now;
    }

    public void clearDefault(Instant now) {
        this.isDefault = false;
        this.updatedAt = now;
    }

    public void update(String name, String color, String icon, Integer position, Instant now) {
        if (name != null) {
            this.name = name;
        }
        this.color = color;
        this.icon = icon;
        if (position != null) {
            this.position = position;
        }
        this.updatedAt = now;
    }
}
