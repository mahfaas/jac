package com.shuld.jac.orderservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.shuld.jac.orderservice.entity.Item;
import com.shuld.jac.orderservice.entity.Order;
import com.shuld.jac.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    private Item persistItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return itemRepository.save(item);
    }

    private String userJson(long userId, String email) {
        return "{\"id\":" + userId + ",\"name\":\"John\",\"surname\":\"Doe\","
                + "\"birthDate\":\"1990-01-01\",\"email\":\"" + email + "\",\"active\":true}";
    }

    private void stubUserByEmail(String email, long userId) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/users/internal/by-email"))
                .withQueryParam("email", WireMock.equalTo(email))
                .willReturn(WireMock.okJson(userJson(userId, email))));
    }

    private void stubUserByEmailNotFound(String email) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/users/internal/by-email"))
                .withQueryParam("email", WireMock.equalTo(email))
                .willReturn(WireMock.aResponse().withStatus(404)));
    }

    private void stubUserById(long userId, String email) {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/users/internal/" + userId))
                .willReturn(WireMock.okJson(userJson(userId, email))));
    }

    private void stubUserByIdUnavailable(long userId) {
        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/api/users/internal/" + userId))
                .willReturn(WireMock.aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    private String createOrderRequestJson(String email, long itemId, int quantity) {
        return "{\"userEmail\":\"" + email + "\",\"items\":[{\"itemId\":" + itemId
                + ",\"quantity\":" + quantity + "}]}";
    }

    private Order persistOrder(Long userId, OrderStatus status, BigDecimal totalPrice) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalPrice(totalPrice);
        return orderRepository.save(order);
    }

    @Test
    void createOrder_success_returnsCreatedWithOrderAndUserInfo() throws Exception {
        Item item = persistItem("Keyboard", "50.00");
        stubUserByEmail("john@example.com", 10L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequestJson("john@example.com", item.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalPrice").value(100.00))
                .andExpect(jsonPath("$.items[0].itemId").value(item.getId()))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void createOrder_userNotFound_returnsNotFound() throws Exception {
        Item item = persistItem("Mouse", "20.00");
        stubUserByEmailNotFound("missing@example.com");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequestJson("missing@example.com", item.getId(), 1)))
                .andExpect(status().isNotFound());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_itemNotFound_returnsNotFound() throws Exception {
        stubUserByEmail("john@example.com", 10L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequestJson("john@example.com", 999L, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"not-an-email\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderById_found_returnsOrderWithUserInfo() throws Exception {
        Order order = persistOrder(10L, OrderStatus.CREATED, new BigDecimal("30.00"));
        stubUserById(10L, "john@example.com");

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));
    }

    @Test
    void getOrderById_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getOrderById_userServiceUnavailable_stillReturnsOrderWithNullUser() throws Exception {
        Order order = persistOrder(10L, OrderStatus.CREATED, new BigDecimal("30.00"));
        stubUserByIdUnavailable(10L);

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void getOrders_filtersByStatus() throws Exception {
        persistOrder(10L, OrderStatus.CREATED, new BigDecimal("10.00"));
        persistOrder(10L, OrderStatus.CANCELLED, new BigDecimal("20.00"));
        stubUserById(10L, "john@example.com");

        mockMvc.perform(get("/api/orders").param("statuses", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
    }

    @Test
    void getOrdersByUserId_returnsOnlyThatUsersOrders() throws Exception {
        persistOrder(10L, OrderStatus.CREATED, new BigDecimal("10.00"));
        persistOrder(20L, OrderStatus.CREATED, new BigDecimal("15.00"));
        stubUserById(10L, "john@example.com");

        mockMvc.perform(get("/api/orders/user/{userId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(10));
    }

    @Test
    void updateOrder_success_updatesStatus() throws Exception {
        Order order = persistOrder(10L, OrderStatus.CREATED, new BigDecimal("10.00"));
        stubUserById(10L, "john@example.com");

        mockMvc.perform(put("/api/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void updateOrder_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/orders/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_softDeletes_subsequentGetReturnsNotFound() throws Exception {
        Order order = persistOrder(10L, OrderStatus.CREATED, new BigDecimal("10.00"));

        mockMvc.perform(delete("/api/orders/{id}", order.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isNotFound());

        Boolean deletedFlag = jdbcTemplate.queryForObject(
                "SELECT deleted FROM orders WHERE id = ?", Boolean.class, order.getId());
        assertThat(deletedFlag).isTrue();
    }
}
