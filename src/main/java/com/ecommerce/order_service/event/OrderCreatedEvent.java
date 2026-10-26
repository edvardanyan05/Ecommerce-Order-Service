package com.ecommerce.order_service.event;

/**
 * Published after a new order has been persisted.
 * Immutable, so it can be safely handed to listeners running on other threads.
 */
public record OrderCreatedEvent(Long orderId, String customerEmail, String productName) {
}
