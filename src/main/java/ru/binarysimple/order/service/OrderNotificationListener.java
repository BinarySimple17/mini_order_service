package ru.binarysimple.order.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.saga.events.OrderCreatedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {

        log.debug("Processing notification for order {}", event.getOrder().getId());

        notificationService.sendNotification(event.getOrder());
    }
}