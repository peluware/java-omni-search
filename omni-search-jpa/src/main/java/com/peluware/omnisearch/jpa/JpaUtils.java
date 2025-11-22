package com.peluware.omnisearch.jpa;

import com.peluware.domain.Sort;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

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

}
