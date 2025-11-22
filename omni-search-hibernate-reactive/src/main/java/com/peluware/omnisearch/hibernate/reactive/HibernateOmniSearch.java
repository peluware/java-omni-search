package com.peluware.omnisearch.hibernate.reactive;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.DefaultJpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaContext;
import com.peluware.omnisearch.jpa.JpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaUtils;
import com.peluware.omnisearch.jpa.rsql.DefaultRsqlJpaBuilderOptions;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import com.peluware.omnisearch.reactive.mutiny.MutinyOmniSearch;
import cz.jirutka.rsql.parser.RSQLParser;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;


public class HibernateOmniSearch implements MutinyOmniSearch {

    private final Mutiny.SessionFactory sessionFactory;
    private final JpaContext jpaContext;
    private final JpaOmniSearchPredicateBuilder predicateBuilder;

    public HibernateOmniSearch(Mutiny.SessionFactory sessionFactory, JpaOmniSearchPredicateBuilder predicateBuilder) {
        this.sessionFactory = sessionFactory;
        this.jpaContext = new HibernateReactiveJpaContext(sessionFactory);
        this.predicateBuilder = predicateBuilder;
    }

    public HibernateOmniSearch(Mutiny.SessionFactory sessionFactory, RSQLParser rsqlParser, RsqlJpaBuilderOptions rsqlBuilderTools) {
        this(sessionFactory, new DefaultJpaOmniSearchPredicateBuilder(rsqlParser, rsqlBuilderTools));
    }

    public HibernateOmniSearch(Mutiny.SessionFactory sessionFactory, RSQLParser rsqlParser) {
        this(sessionFactory, rsqlParser, new DefaultRsqlJpaBuilderOptions());
    }

    public HibernateOmniSearch(Mutiny.SessionFactory sessionFactory) {
        this(sessionFactory, new RSQLParser());
    }


    @Override
    public <E> Uni<List<E>> list(Class<E> entityClass, OmniSearchOptions options) {
        return sessionFactory.withSession(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(entityClass);
            var root = cq.from(entityClass);

            var predicate = predicateBuilder.buildPredicate(jpaContext, root, options);

            cq.where(predicate);

            var sort = options.getSort();
            if (sort.isSorted()) {
                cq.orderBy(JpaUtils.getOrders(sort, root, cb));
            }

            var pagination = options.getPagination();
            if (!pagination.isPaginated()) {
                return session
                        .createQuery(cq)
                        .getResultList();
            }

            return session
                    .createQuery(cq)
                    .setFirstResult(pagination.getNumber() * pagination.getSize())
                    .setMaxResults(pagination.getSize())
                    .getResultList();
        });
    }

    @Override
    public <E> Uni<Long> count(Class<E> entityClass, OmniSearchBaseOptions options) {
        return sessionFactory.withSession(session -> {

            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);

            var predicate = predicateBuilder.buildPredicate(jpaContext, root, options);

            cq
                    .select(cb.count(root))
                    .where(predicate);

            return session
                    .createQuery(cq)
                    .getSingleResult();
        });
    }
}
