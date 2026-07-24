package com.shuld.jac.user_service.integration;

import com.shuld.jac.user_service.dto.PaymentCardDto;
import com.shuld.jac.user_service.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

    private Long createUserAndGetId(String name, String surname, String email) throws Exception {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setSurname(surname);
        dto.setEmail(email);
        dto.setBirthDate(LocalDate.of(1990, 1, 1));

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    private PaymentCardDto cardDto(Long userId, String number, String holder) {
        PaymentCardDto dto = new PaymentCardDto();
        dto.setUserId(userId);
        dto.setNumber(number);
        dto.setHolder(holder);
        dto.setExpirationDate("12/29");
        return dto;
    }

    private Long createCardAndGetId(Long userId, String number) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto(userId, number, "Card Holder"))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    @Test
    void createCard_persistsAndReturnsCreatedCard() throws Exception {
        Long userId = createUserAndGetId("Card", "Owner", "card.owner@example.com");

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto(userId, "4111111111111111", "Card Owner"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.number").value("4111111111111111"));

        assertThat(cardRepository.countByUserId(userId)).isEqualTo(1);
    }

    @Test
    void createCard_userNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto(999999L, "4111111111111111", "Nobody"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_maxCardsExceeded_returnsConflict() throws Exception {
        Long userId = createUserAndGetId("Max", "Cards", "max.cards@example.com");

        for (int i = 0; i < 5; i++) {
            String number = "411111111111" + String.format("%04d", i);
            mockMvc.perform(post("/api/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cardDto(userId, number, "Max Cards"))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto(userId, "4111111111119999", "Max Cards"))))
                .andExpect(status().isConflict());
    }

    @Test
    void getCardById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/cards/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardById_found_returnsCard() throws Exception {
        Long userId = createUserAndGetId("Get", "Card", "get.card@example.com");
        Long cardId = createCardAndGetId(userId, "4111111111111111");

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));
    }

    @Test
    void getCardsByUserId_returnsOnlyThatUsersCards() throws Exception {
        Long userA = createUserAndGetId("User", "A", "user.a@example.com");
        Long userB = createUserAndGetId("User", "B", "user.b@example.com");
        createCardAndGetId(userA, "4111111111111111");
        createCardAndGetId(userB, "4222222222222222");

        mockMvc.perform(get("/api/users/{userId}/cards", userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(userA));
    }

    @Test
    void updateCard_found_updatesFieldsAndReturnsOk() throws Exception {
        Long userId = createUserAndGetId("Update", "Card", "update.card@example.com");
        Long cardId = createCardAndGetId(userId, "4111111111111111");

        PaymentCardDto update = cardDto(userId, "4111111111111111", "Updated Holder");
        update.setExpirationDate("01/30");

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Updated Holder"))
                .andExpect(jsonPath("$.expirationDate").value("01/30"));
    }

    @Test
    void updateCard_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/cards/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardDto(1L, "4111111111111111", "Nobody"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateAndDeactivateCard_togglesActiveFlag() throws Exception {
        Long userId = createUserAndGetId("Toggle", "Card", "toggle.card@example.com");
        Long cardId = createCardAndGetId(userId, "4111111111111111");

        mockMvc.perform(patch("/api/cards/{id}/deactivate", cardId))
                .andExpect(status().isNoContent());
        assertThat(cardRepository.findById(cardId).orElseThrow().getActive()).isFalse();

        mockMvc.perform(patch("/api/cards/{id}/activate", cardId))
                .andExpect(status().isNoContent());
        assertThat(cardRepository.findById(cardId).orElseThrow().getActive()).isTrue();
    }
}
