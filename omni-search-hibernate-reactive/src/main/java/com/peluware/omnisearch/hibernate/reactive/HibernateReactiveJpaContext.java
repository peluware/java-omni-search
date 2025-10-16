package com.peluware.omnisearch.hibernate.reactive;

import com.peluware.omnisearch.jpa.JpaContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.reactive.mutiny.Mutiny;

public record HibernateReactiveJpaContext(Mutiny.SessionFactory sf) implements JpaContext {

    @Override
    public Metamodel getMetamodel() {
        return sf.getMetamodel();
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return sf.getCriteriaBuilder();
    }
}
