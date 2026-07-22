package com.makershub.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtils {

    /**
     * Sanitizes a Pageable to ensure empty or invalid sort parameters (e.g. sort=)
     * gracefully fall back to a valid default sort instead of throwing query syntax errors.
     */
    public static Pageable sanitize(Pageable pageable, String defaultSortProperty, Sort.Direction defaultDirection) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(defaultDirection, defaultSortProperty));
        }

        if (pageable.getSort().isSorted()) {
            boolean invalid = false;
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty() == null || order.getProperty().trim().isEmpty()) {
                    invalid = true;
                    break;
                }
            }
            if (invalid) {
                return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(defaultDirection, defaultSortProperty));
            }
        }
        return pageable;
    }

    public static Pageable sanitize(Pageable pageable) {
        return sanitize(pageable, "createdAt", Sort.Direction.DESC);
    }
}
