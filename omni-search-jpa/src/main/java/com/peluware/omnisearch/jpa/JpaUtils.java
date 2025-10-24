package com.peluware.omnisearch.jpa;

import com.peluware.domain.Sort;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;

import java.util.List;

public final class JpaUtils {

    private JpaUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <E> List<Order> getOrders(Sort sort, Root<E> root, CriteriaBuilder cb) {
        return sort.orders().stream()
                .map(order -> {
                    var path = root.get(order.property());
                    return order.direction() == com.peluware.domain.Order.Direction.ASC ? cb.asc(path) : cb.desc(path);
                })
                .toList();
    }

    public static boolean needDistinct(OmniSearchBaseOptions options, Root<?> root, JpaContext jpaContext) {
        var hasSearch = options.getSearch() != null && !options.getSearch().isBlank();
        // Si hay búsqueda de texto, revisa si hay propagaciones o colecciones de elementos básicos
        if (hasSearch) {
            // Revisa si hay propagaciones
            var hasPropagations = options.getPropagations() != null && !options.getPropagations().isEmpty();
            if (hasPropagations) {
                return true;
            }
            // Revisa si hay colecciones de elementos básicos
            var hasBasicElementCollection = hasBasicElementCollection(root, jpaContext);
            if (hasBasicElementCollection) {
                return true;
            }
        }

        // Revisa si la query RSQL contiene campos anidados
        return options.getQuery() != null && containsNestedField(options.getQuery());
    }


    private static boolean containsNestedField(Node node) {
        if (node instanceof ComparisonNode cmp) {
            // Heuristicamente puede fallar si el campo tiene un punto en su nombre (pero es raro) o si el campo anidado
            // es de una relacion de muchos a uno o de uno a uno, en tal caso habria que revisar el metamodelo JPA
            // TODO: mejorar esta logica si es necesario
            return cmp.getSelector().contains(".");
        }

        if (node instanceof LogicalNode log) {
            // revisa recursivamente hijos AND/OR
            for (var child : log.getChildren()) {
                if (containsNestedField(child)) return true;
            }
        }
        return false;
    }

    private static boolean hasBasicElementCollection(Root<?> root, JpaContext jpaContext) {
        var managedType = jpaContext.getMetamodel().managedType(root.getJavaType());
        for (var attribute : managedType.getAttributes()) {
            var type = attribute.getPersistentAttributeType();
            if (type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION &&
                    attribute instanceof PluralAttribute<?, ?, ?> plural &&
                    plural.getElementType().getPersistenceType() == Type.PersistenceType.BASIC) {
                return true;
            }
        }
        return false;
    }
}
