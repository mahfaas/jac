package com.shuld.jac.orderservice.dto;

import com.shuld.jac.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderUpdateRequest {

    @NotNull
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
