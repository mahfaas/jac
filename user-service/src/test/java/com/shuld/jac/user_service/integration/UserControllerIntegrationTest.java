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

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private UserDto userDto(String name, String surname, String email) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setSurname(surname);
        dto.setEmail(email);
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        return dto;
    }

    private Long createUserAndGetId(String name, String surname, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto(name, surname, email))))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    @Test
    void createUser_persistsAndReturnsCreatedUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto("John", "Doe", "john.doe@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void createUser_invalidPayload_returnsBadRequest() throws Exception {
        UserDto invalid = new UserDto();
        invalid.setBirthDate(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUserById_found_returnsUser() throws Exception {
        Long id = createUserAndGetId("Jane", "Roe", "jane.roe@example.com");

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane.roe@example.com"));
    }

    @Test
    void getAllUsers_filtersByNameAndSurname() throws Exception {
        createUserAndGetId("Alice", "Anderson", "alice@example.com");
        createUserAndGetId("Bob", "Brown", "bob@example.com");

        mockMvc.perform(get("/api/users").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"));
    }

    @Test
    void updateUser_found_updatesFieldsAndReturnsOk() throws Exception {
        Long id = createUserAndGetId("Old", "Name", "old.name@example.com");

        UserDto update = userDto("New", "Name", "new.name@example.com");
        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.email").value("new.name@example.com"));

        assertThat(userRepository.findById(id).orElseThrow().getName()).isEqualTo("New");
    }

    @Test
    void updateUser_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/users/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto("A", "B", "a.b@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateAndDeactivateUser_togglesActiveFlag() throws Exception {
        Long id = createUserAndGetId("Toggle", "User", "toggle.user@example.com");

        mockMvc.perform(patch("/api/users/{id}/deactivate", id))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(id).orElseThrow().getActive()).isFalse();

        mockMvc.perform(patch("/api/users/{id}/activate", id))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findById(id).orElseThrow().getActive()).isTrue();
    }

    @Test
    void getUserWithCards_secondCallIsRealCacheHit() throws Exception {
        Long id = createUserAndGetId("Cached", "User", "cached.user@example.com");

        mockMvc.perform(get("/api/users/{id}/with-cards", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cached"))
                .andExpect(jsonPath("$.cards").isArray());

        mockMvc.perform(get("/api/users/{id}/with-cards", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cached"))
                .andExpect(jsonPath("$.cards").isArray());
    }

    @Test
    void getUserWithCards_cacheEvictedWhenCardIsAddedThroughCardController() throws Exception {
        Long id = createUserAndGetId("WithCards", "User", "with.cards.user@example.com");

        mockMvc.perform(get("/api/users/{id}/with-cards", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards").isEmpty());

        PaymentCardDto card = new PaymentCardDto();
        card.setUserId(id);
        card.setNumber("4111111111111111");
        card.setHolder("With Cards User");
        card.setExpirationDate("12/29");

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/{id}/with-cards", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards.length()").value(1))
                .andExpect(jsonPath("$.cards[0].number").value("4111111111111111"));
    }
}
