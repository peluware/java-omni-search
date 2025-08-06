package com.peluware.omnisearch.core;

import com.peluware.domain.Order;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import cz.jirutka.rsql.parser.ast.Node;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * Represents the extended options for search operations, including sorting and pagination.
 */
@Getter
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
    public OmniSearchOptions joins(@NotNull Set<String> joins) {
        return (OmniSearchOptions) super.joins(joins);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions joins(String @NotNull ... joins) {
        return (OmniSearchOptions) super.joins(joins);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OmniSearchOptions query(Node query) {
        return (OmniSearchOptions) super.query(query);
    }

    /**
     * Sets the sort configuration.
     *
     * @param sort the sorting criteria
     * @return the updated options
     */
    public OmniSearchOptions sort(@NotNull Sort sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets the sorting criteria using the specified order list.
     *
     * @param orders the sorting orders
     * @return the updated options
     */
    public OmniSearchOptions sort(Order @NotNull ... orders) {
        return sort(Sort.by(orders));
    }

    /**
     * Sets the sorting criteria using a collection of orders.
     *
     * @param orders the collection of sorting orders
     * @return the updated options
     */
    public OmniSearchOptions sort(@NotNull Collection<Order> orders) {
        return sort(Sort.by(orders));
    }

    /**
     * Sets the pagination options.
     *
     * @param page the pagination configuration
     * @return the updated options
     */
    public OmniSearchOptions pagination(@NotNull Pagination page) {
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
        return pagination(new Pagination(pageNumber, pageSize));
    }
}
