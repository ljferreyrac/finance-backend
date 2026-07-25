package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTagRepositoryTest {

    @Mock
    private JpaTagRepositoryPort jpaPort;

    private JpaTagRepository repository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TAG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new JpaTagRepository(jpaPort);
    }

    private Tag buildTag() {
        return new Tag(TAG_ID, USER_ID, "viaje", "#FFF");
    }

    @Test
    @DisplayName("findByUserId maps every returned entity to a domain Tag")
    void findByUserIdMapsEntities() {
        TagEntity entity = TagEntity.fromTag(buildTag());
        when(jpaPort.findByUserId(USER_ID)).thenReturn(List.of(entity));

        List<Tag> result = repository.findByUserId(USER_ID);

        assertThat(result).containsExactly(buildTag());
    }

    @Test
    @DisplayName("findByIdAndUserId maps a present entity")
    void findByIdAndUserIdMapsPresentEntity() {
        TagEntity entity = TagEntity.fromTag(buildTag());
        when(jpaPort.findByIdAndUserId(TAG_ID, USER_ID)).thenReturn(Optional.of(entity));

        Optional<Tag> result = repository.findByIdAndUserId(TAG_ID, USER_ID);

        assertThat(result).contains(buildTag());
    }

    @Nested
    @DisplayName("findByIdsAndUserId")
    class FindByIdsAndUserId {

        @Test
        @DisplayName("a null id set short-circuits to an empty list without querying the port")
        void nullIdsShortCircuits() {
            List<Tag> result = repository.findByIdsAndUserId(null, USER_ID);

            assertThat(result).isEmpty();
            verify(jpaPort, never()).findByIdInAndUserId(any(), any());
        }

        @Test
        @DisplayName("an empty id set short-circuits to an empty list without querying the port")
        void emptyIdsShortCircuits() {
            List<Tag> result = repository.findByIdsAndUserId(Set.of(), USER_ID);

            assertThat(result).isEmpty();
            verify(jpaPort, never()).findByIdInAndUserId(any(), any());
        }

        @Test
        @DisplayName("a non-empty id set queries the port and maps the results")
        void nonEmptyIdsQueriesPort() {
            Set<UUID> ids = Set.of(TAG_ID);
            TagEntity entity = TagEntity.fromTag(buildTag());
            when(jpaPort.findByIdInAndUserId(ids, USER_ID)).thenReturn(List.of(entity));

            List<Tag> result = repository.findByIdsAndUserId(ids, USER_ID);

            assertThat(result).containsExactly(buildTag());
        }
    }

    @Test
    @DisplayName("existsByUserIdAndName delegates directly to the port")
    void existsByUserIdAndNameDelegates() {
        when(jpaPort.existsByUserIdAndName(USER_ID, "viaje")).thenReturn(true);

        assertThat(repository.existsByUserIdAndName(USER_ID, "viaje")).isTrue();
    }

    @Test
    @DisplayName("save converts the domain tag to an entity and back")
    void saveConvertsToEntityAndBack() {
        Tag tag = buildTag();
        TagEntity savedEntity = TagEntity.fromTag(tag);
        when(jpaPort.save(any(TagEntity.class))).thenReturn(savedEntity);

        Tag result = repository.save(tag);

        assertThat(result).isEqualTo(tag);
    }

    @Nested
    @DisplayName("deleteByIdAndUserId")
    class DeleteByIdAndUserId {

        @Test
        @DisplayName("true when a row was actually deleted")
        void trueWhenRowDeleted() {
            when(jpaPort.deleteByIdAndUserId(TAG_ID, USER_ID)).thenReturn(1L);

            assertThat(repository.deleteByIdAndUserId(TAG_ID, USER_ID)).isTrue();
        }

        @Test
        @DisplayName("false when no row matched")
        void falseWhenNoRowMatched() {
            when(jpaPort.deleteByIdAndUserId(TAG_ID, USER_ID)).thenReturn(0L);

            assertThat(repository.deleteByIdAndUserId(TAG_ID, USER_ID)).isFalse();
        }
    }
}
