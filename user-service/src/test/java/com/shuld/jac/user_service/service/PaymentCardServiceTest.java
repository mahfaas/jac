package com.shuld.jac.user_service.service;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.entity.PaymentCard;
import com.shuld.jac.user_service.entity.User;
import com.shuld.jac.user_service.exception.MaxCardsExceededException;
import com.shuld.jac.user_service.exception.ResourceNotFoundException;
import com.shuld.jac.user_service.mapper.PaymentCardMapper;
import com.shuld.jac.user_service.repository.PaymentCardRepository;
import com.shuld.jac.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentCardMapper cardMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache usersCache;

    private PaymentCardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new PaymentCardService(cardRepository, userRepository, cardMapper, cacheManager);
    }

    private PaymentCardDto sampleCardDto(Long userId) {
        PaymentCardDto dto = new PaymentCardDto();
        dto.setUserId(userId);
        dto.setNumber("4111111111111111");
        dto.setHolder("John Doe");
        dto.setExpirationDate("12/29");
        return dto;
    }

    @Test
    void createCard_success_savesCardAndEvictsUserCache() {
        User user = new User();
        user.setId(1L);
        PaymentCardDto dto = sampleCardDto(1L);
        PaymentCard card = new PaymentCard();
        PaymentCardDto expected = sampleCardDto(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(1L)).thenReturn(2L);
        when(cardMapper.toEntity(dto)).thenReturn(card);
        when(cacheManager.getCache("users")).thenReturn(usersCache);
        when(cardMapper.toDto(card)).thenReturn(expected);

        PaymentCardDto result = cardService.createCard(dto);

        assertThat(card.getUser()).isEqualTo(user);
        assertThat(result).isEqualTo(expected);
        verify(userRepository).flush();
        verify(usersCache).evict(1L);
    }

    @Test
    void createCard_userNotFound_throwsAndDoesNotEvict() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.createCard(sampleCardDto(1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cacheManager, never()).getCache(any());
    }

    @Test
    void createCard_maxCardsExceeded_throwsAndDoesNotSave() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> cardService.createCard(sampleCardDto(1L)))
                .isInstanceOf(MaxCardsExceededException.class);

        verify(userRepository, never()).flush();
        verify(cacheManager, never()).getCache(any());
    }

    @Test
    void getCardById_found_returnsMappedDto() {
        PaymentCard card = new PaymentCard();
        PaymentCardDto dto = sampleCardDto(1L);
        when(cardRepository.findById(5L)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(dto);

        PaymentCardDto result = cardService.getCardById(5L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getCardById_notFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllCards_delegatesToRepository() {
        PaymentCard card = new PaymentCard();
        PaymentCardDto dto = sampleCardDto(1L);
        Pageable pageable = Pageable.unpaged();
        when(cardRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(card)));
        when(cardMapper.toDto(card)).thenReturn(dto);

        Page<PaymentCardDto> result = cardService.getAllCards(pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void getCardsByUserId_delegatesToRepository() {
        PaymentCard card = new PaymentCard();
        PaymentCardDto dto = sampleCardDto(1L);
        Pageable pageable = Pageable.unpaged();
        when(cardRepository.findByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(card)));
        when(cardMapper.toDto(card)).thenReturn(dto);

        Page<PaymentCardDto> result = cardService.getCardsByUserId(1L, pageable);

        assertThat(result.getContent()).containsExactly(dto);
    }

    @Test
    void updateCard_found_updatesFieldsAndEvictsOwnerCache() {
        User owner = new User();
        owner.setId(7L);
        PaymentCard card = new PaymentCard();
        card.setUser(owner);

        PaymentCardDto update = sampleCardDto(7L);
        update.setNumber("4222222222222222");
        update.setHolder("New Holder");
        update.setExpirationDate("01/30");

        PaymentCardDto expected = sampleCardDto(7L);

        when(cardRepository.findById(9L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cacheManager.getCache("users")).thenReturn(usersCache);
        when(cardMapper.toDto(card)).thenReturn(expected);

        PaymentCardDto result = cardService.updateCard(9L, update);

        assertThat(card.getNumber()).isEqualTo("4222222222222222");
        assertThat(card.getHolder()).isEqualTo("New Holder");
        assertThat(card.getExpirationDate()).isEqualTo("01/30");
        assertThat(result).isEqualTo(expected);
        verify(usersCache).evict(7L);
    }

    @Test
    void updateCard_notFound_throwsAndDoesNotSave() {
        when(cardRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.updateCard(9L, sampleCardDto(7L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void setCardActive_found_updatesFlagAndEvictsOwnerCache() {
        when(cardRepository.findUserIdById(9L)).thenReturn(Optional.of(7L));
        when(cacheManager.getCache("users")).thenReturn(usersCache);

        cardService.setCardActive(9L, false);

        verify(cardRepository).setActive(9L, false);
        verify(usersCache).evict(7L);
    }

    @Test
    void setCardActive_notFound_throwsAndDoesNotUpdate() {
        when(cardRepository.findUserIdById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.setCardActive(9L, true))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cardRepository, never()).setActive(any(), anyBoolean());
    }
}
