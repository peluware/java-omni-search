package com.peluware.omnisearch;

import com.peluware.domain.Page;

import java.util.List;

/**
 * Defines the contract for performing dynamic and flexible entity-based search operations.
 */
public interface OmniSearch {

    /**
     * Executes a search operation for the specified entity class using the provided options.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return a list of matched entities
     */
    <E> List<E> list(Class<E> entityClass, OmniSearchOptions options);

    /**
     * Counts the total number of matched entities for the specified entity class using the provided options.
     *
     * @param entityClass the class of the entity to count
     * @param options     the search options including filters and joins
     * @param <E>         the entity type
     * @return the total number of matched entities
     */
    <E> long count(Class<E> entityClass, OmniSearchBaseOptions options);


    /**
     * Executes a paginated search operation for the specified entity class using the provided options.
     * This method combines the results of {@link #list(Class, OmniSearchOptions)} and {@link #count(Class, OmniSearchBaseOptions)}
     * to return a {@link Page} object.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return a paginated result of matched entities
     */
    default <E> Page<E> page(Class<E> entityClass, OmniSearchOptions options) {
        return Page.deferred(
                list(entityClass, options),
                options.getPagination(),
                options.getSort(),
                () -> count(entityClass, options)
        );
    }
}
