package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.DuplicateCategoryNameException;
import com.finanzasia.domain.exceptions.LastCategoryException;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.CategoryDetail;
import com.finanzasia.domain.port.out.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OTHER_CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository);
    }

    // --- helpers ---

    private Category buildCategory(UUID id, UUID userId, String name, boolean isDefault) {
        return new Category(id, userId, name, "#FF5733", "food", isDefault, 1, Instant.now(), Instant.now());
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("listCategories")
    class ListCategories {

        @Test
        @DisplayName("delegates to repository and returns the result")
        void listCategoriesReturnsUserCategories() {
            List<Category> expected = List.of(
                    buildCategory(UUID.randomUUID(), USER_ID, "Comida", true),
                    buildCategory(UUID.randomUUID(), USER_ID, "Transporte", false));
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(expected);

            List<CategoryDetail> result = service.listCategories(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CategoryDetail::category)
                    .containsExactlyElementsOf(expected);
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("saves and returns the new category when name is unique")
        void createCategorySuccess() {
            when(categoryRepository.existsByUserAndName(USER_ID, "Comida")).thenReturn(false);
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CategoryDetail result = service.createCategory(USER_ID, "Comida", "#FF5733", "food", false);

            assertThat(result.category().getId()).isNotNull();
            assertThat(result.category().getName()).isEqualTo("Comida");
            assertThat(result.category().getUserId()).isEqualTo(USER_ID);
            assertThat(result.category().isDefault()).isFalse();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("throws DuplicateCategoryNameException when a category with the same name exists")
        void createCategoryDuplicateNameThrows409() {
            when(categoryRepository.existsByUserAndName(USER_ID, "Comida")).thenReturn(true);

            assertThatThrownBy(() -> service.createCategory(USER_ID, "Comida", null, null, false))
                    .isInstanceOf(DuplicateCategoryNameException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("clears existing default before saving when isDefault is true")
        void createCategoryIsDefaultClearsExistingDefault() {
            when(categoryRepository.existsByUserAndName(USER_ID, "Comida")).thenReturn(false);
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CategoryDetail result = service.createCategory(USER_ID, "Comida", "#FF5733", "food", true);

            verify(categoryRepository).clearDefaultForUser(USER_ID);
            assertThat(result.category().isDefault()).isTrue();
        }

        @Test
        @DisplayName("does not clear defaults when isDefault is false")
        void createCategoryNotDefaultDoesNotClearDefaults() {
            when(categoryRepository.existsByUserAndName(USER_ID, "Transporte")).thenReturn(false);
            when(categoryRepository.findAllByUser(USER_ID)).thenReturn(List.of());
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createCategory(USER_ID, "Transporte", null, null, false);

            verify(categoryRepository, never()).clearDefaultForUser(any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("updates allowed fields and saves")
        void updateCategorySuccess() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CategoryDetail result = service.updateCategory(USER_ID, CATEGORY_ID, "Gastos de casa",
                    "#3498DB", "home", 3);

            assertThat(result.category().getName()).isEqualTo("Gastos de casa");
            assertThat(result.category().getPosition()).isEqualTo(3);
            verify(categoryRepository).save(existing);
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not belong to user")
        void updateCategoryNotFoundThrows404() {
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCategory(
                    USER_ID, CATEGORY_ID, "Nueva", null, null, null))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateCategoryNameException when new name conflicts with another category")
        void updateCategoryDuplicateNameThrows409() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByUserAndName(USER_ID, "Transporte")).thenReturn(true);

            assertThatThrownBy(() -> service.updateCategory(
                    USER_ID, CATEGORY_ID, "Transporte", null, null, null))
                    .isInstanceOf(DuplicateCategoryNameException.class);

            verify(categoryRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("deletes category when it has no expenses")
        void deleteCategoryNoExpensesSuccess() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countCategoriesByUser(USER_ID)).thenReturn(3L);
            when(categoryRepository.countExpensesByCategory(CATEGORY_ID)).thenReturn(0L);

            service.deleteCategory(USER_ID, CATEGORY_ID, null);

            verify(categoryRepository).delete(CATEGORY_ID);
            verify(categoryRepository, never()).reassignExpenses(any(), any(), any());
        }

        @Test
        @DisplayName("throws CategoryInUseException when category has expenses and no reassignTo")
        void deleteCategoryWithExpensesNoReassignThrows409() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countCategoriesByUser(USER_ID)).thenReturn(3L);
            when(categoryRepository.countExpensesByCategory(CATEGORY_ID)).thenReturn(5L);

            assertThatThrownBy(() -> service.deleteCategory(USER_ID, CATEGORY_ID, null))
                    .isInstanceOf(CategoryInUseException.class)
                    .satisfies(ex -> assertThat(((CategoryInUseException) ex).getExpenseCount()).isEqualTo(5L));

            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("reassigns expenses and deletes when reassignTo is provided")
        void deleteCategoryWithExpensesWithReassignSuccess() {
            Category target = buildCategory(OTHER_CATEGORY_ID, USER_ID, "Otros", false);
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countCategoriesByUser(USER_ID)).thenReturn(3L);
            when(categoryRepository.countExpensesByCategory(CATEGORY_ID)).thenReturn(5L);
            when(categoryRepository.findByIdAndUser(OTHER_CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(target));

            service.deleteCategory(USER_ID, CATEGORY_ID, OTHER_CATEGORY_ID);

            verify(categoryRepository).reassignExpenses(CATEGORY_ID, OTHER_CATEGORY_ID, USER_ID);
            verify(categoryRepository).delete(CATEGORY_ID);
        }

        @Test
        @DisplayName("throws LastCategoryException when it is the only category")
        void deleteCategoryLastCategoryThrows409() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", true);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countCategoriesByUser(USER_ID)).thenReturn(1L);

            assertThatThrownBy(() -> service.deleteCategory(USER_ID, CATEGORY_ID, null))
                    .isInstanceOf(LastCategoryException.class);

            verify(categoryRepository, never()).delete(any());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("setDefaultCategory")
    class SetDefaultCategory {

        @Test
        @DisplayName("clears old default, marks category as default, and saves")
        void setDefaultCategorySuccessClearsOldDefault() {
            Category existing = buildCategory(CATEGORY_ID, USER_ID, "Comida", false);
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CategoryDetail result = service.setDefaultCategory(USER_ID, CATEGORY_ID);

            verify(categoryRepository).clearDefaultForUser(USER_ID);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().isDefault()).isTrue();
            assertThat(result.category().isDefault()).isTrue();
        }

        @Test
        @DisplayName("throws CategoryNotFoundException when category does not exist for user")
        void setDefaultCategoryNotFoundThrows404() {
            when(categoryRepository.findByIdAndUser(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setDefaultCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).clearDefaultForUser(any());
            verify(categoryRepository, never()).save(any());
        }
    }
}
