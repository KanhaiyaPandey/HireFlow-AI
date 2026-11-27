package com.hireflow.common.util;

import java.util.Map;
import java.util.TreeSet;

import com.hireflow.common.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Maps the public sort names a list endpoint accepts (e.g. {@code ?sort=name,desc}) to what the query
 * actually sorts on. Anything not listed is rejected with 400, so clients cannot sort on arbitrary
 * or internal fields, and a typo is a clear error instead of a 500.
 *
 * <p>Two flavours:
 * <ul>
 *   <li>{@link #properties} — targets are entity properties, for queries that select an entity.</li>
 *   <li>{@link #jpqlExpressions} — targets are aliases in a hand-written JPQL query, such as
 *       {@code u.name}; passed through as {@link JpaSort#unsafe} because Spring Data cannot check them.
 *       Safe here because only the whitelisted strings ever reach the query.</li>
 * </ul>
 */
public final class SortWhitelist {

    private final Map<String, String> targets;
    private final Sort defaultSort;
    private final boolean jpqlExpressions;

    private SortWhitelist(Map<String, String> targets, String defaultKey, Sort.Direction defaultDirection,
                          boolean jpqlExpressions) {
        this.targets = Map.copyOf(targets);
        this.jpqlExpressions = jpqlExpressions;
        this.defaultSort = order(defaultDirection, defaultKey);
    }

    public static SortWhitelist properties(Map<String, String> targets, String defaultKey, Sort.Direction direction) {
        return new SortWhitelist(targets, defaultKey, direction, false);
    }

    public static SortWhitelist jpqlExpressions(Map<String, String> targets, String defaultKey,
                                                Sort.Direction direction) {
        return new SortWhitelist(targets, defaultKey, direction, true);
    }

    /** Same page and size; sort translated, or the default if none was requested. */
    public Pageable apply(Pageable pageable) {
        Sort sort = Sort.unsorted();
        for (Sort.Order requested : pageable.getSort()) {
            sort = sort.and(order(requested.getDirection(), requested.getProperty()));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                sort.isSorted() ? sort : defaultSort);
    }

    private Sort order(Sort.Direction direction, String key) {
        String target = targets.get(key);
        if (target == null) {
            throw new InvalidRequestException(
                    "Cannot sort by '%s'. Allowed: %s".formatted(key, String.join(", ", new TreeSet<>(targets.keySet()))));
        }
        return jpqlExpressions ? JpaSort.unsafe(direction, target) : Sort.by(direction, target);
    }
}
