package com.peluware.omnisearch.reactive.mutiny;

import com.peluware.domain.Page;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.reactive.FlowOmniSearch;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * Defines the contract for performing dynamic and flexible entity-based search operations
 * using Mutiny's {@link Uni} as the reactive type.
 */
public interface MutinyOmniSearch {

    /**
     * Executes a search operation for the specified entity class using the provided options.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return Uni emitting a list of matched entities
     */
    <E> Uni<List<E>> search(Class<E> entityClass, OmniSearchOptions options);

    /**
     * Executes a search operation for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to search
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return Uni emitting a list of matched entities
     */
    default <E> Uni<List<E>> search(Class<E> entityClass, Consumer<OmniSearchOptions> optionsConsumer) {
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
     * @return Uni emitting the total number of matched entities
     */
    <E> Uni<Long> count(Class<E> entityClass, OmniSearchBaseOptions options);

    /**
     * Counts the total number of matched entities for the specified entity class using a consumer to configure the options.
     *
     * @param entityClass     the class of the entity to count
     * @param optionsConsumer a consumer to configure the search options
     * @param <E>             the entity type
     * @return Uni emitting the total number of matched entities
     */
    default <E> Uni<Long> count(Class<E> entityClass, Consumer<OmniSearchBaseOptions> optionsConsumer) {
        var options = new OmniSearchBaseOptions();
        optionsConsumer.accept(options);
        return count(entityClass, options);
    }

    /**
     * Executes a paginated search operation for the specified entity class using the provided options.
     * This method combines the results of {@link #search(Class, OmniSearchOptions)} and {@link #count(Class, OmniSearchBaseOptions)}
     * to return a {@link Page} object containing the results and pagination metadata.
     *
     * @param entityClass the class of the entity to search
     * @param options     the search options including filters, sorting, and pagination
     * @param <E>         the entity type
     * @return Uni emitting a Page of matched entities
     */
    default <E> Uni<Page<E>> page(Class<E> entityClass, OmniSearchOptions options) {
        return MutinyPageUtils.deferred(
                search(entityClass, options),
                options.getPagination(),
                options.getSort(),
                () -> count(entityClass, options)
        );
    }

    /**
     * Adapter method to convert a MutinyOmniSearch instance into a FlowOmniSearch instance.
     *
     * @param mutinyOmniSearch the MutinyOmniSearch instance to adapt
     * @return a FlowOmniSearch instance
     */
    static FlowOmniSearch toFlow(MutinyOmniSearch mutinyOmniSearch) {
        return new FlowOmniSearch() {
            @Override
            public <E> Flow.Publisher<List<E>> search(Class<E> entityClass, OmniSearchOptions options) {
                return mutinyOmniSearch.search(entityClass, options).convert().toPublisher();
            }

            @Override
            public <E> Flow.Publisher<Long> count(Class<E> entityClass, OmniSearchBaseOptions options) {
                return mutinyOmniSearch.count(entityClass, options).convert().toPublisher();
            }
        };
    }

    /**
     * Adapter method to convert a FlowOmniSearch instance into a MutinyOmniSearch instance.
     *
     * @param flowOmniSearch the FlowOmniSearch instance to adapt
     * @return a MutinyOmniSearch instance
     */
    static MutinyOmniSearch fromFlow(FlowOmniSearch flowOmniSearch) {
        return new MutinyOmniSearch() {
            @Override
            public <E> Uni<List<E>> search(Class<E> entityClass, OmniSearchOptions options) {
                return Uni.createFrom().publisher(flowOmniSearch.search(entityClass, options));
            }

            @Override
            public <E> Uni<Long> count(Class<E> entityClass, OmniSearchBaseOptions options) {
                return Uni.createFrom().publisher(flowOmniSearch.count(entityClass, options));
            }
        };
    }
}
