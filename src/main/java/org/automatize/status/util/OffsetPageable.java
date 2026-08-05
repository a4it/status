package org.automatize.status.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

/**
 * <p>
 * A {@link Pageable} addressed by absolute row offset rather than by page number.
 * </p>
 *
 * <p>
 * {@link org.springframework.data.domain.PageRequest} can only express offsets that are an
 * exact multiple of the page size. That is insufficient when a single logical page is
 * assembled from two independently ordered sources, where the second source has to be read
 * from an offset determined by the size of the first. This implementation lets a query be
 * issued for an arbitrary {@code offset}/{@code limit} window while keeping the database in
 * charge of skipping rows.
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public final class OffsetPageable implements Pageable {

    private final long offset;
    private final int limit;
    private final Sort sort;

    /**
     * Creates a pageable covering {@code limit} rows starting at {@code offset}.
     *
     * @param offset the zero-based index of the first row to return; must not be negative
     * @param limit the maximum number of rows to return; must be positive
     * @param sort the sort to apply; must not be null
     */
    public OffsetPageable(long offset, int limit, Sort sort) {
        // A negative offset or a non-positive limit cannot be translated into a valid query window
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("offset must be >= 0 and limit must be >= 1");
        }
        this.offset = offset;
        this.limit = limit;
        this.sort = Objects.requireNonNull(sort, "sort must not be null");
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageable(offset - limit, limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageable((long) pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset >= limit;
    }

    @Override
    public boolean equals(Object other) {
        // Identity short-circuit avoids the field comparison for the common self-comparison
        if (this == other) {
            return true;
        }
        // Only another OffsetPageable can be equal to this one
        if (!(other instanceof OffsetPageable that)) {
            return false;
        }
        return offset == that.offset && limit == that.limit && sort.equals(that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, limit, sort);
    }

    @Override
    public String toString() {
        return "OffsetPageable{offset=" + offset + ", limit=" + limit + ", sort=" + sort + '}';
    }
}
