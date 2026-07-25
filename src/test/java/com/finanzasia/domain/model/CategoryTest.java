package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Category buildCategory() {
        Instant now = Instant.now();
        return new Category(ID, USER_ID, "Comida", "#FF0000", "food-icon", false, 0, now, now);
    }

    @Test
    @DisplayName("getters return the values passed to the constructor")
    void gettersReturnConstructorValues() {
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();

        Category category = new Category(ID, USER_ID, "Transporte", "#00FF00", "car-icon",
                true, 3, createdAt, updatedAt);

        assertThat(category.getId()).isEqualTo(ID);
        assertThat(category.getUserId()).isEqualTo(USER_ID);
        assertThat(category.getName()).isEqualTo("Transporte");
        assertThat(category.getColor()).isEqualTo("#00FF00");
        assertThat(category.getIcon()).isEqualTo("car-icon");
        assertThat(category.isDefault()).isTrue();
        assertThat(category.getPosition()).isEqualTo(3);
        assertThat(category.getCreatedAt()).isEqualTo(createdAt);
        assertThat(category.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Nested
    @DisplayName("belongsTo")
    class BelongsTo {

        @Test
        @DisplayName("true when the owner id matches")
        void trueWhenOwnerMatches() {
            assertThat(buildCategory().belongsTo(USER_ID)).isTrue();
        }

        @Test
        @DisplayName("false when the owner id differs")
        void falseWhenOwnerDiffers() {
            assertThat(buildCategory().belongsTo(UUID.randomUUID())).isFalse();
        }
    }

    @Test
    @DisplayName("markAsDefault sets isDefault true and bumps updatedAt")
    void markAsDefaultSetsFlagAndTimestamp() {
        Category category = buildCategory();
        Instant now = Instant.now();

        category.markAsDefault(now);

        assertThat(category.isDefault()).isTrue();
        assertThat(category.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("clearDefault sets isDefault false and bumps updatedAt")
    void clearDefaultClearsFlagAndBumpsTimestamp() {
        Category category = buildCategory();
        Instant now = Instant.now();
        category.markAsDefault(now);

        Instant later = now.plusSeconds(5);
        category.clearDefault(later);

        assertThat(category.isDefault()).isFalse();
        assertThat(category.getUpdatedAt()).isEqualTo(later);
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("a non-null name replaces the existing one")
        void nonNullNameReplacesExisting() {
            Category category = buildCategory();
            Instant now = Instant.now();

            category.update("Comida y bebida", "#123456", "new-icon", 5, now);

            assertThat(category.getName()).isEqualTo("Comida y bebida");
            assertThat(category.getColor()).isEqualTo("#123456");
            assertThat(category.getIcon()).isEqualTo("new-icon");
            assertThat(category.getPosition()).isEqualTo(5);
            assertThat(category.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("a null name and a null position leave the existing values in place")
        void nullNameAndPositionKeepExisting() {
            Category category = buildCategory();
            Instant now = Instant.now();

            category.update(null, "#123456", "new-icon", null, now);

            assertThat(category.getName()).isEqualTo("Comida");
            assertThat(category.getPosition()).isEqualTo(0);
            assertThat(category.getColor()).isEqualTo("#123456");
            assertThat(category.getIcon()).isEqualTo("new-icon");
            assertThat(category.getUpdatedAt()).isEqualTo(now);
        }
    }
}
