package com.shuld.jac.user_service.controller;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.service.PaymentCardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentCardController {

    private final PaymentCardService cardService;

    public PaymentCardController(PaymentCardService cardService) {
        this.cardService = cardService;
    }

    @PreAuthorize("hasRole('ADMIN') or #dto.userId == authentication.principal.userId")
    @PostMapping("/api/cards")
    public ResponseEntity<PaymentCardDto> createCard(@Valid @RequestBody PaymentCardDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(dto));
    }

    @GetMapping("/api/cards/{id}")
    public ResponseEntity<PaymentCardDto> getCardById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/cards")
    public ResponseEntity<Page<PaymentCardDto>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    @GetMapping("/api/users/{userId}/cards")
    public ResponseEntity<Page<PaymentCardDto>> getCardsByUserId(@PathVariable("userId") Long userId, Pageable pageable) {
        return ResponseEntity.ok(cardService.getCardsByUserId(userId, pageable));
    }

    @PutMapping("/api/cards/{id}")
    public ResponseEntity<PaymentCardDto> updateCard(@PathVariable("id") Long id, @Valid @RequestBody PaymentCardDto dto) {
        return ResponseEntity.ok(cardService.updateCard(id, dto));
    }

    @PatchMapping("/api/cards/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable("id") Long id) {
        cardService.setCardActive(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/cards/{id}/deactivate")
    public ResponseEntity<Void> deactivateCard(@PathVariable("id") Long id) {
        cardService.setCardActive(id, false);
        return ResponseEntity.noContent().build();
    }
}