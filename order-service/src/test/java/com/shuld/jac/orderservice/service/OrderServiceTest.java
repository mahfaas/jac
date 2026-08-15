package com.shuld.jac.orderservice.service;

import com.shuld.jac.orderservice.client.UserServiceClient;
import com.shuld.jac.orderservice.dto.OrderCreateRequest;
import com.shuld.jac.orderservice.dto.OrderItemDto;
import com.shuld.jac.orderservice.dto.OrderResponseDto;
import com.shuld.jac.orderservice.dto.OrderUpdateRequest;
import com.shuld.jac.orderservice.dto.UserInfoDto;
import com.shuld.jac.orderservice.entity.Item;
import com.shuld.jac.orderservice.entity.Order;
import com.shuld.jac.orderservice.entity.OrderStatus;
import com.shuld.jac.orderservice.exception.ResourceNotFoundException;
import com.shuld.jac.orderservice.mapper.OrderMapper;
import com.shuld.jac.orderservice.repository.ItemRepository;
import com.shuld.jac.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserServiceClient userServiceClient;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, itemRepository, orderMapper, userServiceClient);
    }

    private Item sampleItem(Long id, String price) {
        Item item = new Item();
        item.setId(id);
        item.setName("Item " + id);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private Order sampleOrder(Long id, Long userId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalPrice(BigDecimal.ZERO);
        return order;
    }

    private UserInfoDto sampleUser(Long id) {
        UserInfoDto user = new UserInfoDto();
        user.setId(id);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@example.com");
        user.setActive(true);
        return user;
    }

    private OrderCreateRequest sampleCreateRequest(String email, List<OrderItemDto> items) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setUserEmail(email);
        request.setItems(items);
        return request;
    }

    private OrderItemDto sampleOrderItemDto(Long itemId, int quantity) {
        OrderItemDto dto = new OrderItemDto();
        dto.setItemId(itemId);
        dto.setQuantity(quantity);
        return dto;
    }

    @Test
    void createOrder_success_computesTotalPriceAndPersistsWithResolvedUserId() {
        UserInfoDto user = sampleUser(10L);
        OrderCreateRequest request = sampleCreateRequest("john@example.com",
                List.of(sampleOrderItemDto(1L, 2), sampleOrderItemDto(2L, 1)));

        when(userServiceClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem(1L, "10.00")));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(sampleItem(2L, "5.00")));

        Order savedOrder = sampleOrder(100L, 10L, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponseDto expectedDto = new OrderResponseDto();
        expectedDto.setId(100L);
        when(orderMapper.toResponseDto(savedOrder, user)).thenReturn(expectedDto);

        OrderResponseDto result = orderService.createOrder(request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order persisted = orderCaptor.getValue();

        assertThat(persisted.getUserId()).isEqualTo(10L);
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(persisted.getTotalPrice()).isEqualByComparingTo("25.00");
        assertThat(persisted.getItems()).hasSize(2);
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void createOrder_userNotFound_throwsResourceNotFoundException() {
        OrderCreateRequest request = sampleCreateRequest("missing@example.com",
                List.of(sampleOrderItemDto(1L, 1)));
        when(userServiceClient.getUserByEmail("missing@example.com")).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_itemNotFound_throwsResourceNotFoundException() {
        UserInfoDto user = sampleUser(10L);
        OrderCreateRequest request = sampleCreateRequest("john@example.com",
                List.of(sampleOrderItemDto(99L, 1)));

        when(userServiceClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_found_enrichesWithUserInfo() {
        Order order = sampleOrder(1L, 10L, OrderStatus.CREATED);
        UserInfoDto user = sampleUser(10L);
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userServiceClient.getUserById(10L)).thenReturn(user);
        when(orderMapper.toResponseDto(order, user)).thenReturn(dto);

        OrderResponseDto result = orderService.getOrderById(1L);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void getOrderById_notFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void getOrders_sameUserAcrossPage_fetchesUserOnlyOnce() {
        Order order1 = sampleOrder(1L, 10L, OrderStatus.CREATED);
        Order order2 = sampleOrder(2L, 10L, OrderStatus.PAID);
        UserInfoDto user = sampleUser(10L);
        Page<Order> page = new PageImpl<>(List.of(order1, order2));

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userServiceClient.getUserById(10L)).thenReturn(user);
        when(orderMapper.toResponseDto(eq(order1), eq(user))).thenReturn(new OrderResponseDto());
        when(orderMapper.toResponseDto(eq(order2), eq(user))).thenReturn(new OrderResponseDto());

        Page<OrderResponseDto> result = orderService.getOrders(null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
        verify(userServiceClient, times(1)).getUserById(10L);
    }

    @Test
    void getOrdersByUserId_fetchesUserOnceForAllOrders() {
        Order order1 = sampleOrder(1L, 5L, OrderStatus.CREATED);
        Order order2 = sampleOrder(2L, 5L, OrderStatus.SHIPPED);
        UserInfoDto user = sampleUser(5L);
        Page<Order> page = new PageImpl<>(List.of(order1, order2));

        when(userServiceClient.getUserById(5L)).thenReturn(user);
        when(orderRepository.findByUserId(eq(5L), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponseDto(eq(order1), eq(user))).thenReturn(new OrderResponseDto());
        when(orderMapper.toResponseDto(eq(order2), eq(user))).thenReturn(new OrderResponseDto());

        Page<OrderResponseDto> result = orderService.getOrdersByUserId(5L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
        verify(userServiceClient, times(1)).getUserById(5L);
    }

    @Test
    void updateOrder_found_updatesStatusAndReturnsDto() {
        Order order = sampleOrder(1L, 10L, OrderStatus.CREATED);
        UserInfoDto user = sampleUser(10L);
        OrderUpdateRequest request = new OrderUpdateRequest();
        request.setStatus(OrderStatus.PAID);
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(userServiceClient.getUserById(10L)).thenReturn(user);
        when(orderMapper.toResponseDto(order, user)).thenReturn(dto);

        OrderResponseDto result = orderService.updateOrder(1L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    void updateOrder_notFound_throwsResourceNotFoundException() {
        OrderUpdateRequest request = new OrderUpdateRequest();
        request.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_found_delegatesToRepositoryDelete() {
        Order order = sampleOrder(1L, 10L, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L);

        verify((CrudRepository<Order, Long>) orderRepository).delete(order);
    }

    @Test
    void deleteOrder_notFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify((CrudRepository<Order, Long>) orderRepository, never()).delete(any());
    }
}
