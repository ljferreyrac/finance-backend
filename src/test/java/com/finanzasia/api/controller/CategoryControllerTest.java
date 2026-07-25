package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.CategoryInUseException;
import com.finanzasia.domain.exceptions.CategoryNotFoundException;
import com.finanzasia.domain.exceptions.DuplicateCategoryNameException;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.CategoryDetail;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.CategoryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryUseCase categoryUseCase;

    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");
    }

    private CategoryDetail buildDetail(long expenseCount) {
        Instant now = Instant.now();
        Category category = new Category(CATEGORY_ID, USER_ID, "Comida", "#FF0000", "food", false, 0, now, now);
        return new CategoryDetail(category, expenseCount);
    }

    @Test
    @DisplayName("GET /api/v1/categories returns the caller's categories with expense counts")
    void listReturnsOwnedCategories() throws Exception {
        when(categoryUseCase.listCategories(USER_ID)).thenReturn(List.of(buildDetail(5)));

        mockMvc.perform(get("/api/v1/categories").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Comida"))
                .andExpect(jsonPath("$[0].expenseCount").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/categories without authentication is rejected with 401")
    void listWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/categories creates a category and returns 201")
    void createReturns201() throws Exception {
        when(categoryUseCase.createCategory(eq(USER_ID), eq("Comida"), eq("#FF0000"), eq("food"), eq(false)))
                .thenReturn(buildDetail(0));

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Comida\",\"color\":\"#FF0000\",\"icon\":\"food\",\"isDefault\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/categories with a blank name fails bean validation with 400")
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"isDefault\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/categories for a duplicate name surfaces as 409")
    void createDuplicateNameReturns409() throws Exception {
        when(categoryUseCase.createCategory(any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new DuplicateCategoryNameException("Comida"));

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Comida\",\"isDefault\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} updates a category and returns 200")
    void updateReturns200() throws Exception {
        when(categoryUseCase.updateCategory(USER_ID, CATEGORY_ID, "Nueva", null, null, null))
                .thenReturn(buildDetail(0));

        mockMvc.perform(put("/api/v1/categories/" + CATEGORY_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} for a category that does not exist surfaces as 404")
    void updateUnknownCategoryReturns404() throws Exception {
        when(categoryUseCase.updateCategory(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CategoryNotFoundException(CATEGORY_ID));

        mockMvc.perform(put("/api/v1/categories/" + CATEGORY_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} with no reassignTo deletes and returns 204")
    void deleteWithoutReassignReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + CATEGORY_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryUseCase).deleteCategory(eq(USER_ID), eq(CATEGORY_ID), isNull());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id}?reassignTo=... passes the reassignment target through")
    void deleteWithReassignPassesTarget() throws Exception {
        UUID reassignTo = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/" + CATEGORY_ID)
                        .param("reassignTo", reassignTo.toString())
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(categoryUseCase).deleteCategory(USER_ID, CATEGORY_ID, reassignTo);
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} for a category still in use surfaces as 409")
    void deleteCategoryInUseReturns409() throws Exception {
        org.mockito.Mockito.doThrow(new CategoryInUseException(CATEGORY_ID, 3))
                .when(categoryUseCase).deleteCategory(any(), any(), any());

        mockMvc.perform(delete("/api/v1/categories/" + CATEGORY_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/v1/categories/{id}/default sets the category as default and returns 200")
    void setDefaultReturns200() throws Exception {
        when(categoryUseCase.setDefaultCategory(USER_ID, CATEGORY_ID)).thenReturn(buildDetail(0));

        mockMvc.perform(patch("/api/v1/categories/" + CATEGORY_ID + "/default")
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID.toString()));
    }
}
