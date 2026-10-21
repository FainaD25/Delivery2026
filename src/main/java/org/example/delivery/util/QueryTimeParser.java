package org.example.delivery.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class QueryTimeParser {
    private QueryTimeParser() {
    }

    /**
     * Parses an offset-bearing ISO-8601 timestamp into a UTC instant.
     *
     * @param datetime timestamp supplied by an API client
     * @return equivalent UTC instant
     * @throws IllegalArgumentException when the timestamp has no valid offset format
     */
    public static Instant parse(String datetime) {
        try {
            return OffsetDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid datetime. Expected ISO-8601 format with timezone offset.", e);
        }
    }
}
