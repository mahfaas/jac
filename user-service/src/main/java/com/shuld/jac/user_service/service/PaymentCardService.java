package com.shuld.jac.user_service.service;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.entity.PaymentCard;
import com.shuld.jac.user_service.entity.User;
import com.shuld.jac.user_service.exception.MaxCardsExceededException;
import com.shuld.jac.user_service.exception.ResourceNotFoundException;
import com.shuld.jac.user_service.mapper.PaymentCardMapper;
import com.shuld.jac.user_service.repository.PaymentCardRepository;
import com.shuld.jac.user_service.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCardService {

    private static final int MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository cardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    public PaymentCardService(PaymentCardRepository cardRepository,
                              UserRepository userRepository,
                              PaymentCardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public PaymentCardDto createCard(PaymentCardDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + dto.getUserId()));

        long existingCards = cardRepository.countByUserId(user.getId());
        if (existingCards >= MAX_CARDS_PER_USER) {
            throw new MaxCardsExceededException(
                    "User id=" + user.getId() + " already has the maximum of " + MAX_CARDS_PER_USER + " cards");
        }

        PaymentCard card = cardMapper.toEntity(dto);
        user.addCard(card);

        userRepository.saveAndFlush(user);

        return cardMapper.toDto(card);
    }

    public PaymentCardDto getCardById(Long id) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: id=" + id));
        return cardMapper.toDto(card);
    }

    public Page<PaymentCardDto> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable).map(cardMapper::toDto);
    }

    public Page<PaymentCardDto> getCardsByUserId(Long userId, Pageable pageable) {
        return cardRepository.findByUserId(userId, pageable).map(cardMapper::toDto);
    }

    @Transactional
    public PaymentCardDto updateCard(Long id, PaymentCardDto dto) {
        PaymentCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: id=" + id));

        card.setNumber(dto.getNumber());
        card.setHolder(dto.getHolder());
        card.setExpirationDate(dto.getExpirationDate());

        PaymentCard saved = cardRepository.save(card);
        return cardMapper.toDto(saved);
    }

    @Transactional
    public void setCardActive(Long id, boolean active) {
        int updated = cardRepository.setActive(id, active);
        if (updated == 0) {
            throw new ResourceNotFoundException("Card not found: id=" + id);
        }
    }
}