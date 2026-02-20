package com.peluware.omnisearch;

import com.peluware.domain.Page;

import java.util.List;

/**
 * Typed facade for performing search operations on a specific entity type.
 *
 * <p>
 * This interface provides a type-safe abstraction over {@link OmniSearch},
 * binding all operations to a single entity class. It eliminates the need
 * to repeatedly provide the entity class when executing search operations.
 * </p>
 *
 * <p>
 * Implementations are typically lightweight adapters that delegate to a
 * shared {@link OmniSearch} engine.
 * </p>
 *
 * @param <E> the entity type
 */
public interface EntityOmniSearch<E> {

    /**
     * Executes a search operation using the provided options.
     *
     * @param options the search options including filters, sorting, and pagination
     * @return a list of matched entities
     */
    List<E> list(OmniSearchOptions options);

    /**
     * Counts the total number of matched entities using the provided options.
     *
     * @param options the search options including filters and joins
     * @return the total number of matched entities
     */
    long count(OmniSearchBaseOptions options);

    /**
     * Executes a paginated search operation using the provided options.
     *
     * <p>
     * Implementations may optimize this operation internally, but typically
     * it combines the results of {@link #list(OmniSearchOptions)} and
     * {@link #count(OmniSearchBaseOptions)}.
     * </p>
     *
     * @param options the search options including filters, sorting, and pagination
     * @return a paginated result of matched entities
     */
    Page<E> page(OmniSearchOptions options);
}