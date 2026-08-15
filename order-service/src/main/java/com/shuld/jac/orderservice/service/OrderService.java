package com.shuld.jac.orderservice.service;

import com.shuld.jac.orderservice.client.UserServiceClient;
import com.shuld.jac.orderservice.dto.OrderCreateRequest;
import com.shuld.jac.orderservice.dto.OrderItemDto;
import com.shuld.jac.orderservice.dto.OrderResponseDto;
import com.shuld.jac.orderservice.dto.OrderUpdateRequest;
import com.shuld.jac.orderservice.dto.UserInfoDto;
import com.shuld.jac.orderservice.entity.Item;
import com.shuld.jac.orderservice.entity.Order;
import com.shuld.jac.orderservice.entity.OrderItem;
import com.shuld.jac.orderservice.entity.OrderStatus;
import com.shuld.jac.orderservice.exception.ResourceNotFoundException;
import com.shuld.jac.orderservice.mapper.OrderMapper;
import com.shuld.jac.orderservice.repository.ItemRepository;
import com.shuld.jac.orderservice.repository.OrderRepository;
import com.shuld.jac.orderservice.repository.specification.OrderSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    public OrderService(OrderRepository orderRepository, ItemRepository itemRepository,
                         OrderMapper orderMapper, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.orderMapper = orderMapper;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequest request) {
        UserInfoDto user = userServiceClient.getUserByEmail(request.getUserEmail());
        if (user == null) {
            throw new ResourceNotFoundException("User not found or unavailable: email=" + request.getUserEmail());
        }

        Order order = new Order();
        order.setUserId(user.getId());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemDto itemDto : request.getItems()) {
            Item item = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: id=" + itemDto.getItemId()));
            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(itemDto.getQuantity());
            order.addItem(orderItem);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }
        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        return orderMapper.toResponseDto(saved, user);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        Order order = findOrderOrThrow(id);
        UserInfoDto user = userServiceClient.getUserById(order.getUserId());
        return orderMapper.toResponseDto(order, user);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrders(LocalDateTime from, LocalDateTime to,
                                             List<OrderStatus> statuses, Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(OrderSpecifications.filterBy(from, to, statuses), pageable);
        Map<Long, UserInfoDto> userCache = new HashMap<>();
        return orders.map(order -> {
            UserInfoDto user = userCache.computeIfAbsent(order.getUserId(), userServiceClient::getUserById);
            return orderMapper.toResponseDto(order, user);
        });
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable) {
        UserInfoDto user = userServiceClient.getUserById(userId);
        return orderRepository.findByUserId(userId, pageable)
                .map(order -> orderMapper.toResponseDto(order, user));
    }

    @Transactional
    public OrderResponseDto updateOrder(Long id, OrderUpdateRequest request) {
        Order order = findOrderOrThrow(id);
        order.setStatus(request.getStatus());
        Order saved = orderRepository.save(order);
        UserInfoDto user = userServiceClient.getUserById(saved.getUserId());
        return orderMapper.toResponseDto(saved, user);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: id=" + id));
    }
}
