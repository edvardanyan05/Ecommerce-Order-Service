package com.ecommerce.order_service.service;

import com.ecommerce.order_service.event.OrderCreatedEvent;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(Order order) {
        // Clients must not choose the ID or status of a new order.
        order.setId(null);
        order.setStatus(Order.DEFAULT_STATUS);
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(
                new OrderCreatedEvent(saved.getId(), saved.getCustomerEmail(), saved.getProductName()));
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
