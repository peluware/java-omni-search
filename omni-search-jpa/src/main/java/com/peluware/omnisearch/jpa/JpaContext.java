package com.peluware.omnisearch.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

public interface JpaContext {

    Metamodel getMetamodel();

    CriteriaBuilder getCriteriaBuilder();

    static JpaContext of(EntityManager em) {
        return new EntityManagerJpaContext(em);
    }

    record EntityManagerJpaContext(EntityManager em) implements JpaContext {
        @Override
        public Metamodel getMetamodel() {
            return em.getMetamodel();
        }

        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            return em.getCriteriaBuilder();
        }
    }
}
