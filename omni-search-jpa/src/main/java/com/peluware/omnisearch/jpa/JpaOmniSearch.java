package com.peluware.omnisearch.jpa;

import com.peluware.omnisearch.core.OmniSearch;
import com.peluware.omnisearch.core.OmniSearchBaseOptions;
import com.peluware.omnisearch.core.OmniSearchOptions;
import com.peluware.omnisearch.jpa.rsql.JpaPredicateVisitor;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
     * @param em           the entity manager used to execute queries
     * @param rsqlBuilderTools helper tools used to build RSQL predicates and joins
     */
    public JpaOmniSearch(EntityManager em, RsqlJpaBuilderOptions rsqlBuilderTools) {
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

        var predicate = buildPredicate(root, cb, options);

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

        var predicate = buildPredicate(root, cb, options);

        query.where(predicate);
        query.select(cb.count(root));

        return em.createQuery(query).getSingleResult();
    }

    public <E> Predicate buildPredicate(Root<E> root, CriteriaBuilder cb, OmniSearchBaseOptions options) {
        var predicate = predicateBuilder.buildPredicate(em, root, options);
        var query = options.getQuery();
        if (query != null) {
            var visitor = new JpaPredicateVisitor<>(root, rsqlBuilderTools);
            var filtersPredicate = query.accept(visitor, em);
            predicate = cb.and(predicate, filtersPredicate);
        }
        return predicate;
    }
}
