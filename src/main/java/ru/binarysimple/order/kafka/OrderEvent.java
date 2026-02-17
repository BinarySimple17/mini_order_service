package ru.binarysimple.order.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import ru.binarysimple.order.model.NotificationContact;
import ru.binarysimple.order.model.NotificationType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderEvent {
//    private Long orderId;

    private String username;

    private BigDecimal totalCost;

    private String status;

//    private NotificationContact contact;

    private NotificationType notificationType;

    private Long parentId;
}
