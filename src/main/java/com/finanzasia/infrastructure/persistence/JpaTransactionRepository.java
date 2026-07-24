package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;
import com.finanzasia.domain.port.out.TransactionRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Uses cursor-based (keyset) pagination. Cursor format is Base64(transactionDate:uuid), opaque to callers.
public class JpaTransactionRepository implements TransactionRepository {

    private final JpaTransactionRepositoryPort jpaPort;
    private final JpaTagRepositoryPort jpaTagPort;

    public JpaTransactionRepository(
            JpaTransactionRepositoryPort jpaPort,
            JpaTagRepositoryPort jpaTagPort) {
        this.jpaPort = jpaPort;
        this.jpaTagPort = jpaTagPort;
    }

    @Override
    public TransactionPage findWithFilter(TransactionFilter filter) {
        String typeStr = filter.type() != null ? filter.type().name() : null;
        String tagIdStr = filter.tagId() != null ? filter.tagId().toString() : null;

        int fetchLimit = filter.limit() + 1; // fetch one extra to detect hasMore

        List<TransactionEntity> rows = jpaPort.findWithFilter(
                filter.userId(),
                typeStr,
                filter.accountId(),
                filter.categoryId(),
                filter.currency(),
                filter.from(),
                filter.to(),
                tagIdStr,
                filter.cursorDate(),
                filter.cursorId() != null ? filter.cursorId().toString() : null,
                fetchLimit);

        boolean hasMore = rows.size() > filter.limit();
        List<Transaction> items = rows.stream()
                .limit(filter.limit())
                .map(TransactionEntity::toDomain)
                .toList();

        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            Transaction last = items.get(items.size() - 1);
            nextCursor = encodeCursor(last.getTransactionDate(), last.getId());
        }

        return new TransactionPage(items, nextCursor, hasMore);
    }

    @Override
    public Optional<Transaction> findByIdAndUser(UUID id, UUID userId) {
        return jpaPort.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .map(TransactionEntity::toDomain);
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity;

        // Re-use the managed entity if it already exists so Hibernate tracks
        // the collection correctly, then apply the tag set from the domain object.
        Optional<TransactionEntity> existing =
                jpaPort.findByIdAndUserIdAndDeletedAtIsNull(transaction.getId(), transaction.getUserId());

        if (existing.isPresent()) {
            entity = existing.get();
            entity.setAmount(transaction.getAmount());
            entity.setCurrency(transaction.getCurrency());
            entity.setAccountId(transaction.getAccountId());
            entity.setFromAccountId(transaction.getFromAccountId());
            entity.setToAccountId(transaction.getToAccountId());
            entity.setCategoryId(transaction.getCategoryId());
            entity.setMerchant(transaction.getMerchant());
            entity.setDescription(transaction.getDescription());
            entity.setTransactionDate(transaction.getTransactionDate());
            entity.setUpdatedAt(transaction.getUpdatedAt());
            entity.setDeletedAt(transaction.getDeletedAt());
            entity.setAmountLocal(transaction.getAmountLocal());
            entity.setExchangeRateApplied(transaction.getExchangeRateApplied());
        } else {
            entity = TransactionEntity.fromDomain(transaction);
        }

        applyTags(entity, transaction.getTags());

        return jpaPort.save(entity).toDomain();
    }

    // Tags must be managed TagEntity instances loaded by ID for the join table to update correctly.
    private void applyTags(TransactionEntity entity, List<Tag> domainTags) {
        if (domainTags == null || domainTags.isEmpty()) {
            entity.getTags().clear();
            return;
        }
        Set<UUID> ids = new HashSet<>();
        for (Tag t : domainTags) {
            ids.add(t.id());
        }
        List<TagEntity> managedTags = jpaTagPort.findAllById(ids);
        entity.getTags().clear();
        entity.getTags().addAll(managedTags);
    }

    @Override
    public void softDelete(UUID transactionId, Instant now) {
        jpaPort.softDelete(transactionId, now);
    }

    public static String encodeCursor(LocalDate date, UUID id) {
        String raw = date.toString() + ":" + id.toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    // Returns null if the cursor is null or malformed, treated as "no cursor".
    public static CursorParts decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            return new CursorParts(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }

    public record CursorParts(LocalDate date, UUID id) {}
}
