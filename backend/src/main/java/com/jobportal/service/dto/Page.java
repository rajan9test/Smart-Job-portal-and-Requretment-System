package com.jobportal.service.dto;

import java.util.List;

/** One page of results plus what a client needs to render pagination controls. */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
