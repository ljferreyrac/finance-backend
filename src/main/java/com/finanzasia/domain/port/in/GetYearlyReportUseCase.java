package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.YearlyReport;

import java.util.UUID;

/** Input port: retrieve an aggregated yearly expense report. */
public interface GetYearlyReportUseCase {

    /**
     * Builds and returns the yearly report for the given user, year, and currency.
     * Always returns a valid report shape; years with no data return zeroed summaries.
     *
     * @param userId     the authenticated user's identifier
     * @param year       calendar year, e.g. 2026
     * @param currency   currency code: "PEN" or "USD"
     * @param accountId  optional filter: only include transactions on this account
     * @param categoryId optional filter: only include transactions in this category
     * @param tagId      optional filter: only include transactions that carry this tag
     * @return fully populated {@link YearlyReport}
     */
    YearlyReport getYearlyReport(UUID userId, int year, String currency,
                                 UUID accountId, UUID categoryId, UUID tagId);
}
