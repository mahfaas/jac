package com.shuld.jac.user_service.mapper;

import com.shuld.jac.user_service.dto.UserDto;
import com.shuld.jac.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User entity);

    @Mapping(target = "active", ignore = true)
    User toEntity(UserDto dto);
}