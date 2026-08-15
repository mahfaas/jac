package com.shuld.jac.orderservice.repository.specification;

import com.shuld.jac.orderservice.entity.Order;
import com.shuld.jac.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty())
                        ? null
                        : root.get("status").in(statuses);
    }

    public static Specification<Order> filterBy(LocalDateTime from, LocalDateTime to, List<OrderStatus> statuses) {
        return Specification.where(createdBetween(from, to)).and(hasStatusIn(statuses));
    }
}
