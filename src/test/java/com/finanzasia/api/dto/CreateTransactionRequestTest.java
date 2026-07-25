package com.finanzasia.api.dto;

import com.finanzasia.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateTransactionRequestTest {

    @Test
    @DisplayName("a null tagIds list is normalized to an empty list by the compact constructor")
    void nullTagIdsBecomesEmptyList() {
        CreateTransactionRequest request = new CreateTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("10.00"), "PEN",
                UUID.randomUUID(), null, null, UUID.randomUUID(),
                "Wong", "desc", LocalDate.now(), null, null);

        assertThat(request.tagIds()).isEmpty();
    }

    @Test
    @DisplayName("a non-null tagIds list is preserved as-is")
    void nonNullTagIdsIsPreserved() {
        List<UUID> tagIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        CreateTransactionRequest request = new CreateTransactionRequest(
                TransactionType.EXPENSE, new BigDecimal("10.00"), "PEN",
                UUID.randomUUID(), null, null, UUID.randomUUID(),
                "Wong", "desc", LocalDate.now(), tagIds, null);

        assertThat(request.tagIds()).isEqualTo(tagIds);
    }
}
