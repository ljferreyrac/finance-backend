package com.finanzasia.api.controller;

import com.finanzasia.api.dto.CategoryDTO;
import com.finanzasia.api.dto.CreateCategoryRequest;
import com.finanzasia.api.dto.UpdateCategoryRequest;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.port.in.CategoryUseCase;
import com.finanzasia.domain.port.out.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = "CRUD operations for expense categories")
@RestController
@RequestMapping("/api/v1/categories")
@Validated
public class CategoryController {

    private final CategoryUseCase categoryUseCase;
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryUseCase categoryUseCase,
                              CategoryRepository categoryRepository) {
        this.categoryUseCase = categoryUseCase;
        this.categoryRepository = categoryRepository;
    }

    @Operation(summary = "List categories",
               description = "Returns all categories owned by the authenticated user, ordered by position then name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category list",
                         content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping
    public List<CategoryDTO> list(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryUseCase.listCategories(principal.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Operation(summary = "Create a category",
               description = "Creates a new category for the authenticated user. "
                           + "If isDefault is true, the previous default category is cleared.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created",
                         content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "409", description = "A category with this name already exists", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDTO create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCategoryRequest request) {

        Category category = categoryUseCase.createCategory(
                principal.getId(),
                request.name(),
                request.color(),
                request.icon(),
                request.isDefault());

        return toDTO(category);
    }

    @Operation(summary = "Update a category",
               description = "Updates an existing category. All fields are optional; "
                           + "omitted fields retain their current values. "
                           + "Color and icon are nullable: passing null clears the existing value.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated",
                         content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "A category with this name already exists", content = @Content)
    })
    @PutMapping("/{id}")
    public CategoryDTO update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {

        Category category = categoryUseCase.updateCategory(
                principal.getId(),
                id,
                request.name(),
                request.color(),
                request.icon(),
                request.position());

        return toDTO(category);
    }

    @Operation(summary = "Delete a category",
               description = "Deletes a category owned by the authenticated user. "
                           + "If the category has associated expenses, a reassignTo UUID must be provided "
                           + "to migrate them before deletion. Returns 409 when expenses exist and no "
                           + "reassignment target is given, or when it is the user's only category.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Category in use or last category", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID") @PathVariable UUID id,
            @Parameter(description = "UUID of the category to migrate expenses to before deletion")
            @RequestParam(required = false) UUID reassignTo) {

        categoryUseCase.deleteCategory(principal.getId(), id, reassignTo);
    }

    @Operation(summary = "Set default category",
               description = "Marks the given category as the user's default, clearing any previous default.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default updated",
                         content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content)
    })
    @PatchMapping("/{id}/default")
    public CategoryDTO setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Category UUID") @PathVariable UUID id) {

        Category category = categoryUseCase.setDefaultCategory(principal.getId(), id);
        return toDTO(category);
    }

    // --- presentation helpers ---

    private CategoryDTO toDTO(Category category) {
        long expenseCount = categoryRepository.countExpensesByCategory(category.getId());
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.isDefault(),
                category.getPosition(),
                expenseCount);
    }
}
