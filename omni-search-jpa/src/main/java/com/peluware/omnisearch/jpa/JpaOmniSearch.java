package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.core.OmniSearch;
import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.OmniSearchOptions;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderTools;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JPA-based implementation of the {@link OmniSearch} interface,
 * allowing flexible querying, sorting, and pagination of JPA-managed entities.
 */
@Slf4j
public class JpaOmniSearch implements OmniSearch {

    private final EntityManager em;
    private final RsqlJpaBuilderTools rsqlBuilderTools;

    /**
     * Constructs a {@code JpaOmniSearch} using the provided {@link EntityManager}
     * and the default {@link RsqlJpaBuilderTools}.
     *
     * @param em the entity manager used to execute queries
     */
    public JpaOmniSearch(EntityManager em) {
        this(em, RsqlJpaBuilderTools.DEFAULT);
    }

    /**
     * Constructs a {@code JpaOmniSearch} using the provided {@link EntityManager}
     * and {@link RsqlJpaBuilderTools} instance.
     *
     * @param em           the entity manager used to execute queries
     * @param rsqlBuilderTools helper tools used to build RSQL predicates and joins
     */
    public JpaOmniSearch(EntityManager em, RsqlJpaBuilderTools rsqlBuilderTools) {
        this.em = em;
        this.rsqlBuilderTools = rsqlBuilderTools;
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
    public <E> List<E> search(Class<E> entityClass, OmniSearchOptions options) {

        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(entityClass);
        var root = query.from(entityClass);

        var predicate = JpaOmniSearchPredicateBuilder.buildPredicate(em, root, options, rsqlBuilderTools);

        query.where(predicate);

        var sort = options.getSort();
        if (sort.isSorted()) {
            query.orderBy(sort.orders().stream()
                    .map(order -> {
                        var path = root.get(order.property());
                        return order.ascending() ? cb.asc(path) : cb.desc(path);
                    })
                    .toList()
            );
        }

        var pagination = options.getPagination();
        if (!pagination.isPaginated()) {
            return em.createQuery(query).getResultList();
        }

        return em
                .createQuery(query)
                .setFirstResult(pagination.offset())
                .setMaxResults(pagination.size())
                .getResultList();
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

        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(entityClass);

        var predicate = JpaOmniSearchPredicateBuilder.buildPredicate(em, root, options, rsqlBuilderTools);

        query.where(predicate);
        query.select(cb.count(root));

        return em.createQuery(query).getSingleResult();
    }
}
