package com.finanzasia.api.controller;

import com.finanzasia.api.dto.MonthlyReportDTO;
import com.finanzasia.api.dto.YearlyReportDTO;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.exceptions.InvalidReportParameterException;
import com.finanzasia.domain.model.MonthlyReport;
import com.finanzasia.domain.model.YearlyReport;
import com.finanzasia.domain.port.in.GetMonthlyReportUseCase;
import com.finanzasia.domain.port.in.GetYearlyReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reports", description = "Read-only aggregated expense reports for charts and dashboards")
@RestController
@RequestMapping("/api/v1/reports")
@Validated
public class ReportController {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final GetMonthlyReportUseCase getMonthlyReport;
    private final GetYearlyReportUseCase getYearlyReport;

    public ReportController(
            GetMonthlyReportUseCase getMonthlyReport,
            GetYearlyReportUseCase getYearlyReport) {
        this.getMonthlyReport = getMonthlyReport;
        this.getYearlyReport = getYearlyReport;
    }

    @Operation(
            summary = "Monthly expense report",
            description = "Returns aggregated expense data for a single calendar month. "
                    + "Periods with no data return zeroed summaries so the UI always has a valid shape to render. "
                    + "All filter parameters are optional; omitting them includes all transactions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated",
                         content = @Content(schema = @Schema(implementation = MonthlyReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid year, month, or currency", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping("/monthly")
    public MonthlyReportDTO monthly(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(description = "Calendar year, e.g. 2026. Defaults to the current year.")
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,

            @Parameter(description = "Calendar month 1-12. Defaults to the current month.")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,

            @Parameter(description = "Currency code.", schema = @Schema(allowableValues = {"PEN", "USD"}))
            @RequestParam(defaultValue = "PEN") String currency,

            @Parameter(description = "Optional: restrict results to a single account.")
            @RequestParam(required = false) UUID accountId,

            @Parameter(description = "Optional: restrict results to a single category.")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Optional: restrict results to transactions carrying a specific tag.")
            @RequestParam(required = false) UUID tagId) {

        validateParams(year, month, currency);

        MonthlyReport report = getMonthlyReport.getMonthlyReport(
                principal.getId(), year, month, currency, accountId, categoryId, tagId);

        return MonthlyReportDTO.from(report);
    }

    @Operation(
            summary = "Yearly expense report",
            description = "Returns aggregated expense data for a full calendar year. "
                    + "Only months that have at least one transaction appear in byMonth; "
                    + "the client fills the remaining months with zero when rendering charts. "
                    + "All filter parameters are optional; omitting them includes all transactions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated",
                         content = @Content(schema = @Schema(implementation = YearlyReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid year or currency", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping("/yearly")
    public YearlyReportDTO yearly(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(description = "Calendar year, e.g. 2026. Defaults to the current year.")
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,

            @Parameter(description = "Currency code.", schema = @Schema(allowableValues = {"PEN", "USD"}))
            @RequestParam(defaultValue = "PEN") String currency,

            @Parameter(description = "Optional: restrict results to a single account.")
            @RequestParam(required = false) UUID accountId,

            @Parameter(description = "Optional: restrict results to a single category.")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Optional: restrict results to transactions carrying a specific tag.")
            @RequestParam(required = false) UUID tagId) {

        validateYearAndCurrency(year, currency);

        YearlyReport report = getYearlyReport.getYearlyReport(
                principal.getId(), year, currency, accountId, categoryId, tagId);

        return YearlyReportDTO.from(report);
    }

    private void validateParams(int year, int month, String currency) {
        validateYearAndCurrency(year, currency);
        if (month < 1 || month > 12) {
            throw new InvalidReportParameterException(
                    "month must be between 1 and 12, got: " + month);
        }
    }

    private void validateYearAndCurrency(int year, String currency) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new InvalidReportParameterException(
                    "year must be between " + MIN_YEAR + " and " + MAX_YEAR + ", got: " + year);
        }
        if (!"PEN".equals(currency) && !"USD".equals(currency)) {
            throw new InvalidReportParameterException(
                    "currency must be PEN or USD, got: " + currency);
        }
    }
}
