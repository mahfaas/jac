package com.shuld.jac.user_service.mapper;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(source = "user.id", target = "userId")
    PaymentCardDto toDto(PaymentCard entity);

    @Mapping(target = "user", ignore = true)
    PaymentCard toEntity(PaymentCardDto dto);
}