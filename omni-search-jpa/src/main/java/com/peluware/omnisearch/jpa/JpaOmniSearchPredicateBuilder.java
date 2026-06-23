package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Metamodel;

/**
 * Builds a JPA {@link Predicate} based on the given root entity and
 * {@link OmniSearchBaseOptions}.
 */
public interface JpaOmniSearchPredicateBuilder {

    /**
     * Creates a {@link Predicate} for the specified root entity using
     * the provided search options.
     *
     * @param root            the root entity of the query
     * @param options         the base search options
     * @param criteriaBuilder the criteria builder
     * @param metamodel       the metamodel used to resolve attributes
     * @param <E>             the type of the root entity
     * @return a {@link Predicate} for use in JPA criteria queries
     */
    <E> Predicate buildPredicate(From<?, E> root, OmniSearchBaseOptions options, CriteriaBuilder criteriaBuilder, Metamodel metamodel);
}
