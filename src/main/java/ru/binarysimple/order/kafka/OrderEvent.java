package ru.binarysimple.order.kafka;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.binarysimple.order.model.NotificationContact;
import ru.binarysimple.order.model.NotificationType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {
    private Long orderId;

    private String username;

    private BigDecimal totalCost;

    private String status;

    private NotificationContact contact;

    private NotificationType notificationType;

    private Long parentId;
}
