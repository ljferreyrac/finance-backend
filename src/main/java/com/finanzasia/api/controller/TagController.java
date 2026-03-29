package com.finanzasia.api.controller;

import com.finanzasia.api.dto.CreateTagRequest;
import com.finanzasia.api.dto.TagDTO;
import com.finanzasia.api.dto.UpdateTagRequest;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.port.in.TagUseCase;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Tags", description = "CRUD operations for user-defined transaction labels")
@RestController
@RequestMapping("/api/v1/tags")
@Validated
public class TagController {

    private final TagUseCase tagUseCase;

    public TagController(TagUseCase tagUseCase) {
        this.tagUseCase = tagUseCase;
    }

    @Operation(summary = "List tags",
               description = "Returns all tags owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag list",
                         content = @Content(schema = @Schema(implementation = TagDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping
    public List<TagDTO> listTags(@AuthenticationPrincipal UserPrincipal principal) {
        return tagUseCase.listTags(principal.getId())
                .stream()
                .map(t -> new TagDTO(t.id(), t.name(), t.color()))
                .toList();
    }

    @Operation(summary = "Create a tag",
               description = "Creates a new tag for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag created",
                         content = @Content(schema = @Schema(implementation = TagDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "409", description = "A tag with this name already exists", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagDTO createTag(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTagRequest request) {

        return toDTO(tagUseCase.createTag(principal.getId(), request.name(), request.color()));
    }

    @Operation(summary = "Update a tag",
               description = "Updates an existing tag. Omitted fields retain their current values.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag updated",
                         content = @Content(schema = @Schema(implementation = TagDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "A tag with this name already exists", content = @Content)
    })
    @PutMapping("/{id}")
    public TagDTO updateTag(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Tag UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTagRequest request) {

        return toDTO(tagUseCase.updateTag(principal.getId(), id, request.name(), request.color()));
    }

    @Operation(summary = "Delete a tag",
               description = "Deletes a tag owned by the authenticated user. "
                           + "The tag is removed from all transactions it was attached to.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Tag UUID") @PathVariable UUID id) {

        tagUseCase.deleteTag(principal.getId(), id);
    }

    // --- presentation helpers ---

    private TagDTO toDTO(com.finanzasia.domain.model.Tag tag) {
        return new TagDTO(tag.id(), tag.name(), tag.color());
    }
}
