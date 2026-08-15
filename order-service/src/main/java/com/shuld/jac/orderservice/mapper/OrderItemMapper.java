package com.shuld.jac.orderservice.mapper;

import com.shuld.jac.orderservice.dto.OrderItemDto;
import com.shuld.jac.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    OrderItemDto toDto(OrderItem entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItem toEntity(OrderItemDto dto);
}
