package com.finanzasia.api.controller;

import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.DuplicateTagException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.port.in.AuthenticateAccessTokenUseCase;
import com.finanzasia.domain.port.in.TagUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagUseCase tagUseCase;

    // JwtAuthFilter and RateLimitFilter are Filter-typed @Components, so @WebMvcTest
    // instantiates them too; these satisfy their constructors. Tests bypass real
    // authentication via .with(user(...)) instead of exercising the filter itself.
    @MockitoBean
    private AuthenticateAccessTokenUseCase authenticateAccessToken;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private UserPrincipal principal;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TAG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(USER_ID, "user@example.com");
    }

    @Test
    @DisplayName("GET /api/v1/tags returns the caller's tags as JSON")
    void listTagsReturnsOwnedTags() throws Exception {
        when(tagUseCase.listTags(USER_ID)).thenReturn(
                List.of(new Tag(TAG_ID, USER_ID, "viaje", "#FF5733")));

        mockMvc.perform(get("/api/v1/tags").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(TAG_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("viaje"))
                .andExpect(jsonPath("$[0].color").value("#FF5733"));
    }

    @Test
    @DisplayName("GET /api/v1/tags without authentication is rejected with 401")
    void listTagsWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/tags creates a tag and returns 201")
    void createTagReturns201() throws Exception {
        when(tagUseCase.createTag(eq(USER_ID), eq("viaje"), eq("#FF5733")))
                .thenReturn(new Tag(TAG_ID, USER_ID, "viaje", "#FF5733"));

        mockMvc.perform(post("/api/v1/tags")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"viaje\",\"color\":\"#FF5733\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TAG_ID.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/tags with a blank name fails bean validation with 400")
    void createTagWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"color\":\"#FF5733\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/tags for a name that already exists surfaces as 409 via GlobalExceptionHandler")
    void createTagDuplicateNameReturns409() throws Exception {
        when(tagUseCase.createTag(any(), any(), any())).thenThrow(new DuplicateTagException("viaje"));

        mockMvc.perform(post("/api/v1/tags")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"viaje\",\"color\":\"#FF5733\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/tags/{id} updates a tag and returns 200")
    void updateTagReturns200() throws Exception {
        when(tagUseCase.updateTag(USER_ID, TAG_ID, "nuevo", "#000000"))
                .thenReturn(new Tag(TAG_ID, USER_ID, "nuevo", "#000000"));

        mockMvc.perform(put("/api/v1/tags/" + TAG_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nuevo\",\"color\":\"#000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nuevo"));
    }

    @Test
    @DisplayName("PUT /api/v1/tags/{id} for a tag that does not exist surfaces as 404")
    void updateUnknownTagReturns404() throws Exception {
        when(tagUseCase.updateTag(any(), any(), any(), any())).thenThrow(new TagNotFoundException(TAG_ID));

        mockMvc.perform(put("/api/v1/tags/" + TAG_ID)
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"nuevo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/tags/{id} deletes a tag and returns 204")
    void deleteTagReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/" + TAG_ID).with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(tagUseCase).deleteTag(USER_ID, TAG_ID);
    }
}
