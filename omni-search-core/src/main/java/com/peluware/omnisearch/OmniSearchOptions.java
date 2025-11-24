package com.peluware.omnisearch;

import com.peluware.domain.DefaultPagination;
import com.peluware.domain.Order;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Set;

/**
 * Represents the extended options for search operations, including sorting and pagination.
 */
public class OmniSearchOptions extends OmniSearchBaseOptions {

    private Sort sort = Sort.unsorted();
    private Pagination pagination = Pagination.unpaginated();

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions search(String search) {
        return (OmniSearchOptions) super.search(search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions propagations(@NonNull Set<String> propagations) {
        return (OmniSearchOptions) super.propagations(propagations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions propagations(String @NonNull ... propagations) {
        return (OmniSearchOptions) super.propagations(propagations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions query(String query) {
        return (OmniSearchOptions) super.query(query);
    }

    /**
     * Sets the sort configuration.
     *
     * @param sort the sorting criteria
     * @return the updated options
     */
    public OmniSearchOptions sort(@NonNull Sort sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets the sorting criteria using the specified order list.
     *
     * @param orders the sorting orders
     * @return the updated options
     */
    public OmniSearchOptions sort(Order @NonNull ... orders) {
        return sort(Sort.by(orders));
    }

    /**
     * Sets the sorting criteria using a collection of orders.
     *
     * @param orders the collection of sorting orders
     * @return the updated options
     */
    public OmniSearchOptions sort(@NonNull Collection<Order> orders) {
        return sort(Sort.by(orders));
    }

    /**
     * Sets the pagination options.
     *
     * @param page the pagination configuration
     * @return the updated options
     */
    public OmniSearchOptions pagination(@NonNull Pagination page) {
        this.pagination = page;
        return this;
    }

    /**
     * Sets the pagination options with the given page number and size.
     *
     * @param pageNumber the page number (zero-based)
     * @param pageSize   the size of the page
     * @return the updated options
     */
    public OmniSearchOptions pagination(int pageNumber, int pageSize) {
        return pagination(new DefaultPagination(pageNumber, pageSize));
    }

    /**
     * Gets the sort configuration.
     *
     * @return the sorting criteria
     */
    public Sort getSort() {
        return sort;
    }

    /**
     * Gets the pagination options.
     *
     * @return the pagination configuration
     */
    public Pagination getPagination() {
        return pagination;
    }
}
