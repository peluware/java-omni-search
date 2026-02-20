package com.peluware.omnisearch;

import com.peluware.domain.Page;

import java.util.List;

/**
 * Default implementation of {@link EntityOmniSearch} that delegates
 * all operations to a shared {@link OmniSearch} engine.
 *
 * <p>
 * This implementation is stateless and lightweight. It simply binds
 * an entity type to an {@link OmniSearch} instance, providing a
 * type-safe and convenient facade.
 * </p>
 *
 * @param <E> the entity type
 */
public class DefaultEntityOmniSearch<E> implements EntityOmniSearch<E> {

    private final OmniSearch omniSearch;
    private final Class<E> entityClass;

    /**
     * Creates a new typed search adapter for the given entity class.
     *
     * @param omniSearch the underlying search engine
     * @param entityClass the entity class to bind
     */
    public DefaultEntityOmniSearch(OmniSearch omniSearch, Class<E> entityClass) {
        this.omniSearch = omniSearch;
        this.entityClass = entityClass;
    }

    @Override
    public List<E> list(OmniSearchOptions options) {
        return omniSearch.list(entityClass, options);
    }

    @Override
    public long count(OmniSearchBaseOptions options) {
        return omniSearch.count(entityClass, options);
    }

    @Override
    public Page<E> page(OmniSearchOptions options) {
        return omniSearch.page(entityClass, options);
    }
}