package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCategoryRepositoryTest {

    @Mock
    private JpaCategoryRepositoryPort jpaPort;

    private JpaCategoryRepository repository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new JpaCategoryRepository(jpaPort);
    }

    private Category buildCategory() {
        Instant now = Instant.now();
        return new Category(CATEGORY_ID, USER_ID, "Comida", "#FF0000", "food-icon", false, 0, now, now);
    }

    @Test
    @DisplayName("findAllByUser maps every returned entity to a domain Category")
    void findAllByUserMapsEntities() {
        CategoryEntity entity = CategoryEntity.fromDomain(buildCategory());
        when(jpaPort.findByUserIdOrderByPositionAscNameAsc(USER_ID)).thenReturn(List.of(entity));

        List<Category> result = repository.findAllByUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("findByIdAndUser maps a present entity")
    void findByIdAndUserMapsPresentEntity() {
        CategoryEntity entity = CategoryEntity.fromDomain(buildCategory());
        when(jpaPort.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.of(entity));

        Optional<Category> result = repository.findByIdAndUser(CATEGORY_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("findByIdAndUser returns empty when the port finds nothing")
    void findByIdAndUserReturnsEmptyWhenAbsent() {
        when(jpaPort.findByIdAndUserId(CATEGORY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndUser(CATEGORY_ID, USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("existsByUserAndName delegates directly to the port")
    void existsByUserAndNameDelegates() {
        when(jpaPort.existsByUserIdAndName(USER_ID, "Comida")).thenReturn(true);

        assertThat(repository.existsByUserAndName(USER_ID, "Comida")).isTrue();
    }

    @Test
    @DisplayName("countExpensesByCategory delegates directly to the port")
    void countExpensesByCategoryDelegates() {
        when(jpaPort.countExpensesByCategory(CATEGORY_ID)).thenReturn(9L);

        assertThat(repository.countExpensesByCategory(CATEGORY_ID)).isEqualTo(9L);
    }

    @Test
    @DisplayName("countCategoriesByUser delegates to countByUserId")
    void countCategoriesByUserDelegates() {
        when(jpaPort.countByUserId(USER_ID)).thenReturn(4L);

        assertThat(repository.countCategoriesByUser(USER_ID)).isEqualTo(4L);
    }

    @Test
    @DisplayName("save converts the domain object to an entity and back")
    void saveConvertsToEntityAndBack() {
        Category category = buildCategory();
        CategoryEntity savedEntity = CategoryEntity.fromDomain(category);
        when(jpaPort.save(any(CategoryEntity.class))).thenReturn(savedEntity);

        Category result = repository.save(category);

        assertThat(result.getId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("delete forwards to deleteById")
    void deleteForwardsToDeleteById() {
        repository.delete(CATEGORY_ID);

        verify(jpaPort).deleteById(CATEGORY_ID);
    }

    @Test
    @DisplayName("reassignExpenses forwards all three ids to the port")
    void reassignExpensesForwardsIds() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        repository.reassignExpenses(fromId, toId, USER_ID);

        verify(jpaPort).reassignExpenses(fromId, toId, USER_ID);
    }

    @Test
    @DisplayName("clearDefaultForUser forwards to the port")
    void clearDefaultForUserForwards() {
        repository.clearDefaultForUser(USER_ID);

        verify(jpaPort).clearDefaultForUser(USER_ID);
    }
}
