package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "User-defined label attached to transactions")
public record TagDTO(
        @Schema(description = "Tag UUID") UUID id,
        @Schema(description = "Tag name", example = "deducible") String name,
        @Schema(description = "Hex colour code", example = "#FF5733") String color
) {}
