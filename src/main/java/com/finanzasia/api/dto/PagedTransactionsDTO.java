package com.finanzasia.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list of transactions with cursor for next page")
public record PagedTransactionsDTO(

        @Schema(description = "Transactions in this page") List<TransactionDTO> items,
        @Schema(description = "Opaque cursor to pass as the 'cursor' query parameter for the next page")
        String nextCursor,
        @Schema(description = "Whether more results exist after this page") boolean hasMore,
        @Schema(description = "Total number of matching transactions (ignoring pagination)") long totalCount
) {}
