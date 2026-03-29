package com.finanzasia.domain.port.in;

import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;
import com.finanzasia.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input port for all transaction operations.
 * Implemented by the application service; invoked by the REST controller.
 */
public interface TransactionUseCase {

    TransactionPage listTransactions(TransactionFilter filter);

    Transaction getTransaction(UUID userId, UUID transactionId);

    Transaction createTransaction(
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

    Transaction updateTransaction(
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
