package org.automatize.status.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OffsetPageable}, the offset-addressed {@link Pageable} used to read a
 * window that does not start on a page-size boundary.
 */
class OffsetPageableTest {

    private static final Sort BY_NAME = Sort.by("name");

    /**
     * Verifies the window is reported exactly as constructed, including an offset that is not a
     * multiple of the page size (which {@code PageRequest} cannot express).
     */
    @Test
    void reportsOffsetAndLimitAsConstructed() {
        OffsetPageable pageable = new OffsetPageable(7, 5, BY_NAME);

        assertThat(pageable.getOffset()).isEqualTo(7);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(BY_NAME);
        assertThat(pageable.isPaged()).isTrue();
    }

    /**
     * Verifies navigation moves the window by exactly one page size in each direction and that
     * the first page is returned when there is nothing before the current window.
     */
    @Test
    void navigatesByOnePageSize() {
        OffsetPageable pageable = new OffsetPageable(10, 5, BY_NAME);

        assertThat(pageable.next().getOffset()).isEqualTo(15);
        assertThat(pageable.previousOrFirst().getOffset()).isEqualTo(5);
        assertThat(pageable.first().getOffset()).isZero();
        assertThat(pageable.withPage(3).getOffset()).isEqualTo(15);
        assertThat(pageable.hasPrevious()).isTrue();
    }

    /**
     * Verifies a window that starts before the first full page reports no previous page and
     * falls back to the first page.
     */
    @Test
    void windowInsideFirstPageHasNoPrevious() {
        OffsetPageable pageable = new OffsetPageable(3, 5, BY_NAME);

        assertThat(pageable.hasPrevious()).isFalse();
        assertThat(pageable.previousOrFirst().getOffset()).isZero();
    }

    /**
     * Verifies a negative offset or a non-positive limit is rejected, since neither can be
     * translated into a valid query window.
     */
    @Test
    void rejectsInvalidWindow() {
        assertThatThrownBy(() -> new OffsetPageable(-1, 5, BY_NAME))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OffsetPageable(0, 0, BY_NAME))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
