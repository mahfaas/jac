package com.shuld.jac.user_service.service;

import com.shuld.jac.user_service.dto.UserDto;
import com.shuld.jac.user_service.entity.User;
import com.shuld.jac.user_service.exception.ResourceNotFoundException;
import com.shuld.jac.user_service.mapper.UserMapper;
import com.shuld.jac.user_service.repository.UserRepository;
import com.shuld.jac.user_service.repository.specification.UserSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserDto createUser(UserDto dto) {
        User user = userMapper.toEntity(dto);
        user.setId(null); // на случай если id случайно пришёл в теле запроса на создание
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
        return userMapper.toDto(user);
    }

    public Page<UserDto> getAllUsers(String name, String surname, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.filterBy(name, surname), pageable)
                .map(userMapper::toDto);
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setBirthDate(dto.getBirthDate());
        user.setEmail(dto.getEmail());
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Transactional
    public void setUserActive(Long id, boolean active) {
        int updated = userRepository.setActive(id, active);
        if (updated == 0) {
            throw new ResourceNotFoundException("User not found: id=" + id);
        }
    }
}