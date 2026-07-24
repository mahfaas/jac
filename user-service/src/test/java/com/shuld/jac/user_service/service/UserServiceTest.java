package com.shuld.jac.user_service.service;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.dto.UserDto;
import com.shuld.jac.user_service.dto.UserWithCardsDto;
import com.shuld.jac.user_service.entity.PaymentCard;
import com.shuld.jac.user_service.entity.User;
import com.shuld.jac.user_service.exception.ResourceNotFoundException;
import com.shuld.jac.user_service.mapper.PaymentCardMapper;
import com.shuld.jac.user_service.mapper.UserMapper;
import com.shuld.jac.user_service.repository.PaymentCardRepository;
import com.shuld.jac.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PaymentCardRepository cardRepository;
    @Mock
    private PaymentCardMapper cardMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper, cardRepository, cardMapper);
    }

    private User sampleUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@example.com");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setActive(true);
        return user;
    }

    private UserDto sampleUserDto(Long id) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setName("John");
        dto.setSurname("Doe");
        dto.setEmail("john@example.com");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setActive(true);
        return dto;
    }

    @Test
    void createUser_savesEntityWithNullIdAndReturnsMappedDto() {
        UserDto inputDto = sampleUserDto(null);
        User mappedEntity = sampleUser(999L);
        User savedEntity = sampleUser(1L);
        UserDto expectedDto = sampleUserDto(1L);

        when(userMapper.toEntity(inputDto)).thenReturn(mappedEntity);
        when(userRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(userMapper.toDto(savedEntity)).thenReturn(expectedDto);

        UserDto result = userService.createUser(inputDto);

        assertThat(mappedEntity.getId()).isNull();
        assertThat(result).isEqualTo(expectedDto);
        verify(userRepository).save(mappedEntity);
    }

    @Test
    void getUserById_found_returnsMappedDto() {
        User entity = sampleUser(1L);
        UserDto dto = sampleUserDto(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(dto);

        UserDto result = userService.getUserById(1L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void getAllUsers_delegatesToRepositoryWithSpecAndPageable() {
        Pageable pageable = Pageable.unpaged();
        User entity = sampleUser(1L);
        UserDto dto = sampleUserDto(1L);
        Page<User> page = new PageImpl<>(List.of(entity));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toDto(entity)).thenReturn(dto);

        Page<UserDto> result = userService.getAllUsers("John", null, pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void updateUser_found_updatesFieldsAndReturnsDto() {
        User existing = sampleUser(1L);
        UserDto update = sampleUserDto(1L);
        update.setName("Jane");
        update.setSurname("Smith");
        update.setEmail("jane@example.com");
        update.setBirthDate(LocalDate.of(1991, 2, 2));

        User saved = sampleUser(1L);
        UserDto expected = sampleUserDto(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(expected);

        UserDto result = userService.updateUser(1L, update);

        assertThat(existing.getName()).isEqualTo("Jane");
        assertThat(existing.getSurname()).isEqualTo("Smith");
        assertThat(existing.getEmail()).isEqualTo("jane@example.com");
        assertThat(existing.getBirthDate()).isEqualTo(LocalDate.of(1991, 2, 2));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updateUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, sampleUserDto(1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void setUserActive_found_updatesActiveFlag() {
        when(userRepository.setActive(1L, true)).thenReturn(1);

        userService.setUserActive(1L, true);

        verify(userRepository).setActive(1L, true);
    }

    @Test
    void setUserActive_notFound_throwsResourceNotFoundException() {
        when(userRepository.setActive(1L, false)).thenReturn(0);

        assertThatThrownBy(() -> userService.setUserActive(1L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserWithCards_found_returnsDtoWithMappedCards() {
        User entity = sampleUser(1L);
        PaymentCard card = new PaymentCard();
        card.setId(5L);
        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setId(5L);
        cardDto.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(cardRepository.findByUserId(1L)).thenReturn(List.of(card));
        when(cardMapper.toDto(card)).thenReturn(cardDto);

        UserWithCardsDto result = userService.getUserWithCards(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getCards()).containsExactly(cardDto);
    }

    @Test
    void getUserWithCards_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserWithCards(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).findByUserId(anyLong());
    }
}
