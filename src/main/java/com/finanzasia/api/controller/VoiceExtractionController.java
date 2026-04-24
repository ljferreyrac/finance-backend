package com.finanzasia.api.controller;

import com.finanzasia.api.dto.TransactionDraftDTO;
import com.finanzasia.api.dto.VoiceExtractRequest;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.AIExtractionException;
import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.in.TranscribeAndExtractVoiceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Transactions", description = "Log and manage expenses, income, and account transfers")
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class VoiceExtractionController {

    private final ExtractTransactionsFromVoiceUseCase extractUseCase;
    private final TranscribeAndExtractVoiceUseCase transcribeAndExtractUseCase;

    public VoiceExtractionController(
            ExtractTransactionsFromVoiceUseCase extractUseCase,
            TranscribeAndExtractVoiceUseCase transcribeAndExtractUseCase) {
        this.extractUseCase = extractUseCase;
        this.transcribeAndExtractUseCase = transcribeAndExtractUseCase;
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
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = TransactionDraftDTO.class)))
            ),
            @ApiResponse(responseCode = "400",
                         description = "Validation error (transcript blank or too long)", content = @Content),
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

    @Operation(
            summary = "Transcribe audio and extract transactions",
            description = "Accepts a raw audio file (m4a, mp3, wav, ogg, flac — max 25 MB). "
                    + "Transcribes it with Groq Whisper, then extracts transaction drafts with Gemini. "
                    + "Returns the same draft list as /extract-voice."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Drafts extracted successfully",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = TransactionDraftDTO.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Missing or empty audio file", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "502", description = "Groq or Gemini service unavailable", content = @Content)
    })
    @PostMapping(value = "/voice-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<TransactionDraftDTO>> transcribeAndExtract(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "timezone", defaultValue = "UTC") String timezone,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] audioBytes = audio.getBytes();
            String filename = audio.getOriginalFilename() != null
                    ? audio.getOriginalFilename()
                    : "voice.m4a";

            List<TransactionDraft> drafts = transcribeAndExtractUseCase
                    .transcribeAndExtract(principal.getId(), audioBytes, filename, timezone);

            return ResponseEntity.ok(drafts.stream().map(this::toDTO).toList());

        } catch (IOException e) {
            throw new AIExtractionException("Failed to read uploaded audio: " + e.getMessage());
        }
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
