package ru.binarysimple.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.binarysimple.order.client.UserServiceClient;
import ru.binarysimple.order.kafka.OrderEvent;
import ru.binarysimple.order.mapper.OrderMapper;
import ru.binarysimple.order.model.NotificationContact;
import ru.binarysimple.order.model.NotificationType;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private final OrderMapper mapper;

    private final UserServiceClient userClient;

    @Value("${app.kafka.topics.order-events:order.events}")
    private String orderTopic;

    @Override
    public void sendNotification(Order order) {

        if (order == null) return;

        try {

            String email = userClient.getUser(order.getUsername()).getEmail();
            NotificationContact contact = new NotificationContact();
            contact.setEmail(email);

            log.debug("Init notification sending for order {}", order.getId());

            sendEvent(order, contact);
        } catch (Exception e) {
            log.error("Failed to process notification for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void sendEvent(Order order, NotificationContact contact) {

        OrderEvent event = mapper.toOrderEvent(order);
        event.setContact(contact);
        event.setParentId(order.getId());
        event.setOrderId(order.getId());

        switch (order.getStatus()) {
            case FAILED, DELIVERY_FAILED, INSUFFICIENT_FUNDS -> event.setNotificationType(NotificationType.FAIL);
            case NEW, IN_PROGRESS, DONE, CANCELED -> event.setNotificationType(NotificationType.SUCCESS);
            default -> event.setNotificationType(NotificationType.DEFAULT);
        }


        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(orderTopic, order.getUsername(), event);

        // ok
        future.thenAccept(result -> {
            log.debug("Notification sent for order {}. Partition: {}, Offset: {}",
                    order.getId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });

        // !ok
        future.exceptionally(ex -> {
            log.error("Failed to send notification for order {}: {}",
                    order.getId(), ex.getMessage(), ex);
            // тут можно потом сохранить в БД, например, в таблицу failed_messages
            return null;
        });

    }
}
