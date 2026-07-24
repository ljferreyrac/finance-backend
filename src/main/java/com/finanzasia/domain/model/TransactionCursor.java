package com.finanzasia.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque position in a cursor-paginated transaction list: the sort key of the
 * last item a client received.
 *
 * <p>Lives in the domain because keyset pagination is part of how transactions
 * are ordered, not a detail of the JPA adapter. Both the web layer (decoding an
 * incoming cursor) and the persistence adapter (encoding the next one) need it,
 * and neither should have to reach into the other to get it.
 */
public record TransactionCursor(LocalDate date, UUID id) {

    /** Encodes a position into the opaque token handed back to clients. */
    public static String encode(LocalDate date, UUID id) {
        String raw = date.toString() + ":" + id.toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a client-supplied cursor.
     *
     * @return null when the cursor is absent or malformed, both of which mean
     *         "start from the beginning" rather than an error. A tampered cursor
     *         must not be able to fail a request.
     */
    public static TransactionCursor decode(String cursor) {
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
            return new TransactionCursor(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }
}
