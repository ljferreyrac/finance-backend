package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryEntityTest {

    @Test
    @DisplayName("fromDomain followed by toDomain round-trips every field")
    void roundTripsAllFields() {
        Instant now = Instant.now();
        Category category = new Category(UUID.randomUUID(), UUID.randomUUID(), "Comida",
                "#FF0000", "food-icon", true, 3, now, now);

        CategoryEntity entity = CategoryEntity.fromDomain(category);
        Category roundTripped = entity.toDomain();

        assertThat(roundTripped.getId()).isEqualTo(category.getId());
        assertThat(roundTripped.getUserId()).isEqualTo(category.getUserId());
        assertThat(roundTripped.getName()).isEqualTo(category.getName());
        assertThat(roundTripped.getColor()).isEqualTo(category.getColor());
        assertThat(roundTripped.getIcon()).isEqualTo(category.getIcon());
        assertThat(roundTripped.isDefault()).isEqualTo(category.isDefault());
        assertThat(roundTripped.getPosition()).isEqualTo(category.getPosition());
        assertThat(roundTripped.getCreatedAt()).isEqualTo(category.getCreatedAt());
        assertThat(roundTripped.getUpdatedAt()).isEqualTo(category.getUpdatedAt());
    }

    @Test
    @DisplayName("every getter reflects the value passed to its setter")
    void gettersReflectSetterValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(60);
        Instant updatedAt = Instant.now();

        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName("Comida");
        entity.setColor("#FF0000");
        entity.setIcon("food-icon");
        entity.setDefault(true);
        entity.setPosition(3);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getName()).isEqualTo("Comida");
        assertThat(entity.getColor()).isEqualTo("#FF0000");
        assertThat(entity.getIcon()).isEqualTo("food-icon");
        assertThat(entity.isDefault()).isTrue();
        assertThat(entity.getPosition()).isEqualTo(3);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
