package com.finanzasia.infrastructure.persistence;

import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.model.Transaction;
import com.finanzasia.domain.model.TransactionFilter;
import com.finanzasia.domain.model.TransactionPage;
import com.finanzasia.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaTransactionRepositoryTest {

    @Mock
    private JpaTransactionRepositoryPort jpaPort;

    @Mock
    private JpaTagRepositoryPort jpaTagPort;

    private JpaTransactionRepository repository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = new JpaTransactionRepository(jpaPort, jpaTagPort);
    }

    private Transaction buildTransaction(List<Tag> tags) {
        Instant now = Instant.now();
        return new Transaction(TX_ID, USER_ID, TransactionType.EXPENSE, new BigDecimal("10.00"), "PEN",
                ACCOUNT_ID, null, null, CATEGORY_ID, "Wong", "desc", null,
                LocalDate.now(), now, now, null, tags, null, null);
    }

    private TransactionEntity entityFor(Transaction transaction) {
        return TransactionEntity.fromDomain(transaction);
    }

    private TransactionFilter buildFilter(TransactionType type, UUID tagId, UUID cursorId, int limit) {
        return new TransactionFilter(USER_ID, type, ACCOUNT_ID, CATEGORY_ID,
                LocalDate.now().minusDays(30), LocalDate.now(), "PEN", tagId,
                cursorId == null ? null : LocalDate.now(), cursorId, limit);
    }

    @Nested
    @DisplayName("findWithFilter")
    class FindWithFilter {

        @Test
        @DisplayName("a null type, tagId and cursorId are all passed through to the port as null")
        void nullOptionalFiltersPassThroughAsNull() {
            TransactionFilter filter = buildFilter(null, null, null, 10);
            when(jpaPort.findWithFilter(eq(USER_ID), isNull(), any(), any(), any(),
                    any(), any(), isNull(), any(), isNull(), anyInt()))
                    .thenReturn(List.of());

            repository.findWithFilter(filter);

            verify(jpaPort).findWithFilter(eq(USER_ID), isNull(), eq(ACCOUNT_ID), eq(CATEGORY_ID),
                    eq("PEN"), any(), any(), isNull(), isNull(), isNull(), eq(11));
        }

        @Test
        @DisplayName("a non-null type and tagId are converted to their string forms")
        void nonNullTypeAndTagIdAreStringified() {
            UUID tagId = UUID.randomUUID();
            TransactionFilter filter = buildFilter(TransactionType.EXPENSE, tagId, null, 10);
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            repository.findWithFilter(filter);

            verify(jpaPort).findWithFilter(eq(USER_ID), eq("EXPENSE"), eq(ACCOUNT_ID), eq(CATEGORY_ID),
                    eq("PEN"), any(), any(), eq(tagId.toString()), any(), isNull(), eq(11));
        }

        @Test
        @DisplayName("a non-null cursorId is converted to its string form")
        void nonNullCursorIdIsStringified() {
            UUID cursorId = UUID.randomUUID();
            TransactionFilter filter = buildFilter(null, null, cursorId, 10);
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            repository.findWithFilter(filter);

            verify(jpaPort).findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), eq(cursorId.toString()), anyInt());
        }

        @Test
        @DisplayName("fetches one extra row beyond the requested limit to detect hasMore")
        void fetchesOneExtraRow() {
            TransactionFilter filter = buildFilter(null, null, null, 5);
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            repository.findWithFilter(filter);

            verify(jpaPort).findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), eq(6));
        }

        @Test
        @DisplayName("when fewer rows than limit+1 come back, hasMore is false and nextCursor is null")
        void fewerRowsThanLimitMeansNoMore() {
            TransactionFilter filter = buildFilter(null, null, null, 10);
            Transaction tx = buildTransaction(List.of());
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(entityFor(tx)));

            TransactionPage page = repository.findWithFilter(filter);

            assertThat(page.hasMore()).isFalse();
            assertThat(page.nextCursor()).isNull();
            assertThat(page.items()).hasSize(1);
        }

        @Test
        @DisplayName("when limit+1 rows come back, hasMore is true, the extra row is trimmed, "
                + "and nextCursor encodes the last item")
        void extraRowSignalsMoreAndBuildsCursor() {
            TransactionFilter filter = buildFilter(null, null, null, 1);
            Transaction first = buildTransaction(List.of());
            Transaction second = buildTransaction(List.of());
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(entityFor(first), entityFor(second)));

            TransactionPage page = repository.findWithFilter(filter);

            assertThat(page.hasMore()).isTrue();
            assertThat(page.items()).hasSize(1);
            assertThat(page.nextCursor()).isNotBlank();
        }

        @Test
        @DisplayName("a limit of zero can report hasMore true with zero items, and must not build a cursor")
        void zeroLimitWithHasMoreDoesNotBuildCursor() {
            TransactionFilter filter = buildFilter(null, null, null, 0);
            Transaction tx = buildTransaction(List.of());
            when(jpaPort.findWithFilter(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(entityFor(tx)));

            TransactionPage page = repository.findWithFilter(filter);

            assertThat(page.hasMore()).isTrue();
            assertThat(page.items()).isEmpty();
            assertThat(page.nextCursor()).isNull();
        }
    }

    @Test
    @DisplayName("findByIdAndUser maps a present, non-deleted entity")
    void findByIdAndUserMapsPresentEntity() {
        Transaction tx = buildTransaction(List.of());
        when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID))
                .thenReturn(Optional.of(entityFor(tx)));

        Optional<Transaction> result = repository.findByIdAndUser(TX_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(TX_ID);
    }

    @Test
    @DisplayName("findByIdAndUser returns empty when the port finds nothing")
    void findByIdAndUserReturnsEmptyWhenAbsent() {
        when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(repository.findByIdAndUser(TX_ID, USER_ID)).isEmpty();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("when no existing entity is found, a fresh entity is built from the domain object")
        void noExistingEntityBuildsFresh() {
            Transaction tx = buildTransaction(List.of());
            when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID)).thenReturn(Optional.empty());
            when(jpaPort.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            Transaction result = repository.save(tx);

            assertThat(result.getId()).isEqualTo(TX_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(tx.getAmount());
        }

        @Test
        @DisplayName("when an existing entity is found, its mutable fields are updated in place")
        void existingEntityIsUpdatedInPlace() {
            Transaction original = buildTransaction(List.of());
            TransactionEntity existingEntity = entityFor(original);

            Transaction updated = new Transaction(TX_ID, USER_ID, TransactionType.EXPENSE,
                    new BigDecimal("99.00"), "USD", ACCOUNT_ID, null, null, CATEGORY_ID,
                    "New Merchant", "new desc", null, LocalDate.now(), original.getCreatedAt(),
                    Instant.now(), null, List.of(), null, null);

            when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID))
                    .thenReturn(Optional.of(existingEntity));
            ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
            when(jpaPort.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            repository.save(updated);

            TransactionEntity saved = captor.getValue();
            assertThat(saved).isSameAs(existingEntity);
            assertThat(saved.getAmount()).isEqualByComparingTo("99.00");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getMerchant()).isEqualTo("New Merchant");
        }

        @Test
        @DisplayName("a non-empty tag list is resolved to managed TagEntity instances via findAllById")
        void nonEmptyTagsAreResolvedToManagedEntities() {
            Tag tag = new Tag(UUID.randomUUID(), USER_ID, "viaje", "#FFF");
            Transaction tx = buildTransaction(List.of(tag));
            TagEntity managedTag = TagEntity.fromTag(tag);

            when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID)).thenReturn(Optional.empty());
            when(jpaTagPort.findAllById(Set.of(tag.id()))).thenReturn(List.of(managedTag));
            when(jpaPort.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            Transaction result = repository.save(tx);

            assertThat(result.getTags()).containsExactly(tag);
        }

        @Test
        @DisplayName("an empty tag list clears any previously attached tags without querying findAllById")
        void emptyTagsClearWithoutQuerying() {
            Transaction tx = buildTransaction(List.of());
            when(jpaPort.findByIdAndUserIdAndDeletedAtIsNull(TX_ID, USER_ID)).thenReturn(Optional.empty());
            when(jpaPort.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            Transaction result = repository.save(tx);

            assertThat(result.getTags()).isEmpty();
            verify(jpaTagPort, never()).findAllById(any());
        }
    }

    @Test
    @DisplayName("softDelete forwards the id and timestamp to the port")
    void softDeleteForwardsIdAndTimestamp() {
        Instant now = Instant.now();

        repository.softDelete(TX_ID, now);

        verify(jpaPort).softDelete(TX_ID, now);
    }
}
