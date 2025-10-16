package com.peluware.omnisearch.jpa;

import com.peluware.domain.Sort;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class JpaUtils {

    public static <E> @NotNull List<Order> getOrders(Sort sort, Root<E> root, CriteriaBuilder cb) {
        return sort.orders().stream()
                .map(order -> {
                    var path = root.get(order.property());
                    return order.direction() == com.peluware.domain.Order.Direction.ASC ? cb.asc(path) : cb.desc(path);
                })
                .toList();
    }
}
