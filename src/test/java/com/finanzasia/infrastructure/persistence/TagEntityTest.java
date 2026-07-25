package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TagEntityTest {

    @Test
    @DisplayName("toTag maps every field")
    void toTagMapsAllFields() {
        Tag tag = new Tag(UUID.randomUUID(), UUID.randomUUID(), "viaje", "#FFF");
        TagEntity entity = TagEntity.fromTag(tag);

        Tag roundTripped = entity.toTag();

        assertThat(roundTripped).isEqualTo(tag);
    }

    @Test
    @DisplayName("every getter reflects the value passed to its setter")
    void gettersReflectSetterValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TagEntity entity = new TagEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setName("viaje");
        entity.setColor("#FFF");

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getName()).isEqualTo("viaje");
        assertThat(entity.getColor()).isEqualTo("#FFF");
    }

    @Nested
    @DisplayName("fromTag")
    class FromTag {

        @Test
        @DisplayName("copies the id when the tag already has one")
        void copiesExistingId() {
            UUID id = UUID.randomUUID();
            Tag tag = new Tag(id, UUID.randomUUID(), "viaje", "#FFF");

            TagEntity entity = TagEntity.fromTag(tag);

            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("leaves the id null for a not-yet-persisted tag, so @GeneratedValue assigns one")
        void leavesIdNullWhenAbsent() {
            Tag tag = new Tag(null, UUID.randomUUID(), "viaje", "#FFF");

            TagEntity entity = TagEntity.fromTag(tag);

            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("onCreate stamps both createdAt and updatedAt")
        void onCreateStampsBothTimestamps() throws Exception {
            TagEntity entity = new TagEntity();

            invokeLifecycleMethod(entity, "onCreate");

            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isEqualTo(entity.getCreatedAt());
        }

        @Test
        @DisplayName("onUpdate only bumps updatedAt, leaving createdAt untouched")
        void onUpdateOnlyBumpsUpdatedAt() throws Exception {
            TagEntity entity = new TagEntity();
            Instant originalCreatedAt = Instant.now().minusSeconds(3600);
            entity.setCreatedAt(originalCreatedAt);
            entity.setUpdatedAt(originalCreatedAt);

            invokeLifecycleMethod(entity, "onUpdate");

            assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(entity.getUpdatedAt()).isAfter(originalCreatedAt);
        }

        private void invokeLifecycleMethod(TagEntity entity, String methodName) throws Exception {
            var method = TagEntity.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(entity);
        }
    }
}
