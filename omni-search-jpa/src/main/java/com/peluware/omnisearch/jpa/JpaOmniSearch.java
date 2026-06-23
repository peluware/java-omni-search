package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.rsql.DefaultRsqlJpaBuilderOptions;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.*;

import java.util.*;

/**
 * JPA-based implementation of the {@link OmniSearch} interface,
 * allowing flexible querying, sorting, and pagination of JPA-managed entities.
 */
public class JpaOmniSearch implements OmniSearch {

    private final EntityManager entityManager;
    private final JpaOmniSearchPredicateBuilder predicateBuilder;

    public JpaOmniSearch(EntityManager entityManager, JpaOmniSearchPredicateBuilder predicateBuilder) {
        this.entityManager = entityManager;
        this.predicateBuilder = predicateBuilder;
    }

    public JpaOmniSearch(EntityManager entityManager, RSQLParser rsqlParser, RsqlJpaBuilderOptions rsqlBuilderTools) {
        this(entityManager, new DefaultJpaOmniSearchPredicateBuilder(rsqlParser, rsqlBuilderTools));
    }

    public JpaOmniSearch(EntityManager entityManager, RSQLParser rsqlParser) {
        this(entityManager, rsqlParser, new DefaultRsqlJpaBuilderOptions());
    }

    public JpaOmniSearch(EntityManager entityManager) {
        this(entityManager, new RSQLParser());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation constructs a dynamic JPA criteria query using the given {@link OmniSearchOptions}.
     * It supports filtering (via RSQL), joins, sorting, and pagination.
     * </p>
     */
    @Override
    public <E> List<E> list(Class<E> entityClass, OmniSearchOptions options) {
        return list(entityClass, options, Map.of());
    }

    public <E> List<E> list(Class<E> entityClass, OmniSearchOptions options, Map<String, Object> hints) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(entityClass);
        var root = cq.from(entityClass);

        var predicate = predicateBuilder.buildPredicate(root, options, cb, entityManager.getMetamodel());
        cq.where(predicate);

        var sort = options.getSort();
        if (sort.isSorted()) {
            cq.orderBy(JpaUtils.getOrders(sort, root, cb, entityManager.getMetamodel()));
        }

        var query = entityManager.createQuery(cq);

        hints.forEach(query::setHint);

        var pagination = options.getPagination();
        if (pagination.isPaginated()) {
            query
                .setFirstResult(pagination.getNumber() * pagination.getSize())
                .setMaxResults(pagination.getSize());
        }

        return query.getResultList();
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation constructs a count query based on the filtering and join
     * criteria provided in the {@link OmniSearchBaseOptions}.
     * </p>
     */
    @Override
    public <E> long count(Class<E> entityClass, OmniSearchBaseOptions options) {
        return count(entityClass, options, Map.of());
    }

    public <E> long count(Class<E> entityClass, OmniSearchBaseOptions options, Map<String, Object> hints) {

        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);

        var predicate = predicateBuilder.buildPredicate(root, options, cb, entityManager.getMetamodel());

        cq
                .where(predicate)
                .select(cb.count(root));

        var query = entityManager.createQuery(cq);

        hints.forEach(query::setHint);

        return query.getSingleResult();
    }

}
