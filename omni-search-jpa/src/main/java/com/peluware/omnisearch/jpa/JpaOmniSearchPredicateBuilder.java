package com.peluware.omnisearch.jpa;


import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.jpa.rsql.JpaPredicateVisitor;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
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
    <E> Predicate buildPredicate(JpaContext em, Root<E> root, OmniSearchBaseOptions options);

    /**
     * Builds a {@link Predicate} based on the provided options, including RSQL query parsing.
     * @param jpaContext the JPA context
     * @param root the root of the query
     * @param options the search options
     * @param rsqlJpaBuilderOptions the RSQL builder options
     * @return the constructed predicate
     * @param <E> the entity type
     */
    default <E> Predicate buildPredicate(JpaContext jpaContext, Root<E> root, OmniSearchBaseOptions options, RsqlJpaBuilderOptions rsqlJpaBuilderOptions) {
        var predicate = buildPredicate(jpaContext, root, options);
        var query = options.getQuery();
        var cb = jpaContext.getCriteriaBuilder();
        if (query != null) {
            var visitor = new JpaPredicateVisitor<>(root, rsqlJpaBuilderOptions);
            var filtersPredicate = query.accept(visitor, jpaContext);
            predicate = cb.and(predicate, filtersPredicate);
        }
        return predicate;
    }
}
