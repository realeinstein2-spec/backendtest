package com.makershub.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PageableUtilsTest {

    @Test
    void sanitize_nullPageable_returnsDefaultSort() {
        Pageable result = PageableUtils.sanitize(null);
        assertThat(result).isNotNull();
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void sanitize_unsortedPageable_returnsDefaultSort() {
        Pageable unsorted = PageRequest.of(0, 10, Sort.unsorted());
        Pageable result = PageableUtils.sanitize(unsorted);
        assertThat(result).isNotNull();
    }

    @Test
    void sanitize_validSortProperty_preservesSort() {
        Pageable validSort = PageRequest.of(1, 15, Sort.by(Sort.Direction.ASC, "fullName"));
        Pageable result = PageableUtils.sanitize(validSort);
        assertThat(result.getPageNumber()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(15);
        assertThat(result.getSort().getOrderFor("fullName")).isNotNull();
        assertThat(result.getSort().getOrderFor("fullName").getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
