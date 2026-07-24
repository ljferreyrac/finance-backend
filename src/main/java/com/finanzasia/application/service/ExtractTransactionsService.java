package com.finanzasia.application.service;

import com.finanzasia.domain.model.TransactionDraft;
import com.finanzasia.domain.model.TransactionType;
import com.finanzasia.domain.port.in.ExtractTransactionsFromVoiceUseCase;
import com.finanzasia.domain.port.out.AIExtractionPort;
import com.finanzasia.domain.port.out.AIExtractionPort.AccountContext;
import com.finanzasia.domain.port.out.AIExtractionPort.AITransactionRaw;
import com.finanzasia.domain.port.out.AIExtractionPort.CategoryContext;
import com.finanzasia.domain.port.out.AIExtractionPort.TagContext;
import com.finanzasia.domain.port.out.AccountRepository;
import com.finanzasia.domain.port.out.CategoryRepository;
import com.finanzasia.domain.port.out.TagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Loads the user's categories/accounts as AI naming context, delegates extraction to
 * {@link AIExtractionPort}, and maps raw output to {@link TransactionDraft}. No business-rule
 * validation happens here; that occurs when the user confirms via the create-transaction endpoint.
 */
@Service
public class ExtractTransactionsService implements ExtractTransactionsFromVoiceUseCase {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TagRepository tagRepository;
    private final AIExtractionPort aiExtractionPort;

    public ExtractTransactionsService(
            CategoryRepository categoryRepository,
            AccountRepository accountRepository,
            TagRepository tagRepository,
            AIExtractionPort aiExtractionPort) {
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.tagRepository = tagRepository;
        this.aiExtractionPort = aiExtractionPort;
    }

    @Override
    public List<TransactionDraft> extract(UUID userId, String transcript, String userTimezone) {
        List<CategoryContext> categories = categoryRepository.findAllByUser(userId)
                .stream()
                .map(c -> new CategoryContext(c.getId(), c.getName()))
                .toList();

        List<AccountContext> accounts = accountRepository.findAllByUser(userId)
                .stream()
                .map(a -> new AccountContext(a.getId(), a.getName(), a.getBank(), a.getCurrency()))
                .toList();

        List<TagContext> tags = tagRepository.findByUserId(userId)
                .stream()
                .map(t -> new TagContext(t.id(), t.name()))
                .toList();

        List<AITransactionRaw> rawResults =
                aiExtractionPort.extractFromText(transcript, categories, accounts, tags, userTimezone);

        return rawResults.stream()
                .map(raw -> toDraft(raw, categories, accounts, tags))
                .toList();
    }

    private TransactionDraft toDraft(
            AITransactionRaw raw,
            List<CategoryContext> categories,
            List<AccountContext> accounts,
            List<TagContext> tags) {

        TransactionType type = parseType(raw.type());

        UUID categoryId = parseUuid(raw.categoryId());
        String categoryName = resolveCategory(categoryId, categories, raw.categoryId());

        UUID accountId = parseUuid(raw.accountId());
        String accountName = resolveAccount(accountId, accounts, raw.accountId());

        LocalDate date = parseDate(raw.transactionDate());

        Set<UUID> validTagIds = tags.stream().map(TagContext::id).collect(java.util.stream.Collectors.toSet());
        List<UUID> resolvedTagIds = raw.tagIds() == null ? List.of() : raw.tagIds().stream()
                .map(this::parseUuid)
                .filter(id -> id != null && validTagIds.contains(id))
                .toList();

        return new TransactionDraft(
                type,
                raw.amount(),
                raw.currency(),
                categoryId,
                categoryName,
                accountId,
                accountName,
                raw.merchant(),
                raw.description(),
                date,
                raw.confidence(),
                resolvedTagIds);
    }

    private TransactionType parseType(String typeStr) {
        if (typeStr == null) {
            return TransactionType.EXPENSE;
        }
        try {
            return TransactionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return TransactionType.EXPENSE;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }

    private String resolveCategory(UUID categoryId, List<CategoryContext> categories, String rawValue) {
        if (categoryId == null) {
            return rawValue;
        }
        return categories.stream()
                .filter(c -> c.id().equals(categoryId))
                .map(CategoryContext::name)
                .findFirst()
                .orElse(null);
    }

    private String resolveAccount(UUID accountId, List<AccountContext> accounts, String rawValue) {
        if (accountId == null) {
            return rawValue;
        }
        return accounts.stream()
                .filter(a -> a.id().equals(accountId))
                .map(AccountContext::name)
                .findFirst()
                .orElse(null);
    }
}
