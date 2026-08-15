package com.shuld.jac.orderservice.mapper;

import com.shuld.jac.orderservice.dto.ItemDto;
import com.shuld.jac.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(Item entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntity(ItemDto dto);
}
