package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.TransactionDetail;
import com.finanzasia.domain.model.TransactionDetailPage;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {

    TransactionDetailPage listTransactions(TransactionFilter filter);

    TransactionDetail getTransaction(UUID userId, UUID transactionId);

    TransactionDetail createTransaction(
            UUID userId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            LocalDate transactionDate,
            List<UUID> tagIds,
            BigDecimal amountLocal);

    TransactionDetail updateTransaction(
            UUID userId,
            UUID transactionId,
            BigDecimal amount,
            String currency,
            UUID accountId,
            UUID fromAccountId,
            UUID toAccountId,
            UUID categoryId,
            String merchant,
            String description,
            LocalDate transactionDate,
            List<UUID> tagIds,
            BigDecimal amountLocal);

    void deleteTransaction(UUID userId, UUID transactionId);
}
