package com.peluware.omnisearch.jpa;

import com.peluware.domain.Sort;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;

import java.util.List;
import java.util.Set;

public final class JpaUtils {

    private JpaUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <E> List<Order> getOrders(Sort sort, Root<E> root, CriteriaBuilder cb, JpaContext jpaContext) {
        return sort.orders().stream()
                .map(order -> {
                    var path = findPath(order.property(), root, jpaContext, JoinType.LEFT);
                    return order.direction() == com.peluware.domain.Order.Direction.ASC
                            ? cb.asc(path)
                            : cb.desc(path);
                })
                .toList();
    }

    /**
     * Find a property path in the graph From startRoot, using INNER JOIN for associations and element collections.
     *
     * @param path       The property path to find.
     * @param startRoot  From that property path depends on.
     * @param jpaContext JPA Context to access the metamodel and criteria builder.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public static Path<?> findPath(String path, Path<?> startRoot, JpaContext jpaContext) {
        return findPath(path, startRoot, jpaContext, JoinType.INNER);
    }


    /**
     * Find a property path in the graph From startRoot
     *
     * @param path       The property path to find.
     * @param startRoot  From that property path depends on.
     * @param jpaContext JPA Context to access the metamodel and criteria builder.
     * @param joinType   The type of join to use for associations and element collections.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public static Path<?> findPath(String path, Path<?> startRoot, JpaContext jpaContext, JoinType joinType) {
        var graph = path.split("\\.");

        var metaModel = jpaContext.getMetamodel();
        var classMetadata = metaModel.managedType(startRoot.getJavaType());
        var currentRoot = startRoot;

        var graphLength = graph.length;
        for (int i = 0; i < graphLength; i++) {
            var attribute = graph[i];

            if (!hasAttribute(attribute, classMetadata)) {
                throw new IllegalArgumentException("Unknown property: " + attribute + " From<?,?> entity " + classMetadata.getJavaType().getName());
            }

            var jpaAttribute = classMetadata.getAttribute(attribute);
            var persistentAttributeType = jpaAttribute.getPersistentAttributeType();
            var attributeType = getSingularType(jpaAttribute);

            if (jpaAttribute.isAssociation()) {

                classMetadata = metaModel.managedType(attributeType);
                currentRoot = getOrCreateJoin((From<?, ?>) currentRoot, attribute, joinType);

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.EMBEDDED) {

                classMetadata = metaModel.embeddable(attributeType);
                currentRoot = currentRoot.get(attribute);

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {

                currentRoot = getOrCreateJoin((From<?, ?>) currentRoot, attribute, joinType);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("ElementCollection must be the last part of the path: " + path);
                }

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.BASIC) {

                currentRoot = currentRoot.get(attribute);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("Basic attribute must be the last part of the path: " + path);
                }

            }
        }

        return currentRoot;
    }


    /**
     * Verifies if a class metamodel has the specified property.
     *
     * @param property      Property name.
     * @param classMetadata Class metamodel that may hold that property.
     * @return <tt>true</tt> if the class has that property, <tt>false</tt> otherwise.
     */
    public static <E> boolean hasAttribute(String property, ManagedType<E> classMetadata) {
        Set<Attribute<? super E, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super E, ?> name : names) {
            if (name.getName().equals(property)) return true;
        }
        return false;
    }

    /**
     * Get the property Type out of the metamodel.
     *
     * @param property Property name for type extraction.
     * @return Class java type for the property,
     * if the property is a pluralAttribute it will take the bindable java type of that collection.
     */
    public static Class<?> getSingularType(Attribute<?, ?> property) {
        if (property.isCollection()) {
            return ((PluralAttribute<?, ?, ?>) property).getBindableJavaType();
        }
        return property.getJavaType();
    }


    @SuppressWarnings("unchecked")
    public static <Z, X> Join<Z, X> getOrCreateJoin(From<Z, X> from, String attribute, JoinType joinType) {
        return (Join<Z, X>) from.getJoins().stream()
                .filter(join -> join.getAttribute().getName().equals(attribute) && join.getJoinType() == joinType)
                .findFirst()
                .orElseGet(() -> from.join(attribute, joinType));
    }
}
