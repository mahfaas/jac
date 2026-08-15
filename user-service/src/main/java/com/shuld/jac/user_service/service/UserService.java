package com.shuld.jac.user_service.service;

import com.shuld.jac.user_service.dto.UserDto;
import com.shuld.jac.user_service.dto.UserWithCardsDto;
import com.shuld.jac.user_service.entity.User;
import com.shuld.jac.user_service.exception.ResourceNotFoundException;
import com.shuld.jac.user_service.mapper.PaymentCardMapper;
import com.shuld.jac.user_service.mapper.UserMapper;
import com.shuld.jac.user_service.repository.PaymentCardRepository;
import com.shuld.jac.user_service.repository.UserRepository;
import com.shuld.jac.user_service.repository.specification.UserSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentCardRepository cardRepository;
    private final PaymentCardMapper cardMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PaymentCardRepository cardRepository, PaymentCardMapper cardMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public UserDto createUser(UserDto dto) {
        User user = userMapper.toEntity(dto);
        user.setId(null);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
        return userMapper.toDto(user);
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: email=" + email));
        return userMapper.toDto(user);
    }

    public Page<UserDto> getAllUsers(String name, String surname, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.filterBy(name, surname), pageable)
                .map(userMapper::toDto);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
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
    @CacheEvict(value = "users", key = "#id")
    public void setUserActive(Long id, boolean active) {
        int updated = userRepository.setActive(id, active);
        if (updated == 0) {
            throw new ResourceNotFoundException("User not found: id=" + id);
        }
    }

    @Cacheable(value = "users", key = "#id")
    public UserWithCardsDto getUserWithCards(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));

        UserWithCardsDto dto = new UserWithCardsDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setBirthDate(user.getBirthDate());
        dto.setEmail(user.getEmail());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCards(cardRepository.findByUserId(id).stream().map(cardMapper::toDto).toList());
        return dto;
    }
}