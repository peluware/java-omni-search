package com.peluware.omnisearch.mongodb;

@FunctionalInterface
public interface CollectionNameResolver {

    CollectionNameResolver DEFAULT = entityClass -> entityClass.getSimpleName().toLowerCase();

    /**
     * Resolves the collection name for a given entity class.
     *
     * @param entityClass the class of the entity for which to resolve the collection name
     * @return the resolved collection name as a string
     */
    String resolveCollectionName(Class<?> entityClass);

}
