package com.hireflow.common.util;

import java.util.Locale;

/**
 * Builds the {@code LIKE} pattern for free-text search boxes.
 *
 * <p>Queries use {@code lower(column) like :pattern escape '!'}. An empty search becomes {@code %}
 * (match everything) instead of {@code null}, which keeps one query per list endpoint and avoids
 * PostgreSQL's "could not determine data type of parameter" error for null text parameters.
 * The user's own {@code %} and {@code _} are escaped so they match literally.
 */
public final class SearchPatterns {

    public static final char ESCAPE = '!';

    private SearchPatterns() {
    }

    public static String contains(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
