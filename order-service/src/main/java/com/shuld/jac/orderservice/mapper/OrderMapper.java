package com.shuld.jac.orderservice.mapper;

import com.shuld.jac.orderservice.dto.OrderDto;
import com.shuld.jac.orderservice.dto.OrderResponseDto;
import com.shuld.jac.orderservice.dto.UserInfoDto;
import com.shuld.jac.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderDto toDto(Order entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "user", source = "user")
    OrderResponseDto toResponseDto(Order entity, UserInfoDto user);
}
