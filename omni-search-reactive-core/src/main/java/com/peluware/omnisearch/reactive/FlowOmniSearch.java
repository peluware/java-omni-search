package com.peluware.omnisearch.reactive;

import com.peluware.domain.Page;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * Defines the contract for performing dynamic and flexible entity-based search operations.
 */
public interface FlowOmniSearch {

    /**
     * Executes a search operation for the specified entity class using the provided options.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return Publisher emitting a single list of matched entities
     */
    <E> Flow.Publisher<List<E>> search(Class<E> entityClass, OmniSearchOptions options);

    /**
     * Executes a search operation for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to search
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return Publisher emitting a single list of matched entities
     */
    default <E> Flow.Publisher<List<E>> search(Class<E> entityClass, Consumer<OmniSearchOptions> optionsConsumer) {
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
     * @return Publisher emitting a single value: the total number of matched entities
     */
    <E> Flow.Publisher<Long> count(Class<E> entityClass, OmniSearchBaseOptions options);

    /**
     * Counts the total number of matched entities for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to count
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return Publisher emitting a single value: the total number of matched entities
     */
    default <E> Flow.Publisher<Long> count(Class<E> entityClass, Consumer<OmniSearchBaseOptions> optionsConsumer) {
        var options = new OmniSearchBaseOptions();
        optionsConsumer.accept(options);
        return count(entityClass, options);
    }

    /**
     * Executes a paginated search operation for the specified entity class using the provided options.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return Publisher emitting a single Page of matched entities
     */
    default <E> Flow.Publisher<Page<E>> searchPage(Class<E> entityClass, OmniSearchOptions options) {
        return FlowPageUtils.deferred(
                search(entityClass, options),
                options.getPagination(),
                options.getSort(),
                () -> count(entityClass, options)
        );
    }
}
