package com.finanzasia.api.controller;

import com.finanzasia.api.dto.TransactionDraftDTO;
import com.finanzasia.api.dto.VoiceExtractRequest;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Transactions", description = "Log and manage expenses, income, and account transfers")
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class VoiceExtractionController {

    private final ExtractTransactionsFromVoiceUseCase extractUseCase;

    public VoiceExtractionController(ExtractTransactionsFromVoiceUseCase extractUseCase) {
        this.extractUseCase = extractUseCase;
    }

    @Operation(
            summary = "Extract transactions from voice transcript",
            description = "Sends a plain-text voice transcript to the AI model. "
                    + "Returns a list of draft transactions pre-filled with the authenticated user's "
                    + "categories and accounts for review before confirmation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Drafts extracted successfully (list may be empty if nothing was detected)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionDraftDTO.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error (transcript blank or too long)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "502", description = "AI service unavailable", content = @Content)
    })
    @PostMapping("/extract-voice")
    public ResponseEntity<List<TransactionDraftDTO>> extractFromVoice(
            @Valid @RequestBody VoiceExtractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<TransactionDraft> drafts = extractUseCase.extract(
                principal.getId(), request.transcript(), request.userTimezone());

        List<TransactionDraftDTO> dtos = drafts.stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ------------------------------------------------------------------
    // Presentation helper
    // ------------------------------------------------------------------

    private TransactionDraftDTO toDTO(TransactionDraft draft) {
        return new TransactionDraftDTO(
                draft.type() != null ? draft.type().name() : null,
                draft.amount(),
                draft.currency(),
                draft.categoryId() != null ? draft.categoryId().toString() : null,
                draft.categoryName(),
                draft.accountId() != null ? draft.accountId().toString() : null,
                draft.accountName(),
                draft.merchant(),
                draft.description(),
                draft.transactionDate() != null ? draft.transactionDate().toString() : null,
                draft.confidence(),
                draft.tagIds() != null
                        ? draft.tagIds().stream().map(java.util.UUID::toString).toList()
                        : List.of());
    }
}
