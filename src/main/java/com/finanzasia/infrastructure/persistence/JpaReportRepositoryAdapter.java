package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.port.out.ReportRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class JpaReportRepositoryAdapter implements ReportRepository {

    private final JpaReportRepositoryPort jpaPort;

    public JpaReportRepositoryAdapter(JpaReportRepositoryPort jpaPort) {
        this.jpaPort = jpaPort;
    }

    @Override
    public BigDecimal sumByMonth(UUID userId, int year, int month, String currency,
                                 UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.sumByMonth(userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public long countByMonth(UUID userId, int year, int month, String currency,
                             UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.countByMonth(userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<CategoryRow> categoryBreakdownByMonth(
            UUID userId, int year, int month, String currency,
            UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findCategoryBreakdownByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<WeekRow> weekBreakdownByMonth(
            UUID userId, int year, int month, String currency,
            UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findWeekBreakdownByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<MerchantRow> topMerchantsByMonth(
            UUID userId, int year, int month, String currency,
            UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findTopMerchantsByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public BigDecimal sumIncomeByMonth(UUID userId, int year, int month, String currency,
                                       UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.sumIncomeByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public long countIncomeByMonth(UUID userId, int year, int month, String currency,
                                   UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.countIncomeByMonth(
                userId, year, month, currency, accountId, categoryId, tagId);
    }

    @Override
    public BigDecimal sumByYear(UUID userId, int year, String currency,
                                UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.sumByYear(userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public long countByYear(UUID userId, int year, String currency,
                            UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.countByYear(userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<MonthRow> monthBreakdownByYear(UUID userId, int year, String currency,
                                               UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findMonthBreakdownByYear(
                userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<CategoryRow> categoryBreakdownByYear(UUID userId, int year, String currency,
                                                     UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findCategoryBreakdownByYear(
                userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public List<MerchantRow> topMerchantsByYear(UUID userId, int year, String currency,
                                                UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.findTopMerchantsByYear(
                userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public BigDecimal sumIncomeByYear(UUID userId, int year, String currency,
                                      UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.sumIncomeByYear(userId, year, currency, accountId, categoryId, tagId);
    }

    @Override
    public long countIncomeByYear(UUID userId, int year, String currency,
                                  UUID accountId, UUID categoryId, UUID tagId) {
        return jpaPort.countIncomeByYear(userId, year, currency, accountId, categoryId, tagId);
    }
}
