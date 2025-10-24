package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import jakarta.persistence.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JPA-based implementation of the {@link OmniSearch} interface,
 * allowing flexible querying, sorting, and pagination of JPA-managed entities.
 */
@Slf4j
public class JpaOmniSearch implements OmniSearch {

    private final EntityManager em;
    private final RsqlJpaBuilderOptions rsqlBuilderTools;
    private final JpaContext jpaContext;

    @Setter
    private JpaOmniSearchPredicateBuilder predicateBuilder = JpaOmniSearchPredicateBuilder.DEFAULT;

    /**
     * Constructs a {@code JpaOmniSearch} using the provided {@link EntityManager}
     * and the default {@link RsqlJpaBuilderOptions}.
     *
     * @param em the entity manager used to execute queries
     */
    public JpaOmniSearch(EntityManager em) {
        this(em, RsqlJpaBuilderOptions.DEFAULT);
    }

    /**
     * Constructs a {@code JpaOmniSearch} using the provided {@link EntityManager}
     * and {@link RsqlJpaBuilderOptions} instance.
     *
     * @param em               the entity manager used to execute queries
     * @param rsqlBuilderTools helper tools used to build RSQL predicates and joins
     */
    public JpaOmniSearch(EntityManager em, RsqlJpaBuilderOptions rsqlBuilderTools) {
        this.em = em;
        this.rsqlBuilderTools = rsqlBuilderTools;
        this.jpaContext = JpaContext.of(em);
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

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(entityClass);
        var root = cq.from(entityClass);

        var predicate = predicateBuilder.buildPredicate(jpaContext, root, options, rsqlBuilderTools);

        cq.where(predicate);
        if (JpaUtils.needDistinct(options, root, jpaContext)) {
            cq.distinct(true);
        }

        var sort = options.getSort();
        if (sort.isSorted()) {
            cq.orderBy(JpaUtils.getOrders(sort, root, cb));
        }

        var pagination = options.getPagination();
        if (!pagination.isPaginated()) {
            return em.createQuery(cq).getResultList();
        }

        return em
                .createQuery(cq)
                .setFirstResult(pagination.getNumber() * pagination.getSize())
                .setMaxResults(pagination.getSize())
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
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);

        var predicate = predicateBuilder.buildPredicate(jpaContext, root, options, rsqlBuilderTools);

        cq.where(predicate);
        if (JpaUtils.needDistinct(options, root, jpaContext)) {
            cq.distinct(true);
        }

        cq.select(cb.count(root));

        return em.createQuery(cq).getSingleResult();
    }

}
