package com.finanzasia.domain.port.out;

import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    TransactionPage findWithFilter(TransactionFilter filter);

    Optional<Transaction> findByIdAndUser(UUID id, UUID userId);

    Transaction save(Transaction transaction);

    void softDelete(UUID transactionId, Instant now);
}
