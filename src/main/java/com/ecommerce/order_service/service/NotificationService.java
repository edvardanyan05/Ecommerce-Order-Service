package com.ecommerce.order_service.service;

import com.ecommerce.order_service.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class NotificationService {

    /**
     * Runs on a background thread once the order's transaction has committed,
     * so customers are never notified about an order that was rolled back.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("[NOTIFICATION SERVICE] Sending email to {}: Your order #{} for '{}' has been successfully placed!",
                event.customerEmail(), event.orderId(), event.productName());
    }
}
