package com.jobportal.service.dto;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Which slice of a result set to return: {@code GET /jobs?page=0&size=20&sort=salary,desc}.
 *
 * @param page   zero-based page index
 * @param size   items per page (1..100)
 * @param sortBy one of {@link #SORTABLE_FIELDS}
 */
public record PageRequest(int page, int size, String sortBy, Direction direction) {

    public enum Direction { ASC, DESC }

    public static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "salary", "experience", "title");
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        Objects.requireNonNull(direction, "direction");
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("cannot sort by '" + sortBy + "', allowed: " + SORTABLE_FIELDS);
        }
    }

    /** Newest jobs first. */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, "createdAt", Direction.DESC);
    }

    /** Parses a REST-style sort parameter such as {@code "salary,desc"} or {@code "title"}. */
    public static PageRequest of(int page, int size, String sort) {
        String[] parts = sort.split(",");
        Direction dir = parts.length > 1 ? Direction.valueOf(parts[1].trim().toUpperCase(Locale.ROOT)) : Direction.ASC;
        return new PageRequest(page, size, parts[0].trim(), dir);
    }

    public long offset() {
        return (long) page * size;
    }
}
