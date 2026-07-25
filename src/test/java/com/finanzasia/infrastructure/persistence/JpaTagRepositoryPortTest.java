package com.finanzasia.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JpaTagRepositoryPortTest extends AbstractPostgresTest {

    @Autowired
    private JpaTagRepositoryPort tagPort;

    @Autowired
    private TestEntityManager em;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = PersistenceFixtures.user(em).getId();
    }

    @Test
    @DisplayName("findByUserId returns only this user's tags")
    void findByUserIdScopesToOwner() {
        PersistenceFixtures.tag(em, userId, "reimbursable");
        UUID otherUserId = PersistenceFixtures.user(em).getId();
        PersistenceFixtures.tag(em, otherUserId, "reimbursable");

        List<TagEntity> rows = tagPort.findByUserId(userId);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("findByIdAndUserId does not return another user's tag")
    void findByIdAndUserIdScopesToOwner() {
        TagEntity tag = PersistenceFixtures.tag(em, userId, "reimbursable");
        UUID otherUserId = PersistenceFixtures.user(em).getId();

        Optional<TagEntity> found = tagPort.findByIdAndUserId(tag.getId(), otherUserId);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByIdInAndUserId returns the intersection, excluding ids owned by another user")
    void findByIdInAndUserIdExcludesOthers() {
        TagEntity mine = PersistenceFixtures.tag(em, userId, "reimbursable");
        UUID otherUserId = PersistenceFixtures.user(em).getId();
        TagEntity others = PersistenceFixtures.tag(em, otherUserId, "personal");

        List<TagEntity> rows = tagPort.findByIdInAndUserId(Set.of(mine.getId(), others.getId()), userId);

        assertThat(rows).extracting(TagEntity::getId).containsExactly(mine.getId());
    }

    @Test
    @DisplayName("existsByUserIdAndName is user-scoped")
    void existsByUserIdAndName() {
        PersistenceFixtures.tag(em, userId, "reimbursable");
        UUID otherUserId = PersistenceFixtures.user(em).getId();

        assertThat(tagPort.existsByUserIdAndName(userId, "reimbursable")).isTrue();
        assertThat(tagPort.existsByUserIdAndName(otherUserId, "reimbursable")).isFalse();
    }

    @Nested
    @DisplayName("deleteByIdAndUserId")
    class DeleteByIdAndUserId {

        @Test
        @DisplayName("deletes and returns 1 when the tag belongs to the user")
        void deletesOwnedTag() {
            TagEntity tag = PersistenceFixtures.tag(em, userId, "reimbursable");

            long deleted = tagPort.deleteByIdAndUserId(tag.getId(), userId);
            em.flush();
            em.clear();

            assertThat(deleted).isEqualTo(1);
            assertThat(em.find(TagEntity.class, tag.getId())).isNull();
        }

        @Test
        @DisplayName("deletes nothing and returns 0 for another user's tag")
        void doesNotDeleteAnotherUsersTag() {
            TagEntity tag = PersistenceFixtures.tag(em, userId, "reimbursable");
            UUID otherUserId = PersistenceFixtures.user(em).getId();

            long deleted = tagPort.deleteByIdAndUserId(tag.getId(), otherUserId);
            em.flush();
            em.clear();

            assertThat(deleted).isZero();
            assertThat(em.find(TagEntity.class, tag.getId())).isNotNull();
        }
    }
}
