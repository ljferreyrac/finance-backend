package com.finanzasia.api.controller;

import com.finanzasia.api.dto.AccountSummaryDTO;
import com.finanzasia.api.dto.CategorySummaryDTO;
import com.finanzasia.api.dto.CreateTransactionRequest;
import com.finanzasia.api.dto.PagedTransactionsDTO;
import com.finanzasia.api.dto.TagDTO;
import com.finanzasia.api.dto.TransactionDTO;
import com.finanzasia.api.dto.UpdateTransactionRequest;
import com.finanzasia.api.security.UserPrincipal;
import com.finanzasia.domain.model.Account;
import com.finanzasia.domain.model.Category;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.TransactionUseCase;
import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import com.finanzasia.infrastructure.persistence.JpaTransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "Transactions", description = "Log and manage expenses, income, and account transfers")
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
public class TransactionController {

    private static final int MAX_LIMIT = 100;

    private final TransactionUseCase transactionUseCase;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionController(
            TransactionUseCase transactionUseCase,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository) {
        this.transactionUseCase = transactionUseCase;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Operation(summary = "List transactions",
               description = "Returns a cursor-paginated list of transactions. "
                           + "Pass the nextCursor value from a previous response as the 'cursor' parameter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction page",
                         content = @Content(schema = @Schema(implementation = PagedTransactionsDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @GetMapping
    public PagedTransactionsDTO listTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) UUID tagId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        JpaTransactionRepository.CursorParts cursorParts =
                JpaTransactionRepository.decodeCursor(cursor);

        TransactionFilter filter = new TransactionFilter(
                principal.getId(),
                type,
                accountId,
                categoryId,
                from,
                to,
                currency,
                tagId,
                cursorParts != null ? cursorParts.date() : null,
                cursorParts != null ? cursorParts.id() : null,
                safeLimit);

        TransactionPage page = transactionUseCase.listTransactions(filter);

        List<UUID> accountIds = page.items().stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getAccountId(), t.getFromAccountId(), t.getToAccountId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream().toList();

        Map<UUID, Account> accountMap = accountRepository.findAllByUser(principal.getId())
                .stream()
                .filter(a -> accountIds.contains(a.getId()))
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Set<UUID> categoryIds = page.items().stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Category> categoryMap = categoryRepository.findAllByUser(principal.getId())
                .stream()
                .filter(c -> categoryIds.contains(c.getId()))
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<TransactionDTO> dtos = page.items().stream()
                .map(t -> toDTO(t, accountMap, categoryMap))
                .toList();

        // totalCount is the full count without cursor (uses the same filters minus cursor)
        long totalCount = page.items().size(); // approximate; full count omitted for performance

        return new PagedTransactionsDTO(dtos, page.nextCursor(), page.hasMore(), totalCount);
    }

    @Operation(summary = "Create transaction", description = "Creates a new expense, income, or transfer transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                         content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "400",
                         description = "Validation error or invalid transaction", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account or category not found", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDTO createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTransactionRequest request) {

        Transaction transaction = transactionUseCase.createTransaction(
                principal.getId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.accountId(),
                request.fromAccountId(),
                request.toAccountId(),
                request.categoryId(),
                request.merchant(),
                request.description(),
                request.transactionDate(),
                request.tagIds(),
                request.amountLocal());

        return toDTO(transaction, loadAccountMap(principal.getId()), loadCategoryMap(principal.getId()));
    }

    @Operation(summary = "Get transaction", description = "Returns a single transaction by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found",
                         content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    @GetMapping("/{id}")
    public TransactionDTO getTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID") @PathVariable UUID id) {

        Transaction transaction = transactionUseCase.getTransaction(principal.getId(), id);
        return toDTO(transaction, loadAccountMap(principal.getId()), loadCategoryMap(principal.getId()));
    }

    @Operation(summary = "Update transaction",
               description = "Updates an existing transaction. Balance adjustments are reversed and reapplied.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated",
                         content = @Content(schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction or account not found", content = @Content)
    })
    @PutMapping("/{id}")
    public TransactionDTO updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionRequest request) {

        Transaction transaction = transactionUseCase.updateTransaction(
                principal.getId(),
                id,
                request.amount(),
                request.currency(),
                request.accountId(),
                request.fromAccountId(),
                request.toAccountId(),
                request.categoryId(),
                request.merchant(),
                request.description(),
                request.transactionDate(),
                request.tagIds(),
                request.amountLocal());

        return toDTO(transaction, loadAccountMap(principal.getId()), loadCategoryMap(principal.getId()));
    }

    @Operation(summary = "Delete transaction",
               description = "Soft-deletes a transaction and reverses its balance effect on the account.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Transaction UUID") @PathVariable UUID id) {

        transactionUseCase.deleteTransaction(principal.getId(), id);
    }

    // --- presentation helpers ---

    private TransactionDTO toDTO(
            Transaction t,
            Map<UUID, Account> accountMap,
            Map<UUID, Category> categoryMap) {

        AccountSummaryDTO account = t.getAccountId() != null
                ? toAccountSummary(accountMap.get(t.getAccountId()))
                : null;
        AccountSummaryDTO fromAccount = t.getFromAccountId() != null
                ? toAccountSummary(accountMap.get(t.getFromAccountId()))
                : null;
        AccountSummaryDTO toAccount = t.getToAccountId() != null
                ? toAccountSummary(accountMap.get(t.getToAccountId()))
                : null;

        CategorySummaryDTO category = t.getCategoryId() != null
                ? toCategorySummary(categoryMap.get(t.getCategoryId()))
                : null;

        List<TagDTO> tags = t.getTags() != null
                ? t.getTags().stream().map(this::toTagDTO).toList()
                : Collections.emptyList();

        return new TransactionDTO(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getCurrency(),
                account,
                fromAccount,
                toAccount,
                category,
                t.getMerchant(),
                t.getDescription(),
                t.getTransactionDate(),
                t.getCreatedAt(),
                tags,
                t.getAmountLocal(),
                t.getExchangeRateApplied());
    }

    private AccountSummaryDTO toAccountSummary(Account account) {
        if (account == null) { return null; }
        return new AccountSummaryDTO(account.getId(), account.getName(), account.getColor(), account.getType());
    }

    private CategorySummaryDTO toCategorySummary(Category category) {
        if (category == null) { return null; }
        return new CategorySummaryDTO(category.getId(), category.getName(), category.getColor(), category.getIcon());
    }

    private Map<UUID, Account> loadAccountMap(UUID userId) {
        return accountRepository.findAllByUser(userId)
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private Map<UUID, Category> loadCategoryMap(UUID userId) {
        return categoryRepository.findAllByUser(userId)
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private TagDTO toTagDTO(com.finanzasia.domain.model.Tag tag) {
        return new TagDTO(tag.id(), tag.name(), tag.color());
    }
}
