package com.peluware.omnisearch.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

/**
 * Provides access to JPA metadata and a {@link CriteriaBuilder}.
 * <p>
 * Can be created from an {@link EntityManager} using {@link #of(EntityManager)}.
 */
public interface JpaContext {

    /**
     * Returns the JPA {@link Metamodel} of the persistence unit.
     *
     * @return the metamodel
     */
    Metamodel getMetamodel();

    /**
     * Returns the {@link CriteriaBuilder} for creating JPA criteria queries.
     *
     * @return the criteria builder
     */
    CriteriaBuilder getCriteriaBuilder();

    /**
     * Creates a {@link JpaContext} from the given {@link EntityManager}.
     *
     * @param em the entity manager
     * @return a new {@link JpaContext} wrapping the entity manager
     */
    static JpaContext of(EntityManager em) {
        return new EntityManagerJpaContext(em);
    }

    /**
     * Default {@link JpaContext} implementation backed by an {@link EntityManager}.
     *
     * @param em the entity manager
     */
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
