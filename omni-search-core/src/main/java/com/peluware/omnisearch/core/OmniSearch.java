package com.peluware.omnisearch.core;

import java.util.List;
import java.util.function.Consumer;

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
    <E> List<E> search(Class<E> entityClass, OmniSearchOptions options);

    /**
     * Executes a search operation for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to search
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return a list of matched entities
     */
    default <E> List<E> search(Class<E> entityClass, Consumer<OmniSearchOptions> optionsConsumer) {
        var options = new OmniSearchOptions();
        optionsConsumer.accept(options);
        return search(entityClass, options);
    }

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
     * Counts the total number of matched entities for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to count
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return the total number of matched entities
     */
    default <E> long count(Class<E> entityClass, Consumer<OmniSearchBaseOptions> optionsConsumer) {
        var options = new OmniSearchBaseOptions();
        optionsConsumer.accept(options);
        return count(entityClass, options);
    }
}
