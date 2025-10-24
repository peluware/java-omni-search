package com.peluware.omnisearch.hibernate.reactive;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.JpaContext;
import com.peluware.omnisearch.jpa.JpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaUtils;
import com.peluware.omnisearch.jpa.rsql.RsqlJpaBuilderOptions;
import com.peluware.omnisearch.reactive.mutiny.MutinyOmniSearch;
import io.smallrye.mutiny.Uni;
import lombok.Setter;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;


public class HibernateOmniSearch implements MutinyOmniSearch {

    private final Mutiny.SessionFactory sf;
    private final RsqlJpaBuilderOptions rsqlBuilderTools;
    private final JpaContext jpaContext;

    @Setter
    private JpaOmniSearchPredicateBuilder predicateBuilder = JpaOmniSearchPredicateBuilder.DEFAULT;

    public HibernateOmniSearch(Mutiny.SessionFactory sf) {
        this(sf, RsqlJpaBuilderOptions.DEFAULT);
    }

    public HibernateOmniSearch(Mutiny.SessionFactory sf, RsqlJpaBuilderOptions rsqlBuilderTools) {
        this.sf = sf;
        this.rsqlBuilderTools = rsqlBuilderTools;
        this.jpaContext = new HibernateReactiveJpaContext(sf);
    }

    @Override
    public <E> Uni<List<E>> search(Class<E> entityClass, OmniSearchOptions options) {
        return sf.withSession(session -> {

            var cb = session.getCriteriaBuilder();
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
                return session.createQuery(cq).getResultList();
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
        return sf.withSession(session -> {

            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);

            var predicate = predicateBuilder.buildPredicate(jpaContext, root, options, rsqlBuilderTools);

            cq.select(cb.count(root));
            if (JpaUtils.needDistinct(options, root, jpaContext)) {
                cq.distinct(true);
            }

            cq.where(predicate);

            return session.createQuery(cq).getSingleResult();
        });
    }
}
