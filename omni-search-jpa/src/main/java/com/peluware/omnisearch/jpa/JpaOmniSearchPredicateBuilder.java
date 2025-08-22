package com.peluware.omnisearch.jpa;


import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;


public interface JpaOmniSearchPredicateBuilder {


    JpaOmniSearchPredicateBuilder DEFAULT = new DefaultJpaOmniSearchPredicateBuilder();

    /**
     * Builds a {@link Predicate} based on the provided options.
     *
     * @param em           the entity manager
     * @param root         the root path of the query
     * @param options      the search options
     * @param <E>          thr root of the entity type
     * @return the constructed predicate
     */
    <E> Predicate buildPredicate(EntityManager em, Root<E> root, OmniSearchBaseOptions options);
}
