package com.shuld.jac.user_service.mapper;

import com.shuld.jac.user_service.dto.UserDto;
import com.shuld.jac.user_service.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User entity);

    User toEntity(UserDto dto);
}