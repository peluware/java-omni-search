package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Builds a JPA {@link Predicate} based on the given root entity and
 * {@link OmniSearchBaseOptions}.
 */
public interface JpaOmniSearchPredicateBuilder {

    /**
     * Creates a {@link Predicate} for the specified root entity using
     * the provided search options.
     *
     * @param jpaContext the JPA context containing the criteria builder and metadata
     * @param root       the root entity of the query
     * @param options    the base search options
     * @param <E>        the type of the root entity
     * @return a {@link Predicate} for use in JPA criteria queries
     */
    <E> Predicate buildPredicate(JpaContext jpaContext, Root<E> root, OmniSearchBaseOptions options);
}
