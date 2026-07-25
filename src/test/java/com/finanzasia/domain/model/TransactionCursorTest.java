package com.finanzasia.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCursorTest {

    @Nested
    @DisplayName("encode/decode")
    class RoundTrip {

        @Test
        @DisplayName("decode(encode(x)) returns the original date and id")
        void roundTripsCleanly() {
            LocalDate date = LocalDate.of(2026, 3, 28);
            UUID id = UUID.randomUUID();

            String token = TransactionCursor.encode(date, id);
            TransactionCursor decoded = TransactionCursor.decode(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.date()).isEqualTo(date);
            assertThat(decoded.id()).isEqualTo(id);
        }
    }

    @Nested
    @DisplayName("decode of absent or malformed cursors")
    class MalformedInput {

        @Test
        @DisplayName("null cursor decodes to null")
        void nullCursorIsNull() {
            assertThat(TransactionCursor.decode(null)).isNull();
        }

        @Test
        @DisplayName("blank cursor decodes to null")
        void blankCursorIsNull() {
            assertThat(TransactionCursor.decode("   ")).isNull();
        }

        @Test
        @DisplayName("a token missing the ':' separator decodes to null")
        void missingSeparatorIsNull() {
            String noSeparator = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("2026-03-28".getBytes());

            assertThat(TransactionCursor.decode(noSeparator)).isNull();
        }

        @Test
        @DisplayName("a token whose date part does not parse decodes to null")
        void invalidDatePartIsNull() {
            String badDate = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("not-a-date:" + UUID.randomUUID()).getBytes());

            assertThat(TransactionCursor.decode(badDate)).isNull();
        }

        @Test
        @DisplayName("a token whose id part is not a valid UUID decodes to null")
        void invalidIdPartIsNull() {
            String badId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("2026-03-28:not-a-uuid".getBytes());

            assertThat(TransactionCursor.decode(badId)).isNull();
        }

        @Test
        @DisplayName("a token that is not valid Base64 decodes to null")
        void notBase64IsNull() {
            assertThat(TransactionCursor.decode("!!!not-base64!!!")).isNull();
        }
    }
}
